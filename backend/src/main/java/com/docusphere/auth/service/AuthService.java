package com.docusphere.auth.service;

import com.docusphere.auth.domain.Role;
import com.docusphere.auth.domain.RoleName;
import com.docusphere.auth.domain.User;
import com.docusphere.auth.dto.RegisterRequest;
import com.docusphere.auth.dto.RegisterResponse;
import com.docusphere.auth.repository.RoleRepository;
import com.docusphere.auth.repository.UserRepository;
import com.docusphere.common.exception.DuplicateResourceException;
import com.docusphere.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

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

        // 5. Retourner le DTO de réponse
        return new RegisterResponse(
                savedUser.getPublicId(),
                savedUser.getEmail(),
                savedUser.getFirstName(),
                savedUser.getLastName()
        );
    }
}
