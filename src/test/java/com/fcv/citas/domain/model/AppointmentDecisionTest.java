package com.fcv.citas.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static com.fcv.citas.domain.model.AppointmentFlagsTest.NOW;
import static com.fcv.citas.domain.model.AppointmentFlagsTest.appointment;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** HU-018 — decisión ADMIN sobre una solicitud especializada. */
class AppointmentDecisionTest {

    @Test
    void ca01ApprovingARequestedAppointmentKeepsItsSlotsAndMovesToApproved() {
        assertThat(appointment(AppointmentStatus.REQUESTED, NOW.plusDays(1), null).decide(true, null))
                .isEqualTo(AppointmentStatus.APPROVED);
    }

    @Test
    void ca02RejectingRequiresAReason() {
        Appointment requested = appointment(AppointmentStatus.REQUESTED, NOW.plusDays(1), null);

        assertThat(requested.decide(false, "Sin cupo sintetico")).isEqualTo(AppointmentStatus.REJECTED);
        assertThatThrownBy(() -> requested.decide(false, "  "))
                .isInstanceOf(DomainRuleViolation.class).extracting("code").isEqualTo("REJECTION_REASON_REQUIRED");
        assertThatThrownBy(() -> requested.decide(false, null))
                .isInstanceOf(DomainRuleViolation.class).extracting("code").isEqualTo("REJECTION_REASON_REQUIRED");
    }

    @ParameterizedTest
    @EnumSource(value = AppointmentStatus.class, names = "REQUESTED", mode = EnumSource.Mode.EXCLUDE)
    void ca03OnlyRequestedAppointmentsCanBeDecided(AppointmentStatus status) {
        assertThatThrownBy(() -> appointment(status, NOW.plusDays(1), null).decide(true, null))
                .isInstanceOf(DomainRuleViolation.class).extracting("code").isEqualTo("INVALID_TRANSITION");
    }
}
