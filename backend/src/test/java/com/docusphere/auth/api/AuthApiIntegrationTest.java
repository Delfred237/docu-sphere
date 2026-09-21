package com.docusphere.auth.api;

import com.docusphere.BaseIntegrationTest;
import com.docusphere.auth.domain.EmailVerificationToken;
import com.docusphere.auth.domain.User;
import com.docusphere.auth.repository.EmailVerificationTokenRepository;
import com.docusphere.auth.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthApiIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailVerificationTokenRepository emailVerificationTokenRepository;

    @Test
    @DisplayName("POST /register : doit créer un compte et retourner 201")
    void shouldRegisterNewUser() throws Exception {
        String email = randomEmail();

        mockMvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(email)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.publicId").exists());
    }

    @Test
    @DisplayName("POST /register : doit retourner 409 pour un email déjà utilisé")
    void shouldRejectDuplicateEmail() throws Exception {
        String email = randomEmail();

        mockMvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(email)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(email)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_RESOURCE"));
    }

    @Test
    @DisplayName("POST /register : doit retourner 400 pour des données invalides")
    void shouldRejectInvalidPayload() throws Exception {
        String invalidBody = """
            {
                "firstName": "",
                "lastName": "Doe",
                "email": "invalid-email",
                "password": "123"
            }
            """;

        mockMvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    @DisplayName("Flux complet : register → récupérer OTP en DB → verify → login")
    void shouldCompleteFullRegistrationFlow() throws Exception {
        String email = randomEmail();

        // 1. Inscription
        mockMvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(email)))
                .andExpect(status().isCreated());

        // 2. Récupérer le code OTP directement depuis la base de données (H2)
        User user = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        EmailVerificationToken token = emailVerificationTokenRepository.findByUser(user).orElseThrow();
        String code = token.getCode();
        assertThat(code).matches("\\d{6}");

        // 3. Vérifier l'email avec le code
        mockMvc.perform(post("/v1/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "code", code))))
                .andExpect(status().isOk());

        // 4. Connexion réussie
        mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", email, "password", "Password123!"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists());
    }

    @Test
    @DisplayName("POST /login : doit refuser un email non vérifié")
    void shouldRejectLoginForUnverifiedEmail() throws Exception {
        String email = randomEmail();

        mockMvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(email)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", email, "password", "Password123!"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("EMAIL_NOT_VERIFIED"));
    }

    @Test
    @DisplayName("POST /login : doit retourner 401 avec de mauvais identifiants")
    void shouldRejectInvalidCredentials() throws Exception {
        String email = randomEmail();
        registerAndVerifyUser(email);

        mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", email, "password", "WrongPassword!"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    // ============ HELPERS ============

    private String randomEmail() {
        return "user-" + UUID.randomUUID() + "@test.com";
    }

    private String registerBody(String email) {
        return """
            {
                "firstName": "Test",
                "lastName": "User",
                "email": "%s",
                "password": "Password123!"
            }
            """.formatted(email);
    }

    protected void registerAndVerifyUser(String email) throws Exception {
        mockMvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(email)))
                .andExpect(status().isCreated());

        User user = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        user.setEmailVerified(true);
        userRepository.save(user);
    }
}