package com.docusphere.user.controller;

import com.docusphere.auth.domain.User;
import com.docusphere.user.dto.UpdateUserRolesRequest;
import com.docusphere.user.dto.UserProfileResponse;
import com.docusphere.user.dto.UserSummaryResponse;
import com.docusphere.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getCurrentProfile(@AuthenticationPrincipal User user) {
        // userDetails.getUsername() contient l'email que nous avons configuré dans CustomUserDetailsService
        UserProfileResponse profile = userService.getCurrentUserProfile(user);
        return ResponseEntity.ok(profile);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')") // Ou hasAuthority('USER_MANAGE')
    public ResponseEntity<Page<UserSummaryResponse>> listUsers(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(userService.listUsers(pageable));
    }

    @PatchMapping("/{publicId}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserSummaryResponse> updateRoles(
            @PathVariable String publicId,
            @Valid @RequestBody UpdateUserRolesRequest request) {
        return ResponseEntity.ok(userService.updateUserRoles(publicId, request));
    }
}
