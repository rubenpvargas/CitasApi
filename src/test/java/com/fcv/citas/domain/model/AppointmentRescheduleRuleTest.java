package com.fcv.citas.domain.model;

import org.junit.jupiter.api.Test;

import static com.fcv.citas.domain.model.AppointmentFlagsTest.NOW;
import static com.fcv.citas.domain.model.AppointmentFlagsTest.appointment;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** HU-021 — solo una cita APPROVED futura sin otra solicitud PENDING puede reprogramarse. */
class AppointmentRescheduleRuleTest {

    @Test
    void futureApprovedWithoutPendingRequestIsAccepted() {
        assertThatCode(() -> appointment(AppointmentStatus.APPROVED, NOW.plusDays(1), null).requireReschedulable(NOW))
                .doesNotThrowAnyException();
    }

    @Test
    void ca03NotApprovedOrNotFutureIsInvalidTransition() {
        assertThatThrownBy(() -> appointment(AppointmentStatus.REQUESTED, NOW.plusDays(1), null).requireReschedulable(NOW))
                .isInstanceOf(DomainRuleViolation.class).extracting("code").isEqualTo("INVALID_TRANSITION");
        assertThatThrownBy(() -> appointment(AppointmentStatus.APPROVED, NOW, null).requireReschedulable(NOW))
                .isInstanceOf(DomainRuleViolation.class).extracting("code").isEqualTo("INVALID_TRANSITION");
        assertThatThrownBy(() -> appointment(AppointmentStatus.CANCELLED, NOW.plusDays(1), null).requireReschedulable(NOW))
                .isInstanceOf(DomainRuleViolation.class).extracting("code").isEqualTo("INVALID_TRANSITION");
    }

    @Test
    void secondPendingRequestIsRejected() {
        PendingReschedule pending = new PendingReschedule(9L, NOW.plusDays(3), NOW.plusDays(3).plusMinutes(30), "HIC");

        assertThatThrownBy(() -> appointment(AppointmentStatus.APPROVED, NOW.plusDays(1), pending).requireReschedulable(NOW))
                .isInstanceOf(DomainRuleViolation.class).extracting("code").isEqualTo("RESCHEDULE_ALREADY_PENDING");
    }
}
