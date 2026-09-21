package com.docusphere.auth.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitaires des règles métier du token de réinitialisation.
 * Pas de Spring, pas de DB : tests ultra-rapides.
 */
class PasswordResetTokenTest {

    private final User dummyUser = new User();

    @Test
    @DisplayName("Le token généré doit contenir exactement 6 chiffres")
    void shouldGenerateSixDigitCode() {
        PasswordResetToken token = new PasswordResetToken(dummyUser);

        assertThat(token.getToken())
                .hasSize(6)
                .matches("\\d{6}");
    }

    @Test
    @DisplayName("Un token fraîchement créé ne doit pas être expiré")
    void shouldNotBeExpiredWhenCreated() {
        PasswordResetToken token = new PasswordResetToken(dummyUser);

        assertThat(token.isExpired()).isFalse();
    }

    @Test
    @DisplayName("Un token ne doit pas être bloqué à la création")
    void shouldNotBeBlockedWhenCreated() {
        PasswordResetToken token = new PasswordResetToken(dummyUser);

        assertThat(token.isBlocked()).isFalse();
        assertThat(token.getAttempts()).isZero();
    }

    @Test
    @DisplayName("Le token doit être bloqué après 3 tentatives échouées")
    void shouldBeBlockedAfterThreeFailedAttempts() {
        PasswordResetToken token = new PasswordResetToken(dummyUser);

        token.incrementAttempts();
        token.incrementAttempts();
        assertThat(token.isBlocked()).isFalse();

        token.incrementAttempts();
        assertThat(token.isBlocked()).isTrue();
    }
}