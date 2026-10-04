package com.fcv.citas.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** HU-022 — decisión ADMIN sobre una reprogramación. */
class RescheduleDecisionTest {
    private static final LocalDateTime T = LocalDateTime.of(2026, 10, 8, 8, 0);

    static RescheduleRequest request(RescheduleStatus status) {
        return new RescheduleRequest(9L, 1L, 7L, 2L, "ICV", status, T, T.plusMinutes(30), T.plusDays(1),
                T.plusDays(1).plusMinutes(30));
    }

    @Test
    void ca01Ca02PendingCanBeApprovedOrRejectedWithReason() {
        assertThat(request(RescheduleStatus.PENDING).decide(true, null)).isEqualTo(RescheduleStatus.APPROVED);
        assertThat(request(RescheduleStatus.PENDING).decide(false, "Sin cupo")).isEqualTo(RescheduleStatus.REJECTED);
        assertThatThrownBy(() -> request(RescheduleStatus.PENDING).decide(false, " "))
                .isInstanceOf(DomainRuleViolation.class).extracting("code").isEqualTo("REJECTION_REASON_REQUIRED");
    }

    @ParameterizedTest
    @EnumSource(value = RescheduleStatus.class, names = "PENDING", mode = EnumSource.Mode.EXCLUDE)
    void ca03AlreadyDecidedOrClosedRequestsCannotBeDecidedAgain(RescheduleStatus status) {
        assertThatThrownBy(() -> request(status).decide(true, null))
                .isInstanceOf(DomainRuleViolation.class).extracting("code").isEqualTo("INVALID_TRANSITION");
    }
}
