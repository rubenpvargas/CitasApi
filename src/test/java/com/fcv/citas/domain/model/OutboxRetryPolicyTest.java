package com.fcv.citas.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * WF-002 — despacho del outbox (prueba de escritorio): intento → estado / next_attempt_at.
 * Base 30 s, máximo 3 intentos, ahora = 2026-10-04T15:00:00Z.
 */
class OutboxRetryPolicyTest {
    private static final Instant NOW = Instant.parse("2026-10-04T15:00:00Z");
    private final OutboxRetryPolicy policy = new OutboxRetryPolicy(3, Duration.ofSeconds(30));

    @Test
    void ob1SuccessfulDeliveryIsSent() {
        assertThat(policy.afterAttempt(1, true, NOW)).isEqualTo(new OutboxRetryPolicy.Outcome(OutboxStatus.SENT, null));
    }

    @Test
    void ob2FirstFailureRetriesAfterBaseDelay() {
        assertThat(policy.afterAttempt(1, false, NOW))
                .isEqualTo(new OutboxRetryPolicy.Outcome(OutboxStatus.PENDING, NOW.plusSeconds(30)));
    }

    @Test
    void ob3SecondFailureDoublesTheDelay() {
        assertThat(policy.afterAttempt(2, false, NOW))
                .isEqualTo(new OutboxRetryPolicy.Outcome(OutboxStatus.PENDING, NOW.plusSeconds(60)));
    }

    @Test
    void ob4ThirdFailureIsTerminalFailed() {
        assertThat(policy.afterAttempt(3, false, NOW)).isEqualTo(new OutboxRetryPolicy.Outcome(OutboxStatus.FAILED, null));
    }

    @Test
    void ob5SuccessOnLastAttemptIsStillSent() {
        assertThat(policy.afterAttempt(3, true, NOW).status()).isEqualTo(OutboxStatus.SENT);
    }
}
