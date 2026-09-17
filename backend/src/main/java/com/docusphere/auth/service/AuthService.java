package com.docusphere.auth.service;

import com.docusphere.auth.domain.*;
import com.docusphere.auth.dto.*;
import com.docusphere.auth.event.UserRegisteredEvent;
import com.docusphere.auth.repository.EmailVerificationTokenRepository;
import com.docusphere.auth.repository.RefreshTokenRepository;
import com.docusphere.auth.repository.RoleRepository;
import com.docusphere.auth.repository.UserRepository;
import com.docusphere.common.exception.BusinessException;
import com.docusphere.common.exception.DuplicateResourceException;
import com.docusphere.common.exception.ResourceNotFoundException;
import com.docusphere.config.JwtProperties;
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
    private final JwtProperties jwtProperties;

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

    @Transactional
    public LoginResponse login(LoginRequest request) {
        try {
            // Spring Security valide le mot de passe via le PasswordEncoder
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );
        } catch (AuthenticationException e) {
            throw new BusinessException("Invalid email or password.", HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS");
        }

        User user = userRepository.findByEmailIgnoreCase(request.email())
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

    @Transactional
    public void logout(RefreshTokenRequest request) {
        refreshTokenRepository.findByToken(request.refreshToken())
                .ifPresent(token -> {
                    token.setRevoked(true);
                    refreshTokenRepository.save(token);
                });
    }

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


    // Dans la classe AuthService, ajoute cette méthode privée :
    private String generateSecureOtpCode() {
        SecureRandom random = new SecureRandom();
        int code = random.nextInt(1_000_000); // Génère entre 0 et 999999
        return String.format("%06d", code);    // Formate pour avoir toujours 6 chiffres (ex: 004210)
    }
}
