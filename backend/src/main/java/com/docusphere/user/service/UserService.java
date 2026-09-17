package com.docusphere.user.service;

import com.docusphere.auth.domain.Role;
import com.docusphere.auth.domain.User;
import com.docusphere.auth.repository.RoleRepository;
import com.docusphere.auth.repository.UserRepository;
import com.docusphere.common.exception.BusinessException;
import com.docusphere.common.exception.ResourceNotFoundException;
import com.docusphere.user.dto.UpdateUserRolesRequest;
import com.docusphere.user.dto.UserProfileResponse;
import com.docusphere.user.dto.UserSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentUserProfile(User user) {
        return UserProfileResponse.fromEntity(user);
    }

    @Transactional(readOnly = true)
    public Page<UserSummaryResponse> listUsers(Pageable pageable) {
        return userRepository.findAllWithRoles(pageable)
                .map(UserSummaryResponse::fromEntity);
    }

    @Transactional
    public UserSummaryResponse updateUserRoles(String userPublicId, UpdateUserRolesRequest request) {
        User user = userRepository.findByPublicId(userPublicId) // Il faudra ajouter cette méthode au repo
                .orElseThrow(() -> new ResourceNotFoundException("User", "publicId", userPublicId));

        // Protection métier : Un admin ne devrait pas pouvoir se retirer ses propres droits (optionnel mais recommandé)
        // Ici on reste simple.

        Set<Role> newRoles = request.roles().stream()
                .map(roleName -> roleRepository.findByName(roleName)
                        .orElseThrow(() -> new BusinessException("Role not found: " + roleName, HttpStatus.BAD_REQUEST, "ROLE_NOT_FOUND")))
                .collect(Collectors.toSet());

        user.setRoles(newRoles);
        User savedUser = userRepository.save(user);
        return UserSummaryResponse.fromEntity(savedUser);
    }
}
