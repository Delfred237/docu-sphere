package com.docusphere.auth.service;

import com.docusphere.auth.domain.EmailVerificationToken;
import com.docusphere.auth.domain.PasswordResetToken;
import com.docusphere.auth.domain.RefreshToken;
import com.docusphere.auth.domain.Role;
import com.docusphere.auth.domain.RoleName;
import com.docusphere.auth.domain.User;
import com.docusphere.auth.dto.ChangePasswordRequest;
import com.docusphere.auth.dto.LoginRequest;
import com.docusphere.auth.dto.LoginResponse;
import com.docusphere.auth.dto.RegisterRequest;
import com.docusphere.auth.dto.RegisterResponse;
import com.docusphere.auth.dto.ResetPasswordRequest;
import com.docusphere.auth.event.UserRegisteredEvent;
import com.docusphere.auth.repository.EmailVerificationTokenRepository;
import com.docusphere.auth.repository.PasswordResetTokenRepository;
import com.docusphere.auth.repository.RefreshTokenRepository;
import com.docusphere.auth.repository.RoleRepository;
import com.docusphere.auth.repository.UserRepository;
import com.docusphere.common.exception.BusinessException;
import com.docusphere.common.exception.DuplicateResourceException;
import com.docusphere.common.metrics.BusinessMetrics;
import com.docusphere.config.JwtProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitaires du service d'authentification.
 * Toutes les dépendances sont mockées : on teste uniquement la logique métier.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private EmailVerificationTokenRepository emailVerificationTokenRepository;
    @Mock private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtService jwtService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private LoginRateLimiterService rateLimiterService;
    @Mock private JwtProperties jwtProperties;
    @Mock private BusinessMetrics businessMetrics;

    @InjectMocks private AuthService authService;

    // ============ REGISTER ============

    @Test
    @DisplayName("Register : doit rejeter un email déjà existant")
    void registerShouldRejectDuplicateEmail() {
        RegisterRequest request = new RegisterRequest("John", "Doe", "john@test.com", "Password123!");
        when(userRepository.existsByEmailIgnoreCase("john@test.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(DuplicateResourceException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Register : doit hasher le mot de passe avant de sauvegarder")
    void registerShouldEncodePassword() {
        RegisterRequest request = new RegisterRequest("John", "Doe", "john@test.com", "Password123!");
        when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(roleRepository.findByName(RoleName.USER)).thenReturn(Optional.of(new Role()));
        when(passwordEncoder.encode("Password123!")).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(emailVerificationTokenRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        authService.register(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPassword()).isEqualTo("hashedPassword");
    }

    @Test
    @DisplayName("Register : doit publier un événement pour l'envoi de l'email")
    void registerShouldPublishEvent() {
        RegisterRequest request = new RegisterRequest("John", "Doe", "john@test.com", "Password123!");
        when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(roleRepository.findByName(RoleName.USER)).thenReturn(Optional.of(new Role()));
        when(passwordEncoder.encode(anyString())).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(emailVerificationTokenRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        authService.register(request);

        verify(eventPublisher).publishEvent(any(UserRegisteredEvent.class));
    }

    // ============ LOGIN ============

    @Test
    @DisplayName("Login : doit bloquer après trop de tentatives")
    void loginShouldBeBlockedWhenRateLimited() {
        LoginRequest request = new LoginRequest("john@test.com", "Password123!");
        when(rateLimiterService.isBlocked(anyString())).thenReturn(true);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Too many failed login attempts");

        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
    @DisplayName("Login : doit enregistrer l'échec avec des identifiants invalides")
    void loginShouldRecordFailureOnBadCredentials() {
        LoginRequest request = new LoginRequest("john@test.com", "wrongPassword");
        when(rateLimiterService.isBlocked(anyString())).thenReturn(false);
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class);

        verify(rateLimiterService).recordFailure(anyString());
    }

    @Test
    @DisplayName("Login : doit refuser un email non vérifié")
    void loginShouldRejectUnverifiedEmail() {
        LoginRequest request = new LoginRequest("john@test.com", "Password123!");
        User unverifiedUser = buildUser(false);

        when(rateLimiterService.isBlocked(anyString())).thenReturn(false);
        mockSuccessfulAuthentication("john@test.com");
        when(userRepository.findByEmailIgnoreCase("john@test.com")).thenReturn(Optional.of(unverifiedUser));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Email not verified");
    }

    @Test
    @DisplayName("Login : doit retourner access et refresh tokens si tout est valide")
    void loginShouldReturnTokensWhenValid() {
        LoginRequest request = new LoginRequest("john@test.com", "Password123!");
        User verifiedUser = buildUser(true);

        when(rateLimiterService.isBlocked(anyString())).thenReturn(false);
        mockSuccessfulAuthentication("john@test.com");
        when(userRepository.findByEmailIgnoreCase("john@test.com")).thenReturn(Optional.of(verifiedUser));
        when(jwtService.generateAccessToken(any())).thenReturn("access-token");
        when(jwtProperties.getJwtExpirationMs()).thenReturn(900000L);
        when(jwtProperties.getRefreshTokenExpirationMs()).thenReturn(604800000L);
        when(refreshTokenRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        LoginResponse response = authService.login(request);

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isNotBlank();
        assertThat(response.tokenType()).isEqualTo("Bearer");
        verify(rateLimiterService).reset(anyString());
    }

    // ============ RESET PASSWORD ============

    @Test
    @DisplayName("Reset : doit rejeter un code invalide")
    void resetShouldRejectInvalidCode() {
        User user = buildUser(true);
        PasswordResetToken token = new PasswordResetToken(user);
        ResetPasswordRequest request = new ResetPasswordRequest("john@test.com", "000000", "NewPassword123!");

        when(userRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.of(user));
        when(passwordResetTokenRepository.findByUserIdAndDeletedFalse(user.getId()))
                .thenReturn(Optional.of(token));

        // On force un code différent de celui généré
        ResetPasswordRequest wrongRequest = new ResetPasswordRequest(
                "john@test.com", "999999", "NewPassword123!");

        assertThatThrownBy(() -> authService.resetPassword(wrongRequest))
                .isInstanceOf(BusinessException.class);

        verify(passwordEncoder, never()).encode(anyString());
    }

    // ============ CHANGE PASSWORD ============

    @Test
    @DisplayName("Change : doit rejeter si l'ancien mot de passe est incorrect")
    void changePasswordShouldRejectWrongCurrentPassword() {
        User user = buildUser(true);
        user.setPassword("encodedOldPassword");
        ChangePasswordRequest request = new ChangePasswordRequest("wrongOldPassword", "NewPassword123!");

        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThatThrownBy(() -> authService.changePassword(user, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Current password is incorrect");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Change : doit hasher le nouveau mot de passe et révoquer les sessions")
    void changePasswordShouldEncodeNewPasswordAndRevokeSessions() {
        User user = buildUser(true);
        user.setPassword("encodedOldPassword");
        ChangePasswordRequest request = new ChangePasswordRequest("oldPassword", "NewPassword123!");

        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(passwordEncoder.encode("NewPassword123!")).thenReturn("encodedNewPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        authService.changePassword(user, request);

        assertThat(user.getPassword()).isEqualTo("encodedNewPassword");
        verify(refreshTokenRepository).deleteByUser(user);
    }

    // ============ HELPERS ============

    private User buildUser(boolean emailVerified) {
        User user = new User();
        user.setId(1L);
        user.setEmail("john@test.com");
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setPassword("encodedPassword");
        user.setEmailVerified(emailVerified);
        user.setActive(true);
        return user;
    }

    private void mockSuccessfulAuthentication(String email) {
        var authentication = new UsernamePasswordAuthenticationToken(
                email, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
    }
}