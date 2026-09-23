package com.docusphere.auth.api;

import com.docusphere.BaseTestcontainersIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests with real PostgreSQL via Testcontainers.
 * Only runs in CI/CD where Docker is available.
 */
@Tag("integration")
class AuthApiTestcontainersIntegrationTest extends BaseTestcontainersIntegrationTest {

    @Test
    @DisplayName("Register with real PostgreSQL should work")
    void shouldRegisterUserWithRealDatabase() throws Exception {
        String email = "test-" + UUID.randomUUID() + "@test.com";

        mockMvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {
                        "firstName": "Test",
                        "lastName": "User",
                        "email": "%s",
                        "password": "Password123!"
                    }
                    """.formatted(email)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email));
    }
}