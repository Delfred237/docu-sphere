package com.docusphere.auth.service;

import com.docusphere.auth.domain.EmailVerificationToken;
import com.docusphere.auth.domain.Role;
import com.docusphere.auth.domain.RoleName;
import com.docusphere.auth.domain.User;
import com.docusphere.auth.dto.RegisterRequest;
import com.docusphere.auth.dto.RegisterResponse;
import com.docusphere.auth.event.UserRegisteredEvent;
import com.docusphere.auth.repository.EmailVerificationTokenRepository;
import com.docusphere.auth.repository.RoleRepository;
import com.docusphere.auth.repository.UserRepository;
import com.docusphere.common.exception.BusinessException;
import com.docusphere.common.exception.DuplicateResourceException;
import com.docusphere.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationTokenRepository tokenRepository;
    private final ApplicationEventPublisher eventPublisher;

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

        // Création du token de vérification
        EmailVerificationToken verificationToken = new EmailVerificationToken(savedUser);
        tokenRepository.save(verificationToken);

        // Publication de l'événement (découplage)
        eventPublisher.publishEvent(
                new UserRegisteredEvent(
                        this,
                        savedUser.getEmail(),
                        verificationToken.getToken()
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

    // Ajoute la méthode de vérification
    @Transactional
    public void verifyEmail(String token) {
        EmailVerificationToken verificationToken = tokenRepository.findByToken(token)
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
}
