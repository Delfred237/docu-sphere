package com.docusphere.auth.service;

import com.docusphere.auth.domain.*;
import com.docusphere.auth.dto.*;
import com.docusphere.auth.event.UserRegisteredEvent;
import com.docusphere.auth.repository.*;
import com.docusphere.common.exception.BusinessException;
import com.docusphere.common.exception.DuplicateResourceException;
import com.docusphere.common.exception.ResourceNotFoundException;
import com.docusphere.config.JwtProperties;
import com.docusphere.notification.domain.NotificationType;
import com.docusphere.notification.event.NotificationEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationTokenRepository tokenRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final LoginRateLimiterService rateLimiterService;
    private final JwtProperties jwtProperties;


    // =====================================================
    // 1. REGISTER
    // =====================================================
    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        // 1. Vérifier si l'email existe déjà
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new DuplicateResourceException("An account with this email already exists.");
        }

        // 2. Récupérer le rôle par défaut (USER)
        Role userRole = roleRepository.findByName(RoleName.USER)
                .orElseThrow(() -> new ResourceNotFoundException("Default role USER not found in database."));

        // 3. Créer l'utilisateur
        User user = new User();
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setEmail(request.email().toLowerCase().trim()); // Normalisation
        user.setPassword(passwordEncoder.encode(request.password()));

        user.addRole(userRole);

        // 4. Sauvegarder
        User savedUser = userRepository.save(user);

        // Si un ancien token existe pour cet utilisateur (cas de réinscription), on le supprime
        tokenRepository.findByUser(savedUser).ifPresent(tokenRepository::delete);

        // Création du token de vérification
        String otpCode = generateSecureOtpCode();
        EmailVerificationToken verificationToken = new EmailVerificationToken(savedUser, otpCode);
        tokenRepository.save(verificationToken);

        String savedUserFullName = savedUser.getFirstName() + " " + savedUser.getLastName();

        // Publication de l'événement (découplage)
        eventPublisher.publishEvent(
                new UserRegisteredEvent(
                        this,
                        savedUser.getEmail(),
                        savedUserFullName,
                        otpCode
                )
        );

        // Retourner le DTO de réponse
        return new RegisterResponse(
                savedUser.getPublicId(),
                savedUser.getEmail(),
                savedUser.getFirstName(),
                savedUser.getLastName()
        );
    }

    // =====================================================
    // 2. LOGIN
    // =====================================================
    @Transactional
    public LoginResponse login(LoginRequest request) {
        String email = request.email().toLowerCase().trim();

        // Rate Limiting
        if (rateLimiterService.isBlocked(email)) {
            throw new BusinessException(
                    "Too many failed login attempts. Please try again in 15 minutes.",
                    HttpStatus.TOO_MANY_REQUESTS,
                    "LOGIN_BLOCKED"
            );
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.password())
            );
        } catch (AuthenticationException e) {
            rateLimiterService.recordFailure(email);
            throw new BusinessException("Invalid email or password.", HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS");
        }

        // Login réussi : reset le compteur
        rateLimiterService.reset(email);

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new BusinessException("Invalid email or password.", HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS"));

        if (!user.isEmailVerified()) {
            throw new BusinessException("Email not verified. Please check your inbox.", HttpStatus.FORBIDDEN, "EMAIL_NOT_VERIFIED");
        }

        if (!user.isActive()) {
            throw new BusinessException("Account is deactivated.", HttpStatus.FORBIDDEN, "ACCOUNT_DISABLED");
        }

        String accessToken = jwtService.generateAccessToken(user);

        // Révoquer les anciens refresh tokens pour cet utilisateur (optionnel : pour forcer un seul appareil)
        // refreshTokenRepository.deleteByUser(user);

        RefreshToken refreshToken = new RefreshToken(user, jwtProperties.getRefreshTokenExpirationMs());
        refreshTokenRepository.save(refreshToken);

        return new LoginResponse(
                accessToken,
                refreshToken.getToken(),
                "Bearer",
                jwtProperties.getJwtExpirationMs()
        );
    }

    // =====================================================
    // 3. VERIFY EMAIL WITH CODE
    // =====================================================
    @Transactional
    public void verifyEmail(String email, String code) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new BusinessException("Invalid email or code.", HttpStatus.BAD_REQUEST, "INVALID_CREDENTIALS"));

        EmailVerificationToken token = tokenRepository.findByUser(user)
                .orElseThrow(() -> new BusinessException("Invalid email or code.", HttpStatus.BAD_REQUEST, "INVALID_CREDENTIALS"));

        if (token.isExpired()) {
            tokenRepository.delete(token);
            throw new BusinessException("Verification code has expired.", HttpStatus.BAD_REQUEST, "CODE_EXPIRED");
        }

        if (token.isBlocked()) {
            tokenRepository.delete(token); // Sécurité : on nettoie si bloqué
            throw new BusinessException("Too many failed attempts. Please register again.", HttpStatus.BAD_REQUEST, "CODE_BLOCKED");
        }

        if (!token.getCode().equals(code)) {
            token.incrementAttempts();
            tokenRepository.save(token);

            if (token.isBlocked()) {
                tokenRepository.delete(token);
                throw new BusinessException("Too many failed attempts. Please register again.", HttpStatus.BAD_REQUEST, "CODE_BLOCKED");
            }
            throw new BusinessException("Invalid verification code.", HttpStatus.BAD_REQUEST, "INVALID_CODE");
        }

        // Succès
        user.setEmailVerified(true);
        userRepository.save(user);
        tokenRepository.delete(token);
    }

    // =====================================================
    // 4. REFRESH TOKEN
    // =====================================================
    @Transactional
    public LoginResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken storedToken = refreshTokenRepository.findByToken(request.refreshToken())
                .orElseThrow(() -> new BusinessException("Invalid refresh token.", HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN"));

        if (storedToken.isExpired() || storedToken.isRevoked()) {
            throw new BusinessException("Refresh token expired or revoked. Please login again.", HttpStatus.UNAUTHORIZED, "REFRESH_TOKEN_EXPIRED");
        }

        User user = storedToken.getUser();
        String newAccessToken = jwtService.generateAccessToken(user);

        return new LoginResponse(
                newAccessToken,
                storedToken.getToken(), // On garde le même refresh token (Rotation de token peut être ajoutée plus tard)
                "Bearer",
                jwtProperties.getJwtExpirationMs()
        );
    }

    // =====================================================
    // 5. RESEND VERIFICATION CODE
    // =====================================================
    @Transactional
    public void resendVerificationCode(ResendVerificationRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new BusinessException(
                        "If an account exists with this email, a verification code has been sent.",
                        HttpStatus.OK, "VERIFICATION_SENT"
                ));

        if (user.isEmailVerified()) {
            throw new BusinessException("Email is already verified.", HttpStatus.BAD_REQUEST, "ALREADY_VERIFIED");
        }

        // Supprimer l'ancien token s'il existe
        tokenRepository.findByUser(user).ifPresent(tokenRepository::delete);

        // Générer un nouveau code
        String otpCode = generateSecureOtpCode();
        EmailVerificationToken verificationToken = new EmailVerificationToken(user, otpCode);
        tokenRepository.save(verificationToken);

        // Envoyer l'email
        eventPublisher.publishEvent(new UserRegisteredEvent(this, user.getEmail(), user.getFirstName(), otpCode));
    }

    // =====================================================
    // 6. FORGOT PASSWORD
    // =====================================================
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new BusinessException(
                        "If an account exists with this email, a reset code has been sent.",
                        HttpStatus.OK, "RESET_SENT"
                ));

        if (!user.isActive()) {
            throw new BusinessException("Account is deactivated.", HttpStatus.FORBIDDEN, "ACCOUNT_DISABLED");
        }

        // Supprimer l'ancien token de reset s'il existe
        passwordResetTokenRepository.findByUserIdAndDeletedFalse(user.getId())
                .ifPresent(passwordResetTokenRepository::delete);

        // Générer un nouveau code
        PasswordResetToken resetToken = new PasswordResetToken(user);
        passwordResetTokenRepository.save(resetToken);

        // Envoyer l'email de réinitialisation
        eventPublisher.publishEvent(new NotificationEvent(
                this,
                NotificationType.DOCUMENT_SUBMITTED, // On pourrait créer un type PASSWORD_RESET
                user.getId(),
                user.getEmail(),
                "Password Reset Request",
                "Your password reset code is: " + resetToken.getToken() + ". It will expire in 15 minutes.",
                null,
                true
        ));
    }

    // =====================================================
    // 7. RESET PASSWORD
    // =====================================================
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new BusinessException("Invalid email or code.", HttpStatus.BAD_REQUEST, "INVALID_CODE"));

        PasswordResetToken resetToken = passwordResetTokenRepository.findByUserIdAndDeletedFalse(user.getId())
                .orElseThrow(() -> new BusinessException("Invalid email or code.", HttpStatus.BAD_REQUEST, "INVALID_CODE"));

        if (resetToken.isExpired()) {
            passwordResetTokenRepository.delete(resetToken);
            throw new BusinessException("Reset code has expired. Please request a new one.", HttpStatus.BAD_REQUEST, "CODE_EXPIRED");
        }

        if (resetToken.isBlocked()) {
            passwordResetTokenRepository.delete(resetToken);
            throw new BusinessException("Too many failed attempts. Please request a new code.", HttpStatus.BAD_REQUEST, "CODE_BLOCKED");
        }

        if (!resetToken.getToken().equals(request.code())) {
            resetToken.incrementAttempts();
            passwordResetTokenRepository.save(resetToken);

            if (resetToken.isBlocked()) {
                passwordResetTokenRepository.delete(resetToken);
                throw new BusinessException("Too many failed attempts. Please request a new code.", HttpStatus.BAD_REQUEST, "CODE_BLOCKED");
            }
            throw new BusinessException("Invalid reset code.", HttpStatus.BAD_REQUEST, "INVALID_CODE");
        }

        // Succès : mettre à jour le mot de passe
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        // Supprimer le token
        passwordResetTokenRepository.delete(resetToken);

        // Révoquer tous les refresh tokens (sécurité : forcer la reconnexion sur tous les appareils)
        refreshTokenRepository.deleteByUser(user);
    }

    // =====================================================
    // 8. CHANGE PASSWORD (quand connecté)
    // =====================================================
    @Transactional
    public void changePassword(User user, ChangePasswordRequest request) {
        // Vérifier l'ancien mot de passe
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new BusinessException("Current password is incorrect.", HttpStatus.BAD_REQUEST, "WRONG_PASSWORD");
        }

        // Vérifier que le nouveau mot de passe est différent de l'ancien
        if (request.currentPassword().equals(request.newPassword())) {
            throw new BusinessException("New password must be different from current password.", HttpStatus.BAD_REQUEST, "SAME_PASSWORD");
        }

        // Mettre à jour le mot de passe
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        // Révoquer tous les refresh tokens sauf la session actuelle (optionnel : pour forcer la reconnexion ailleurs)
        // Pour simplifier, on révoque tout. L'utilisateur devra se reconnecter avec le nouveau mot de passe.
        refreshTokenRepository.deleteByUser(user);
    }

    // =====================================================
    // 9. LOGOUT
    // =====================================================
    @Transactional
    public void logout(RefreshTokenRequest request) {
        refreshTokenRepository.findByToken(request.refreshToken())
                .ifPresent(token -> {
                    token.setRevoked(true);
                    refreshTokenRepository.save(token);
                });
    }

    @Transactional
    public void verifyEmail(String token) {
        EmailVerificationToken verificationToken = tokenRepository.findByCode(token)
                .orElseThrow(() -> new BusinessException("Invalid verification token.", HttpStatus.BAD_REQUEST, "INVALID_TOKEN"));

        if (verificationToken.isExpired()) {
            tokenRepository.delete(verificationToken);
            throw new BusinessException("Verification token has expired. Please register again or request a new one.",
                    HttpStatus.BAD_REQUEST, "TOKEN_EXPIRED");
        }

        User user = verificationToken.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);

        // Nettoyage du token après utilisation
        tokenRepository.delete(verificationToken);
    }


    // Dans la classe AuthService, ajoute cette méthode privée :
    private String generateSecureOtpCode() {
        SecureRandom random = new SecureRandom();
        int code = random.nextInt(1_000_000); // Génère entre 0 et 999999
        return String.format("%06d", code);    // Formate pour avoir toujours 6 chiffres (ex: 004210)
    }
}
