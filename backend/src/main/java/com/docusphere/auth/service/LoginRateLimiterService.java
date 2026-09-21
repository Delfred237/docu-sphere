package com.docusphere.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiting simple en mémoire pour le MVP.
 * En production, utiliser Redis ou un API Gateway.
 */
@Slf4j
@Service
public class LoginRateLimiterService {

    private static final int MAX_ATTEMPTS = 5;
    private static final int WINDOW_MINUTES = 15;

    private final ConcurrentHashMap<String, AttemptRecord> attempts = new ConcurrentHashMap<>();

    public record AttemptRecord(int count, Instant firstAttempt) {}

    public boolean isBlocked(String email) {
        AttemptRecord record = attempts.get(email.toLowerCase());
        if (record == null) return false;

        // Si la fenêtre est écoulée, on nettoie
        if (Instant.now().isAfter(record.firstAttempt().plusSeconds(WINDOW_MINUTES * 60))) {
            attempts.remove(email.toLowerCase());
            return false;
        }

        return record.count() >= MAX_ATTEMPTS;
    }

    public void recordFailure(String email) {
        attempts.compute(email.toLowerCase(), (key, existing) -> {
            if (existing == null || Instant.now().isAfter(existing.firstAttempt().plusSeconds(WINDOW_MINUTES * 60))) {
                return new AttemptRecord(1, Instant.now());
            }
            return new AttemptRecord(existing.count() + 1, existing.firstAttempt());
        });
    }

    public void reset(String email) {
        attempts.remove(email.toLowerCase());
    }
}