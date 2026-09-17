package com.docusphere.user.dto;

import com.docusphere.auth.domain.User;
import java.util.List;

public record UserSummaryResponse(
        String publicId,
        String email,
        String firstName,
        String lastName,
        boolean active,
        boolean emailVerified,
        List<String> roles
) {
    public static UserSummaryResponse fromEntity(User user) {
        return new UserSummaryResponse(
                user.getPublicId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.isActive(),
                user.isEmailVerified(),
                user.getRoles().stream().map(r -> r.getName().name()).toList()
        );
    }
}