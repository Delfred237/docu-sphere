package com.docusphere.user.controller;

import com.docusphere.auth.domain.User;
import com.docusphere.user.dto.UserProfileResponse;
import com.docusphere.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
