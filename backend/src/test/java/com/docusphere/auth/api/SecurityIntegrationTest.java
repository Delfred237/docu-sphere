package com.docusphere.auth.api;

import com.docusphere.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de sécurité : vérifie que les routes protégées
 * sont bien protégées et que les routes publiques sont accessibles.
 */
class SecurityIntegrationTest extends BaseIntegrationTest {

    @Test
    @DisplayName("GET /users/me : doit retourner 401 sans token")
    void shouldRejectUnauthenticatedRequest() throws Exception {
        mockMvc.perform(get("/v1/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /documents : doit retourner 401 sans token")
    void shouldRejectUnauthenticatedDocumentAccess() throws Exception {
        mockMvc.perform(get("/v1/documents"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /users/me : doit retourner 403 avec un token invalide")
    void shouldRejectInvalidToken() throws Exception {
        mockMvc.perform(get("/v1/users/me")
                        .header("Authorization", "Bearer invalid.token.here"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /actuator/health : doit être accessible sans authentification")
    void shouldAllowPublicHealthEndpoint() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }
}