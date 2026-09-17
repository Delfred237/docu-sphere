package com.docusphere.user.dto;

import com.docusphere.auth.domain.User;

import java.util.List;

public record UserProfileResponse(
        String publicId,
        String email,
        String firstName,
        String lastName,
        boolean emailVerified,
        List<String> roles
) {
    public static UserProfileResponse fromEntity(User user) {
        return new UserProfileResponse(
          user.getPublicId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.isEmailVerified(),
                user.getRoles().stream()
                        .map(role -> role.getName().name())
                        .toList()
        );
    }
}
