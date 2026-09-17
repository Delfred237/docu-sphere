package com.docusphere.user.dto;

import com.docusphere.auth.domain.RoleName;
import jakarta.validation.constraints.NotEmpty;
import java.util.Set;

public record UpdateUserRolesRequest(
        @NotEmpty(message = "At least one role is required")
        Set<RoleName> roles
) {}