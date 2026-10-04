package com.fcv.citas.domain.model;

import java.time.Duration;
import java.time.Instant;

/**
 * WF-002 — política de reintentos del despachador: un 2xx deja el mensaje SENT; un fallo en el intento n
 * (n &lt; máximo) lo reprograma tras base·2^(n−1); el fallo del último intento lo deja FAILED.
 */
public final class OutboxRetryPolicy {
    private final int maxAttempts;
    private final Duration baseDelay;

    public record Outcome(OutboxStatus status, Instant nextAttemptAt) {
    }

    public OutboxRetryPolicy(int maxAttempts, Duration baseDelay) {
        if (maxAttempts < 1 || baseDelay.isNegative()) {
            throw new IllegalArgumentException("Invalid retry policy");
        }
        this.maxAttempts = maxAttempts;
        this.baseDelay = baseDelay;
    }

    /** @param attempt número del intento recién realizado (1 = primero) */
    public Outcome afterAttempt(int attempt, boolean delivered, Instant now) {
        if (delivered) {
            return new Outcome(OutboxStatus.SENT, null);
        }
        if (attempt >= maxAttempts) {
            return new Outcome(OutboxStatus.FAILED, null);
        }
        return new Outcome(OutboxStatus.PENDING, now.plus(baseDelay.multipliedBy(1L << (attempt - 1))));
    }
}
