package com.fcv.citas.domain.model;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static com.fcv.citas.domain.model.AppointmentFlagsTest.NOW;
import static com.fcv.citas.domain.model.AppointmentFlagsTest.appointment;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** HU-024 — tabla de elegibilidad de cierre (prueba de escritorio). */
class AppointmentCloseTest {

    @ParameterizedTest(name = "{0}, inicio {1} min, resultado {2} → {3}")
    @CsvSource({
            "APPROVED, -30, COMPLETED, COMPLETED",
            "APPROVED, 0, NO_SHOW, NO_SHOW",
            "APPROVED, 30, COMPLETED, INVALID_TRANSITION",
            "REQUESTED, -30, COMPLETED, INVALID_TRANSITION",
            "CANCELLED, -30, NO_SHOW, INVALID_TRANSITION",
            "COMPLETED, -30, NO_SHOW, INVALID_TRANSITION",
            "APPROVED, -30, CANCELLED, INVALID_TRANSITION"
    })
    void closeEligibility(AppointmentStatus status, long minutesFromNow, AppointmentStatus outcome, String expected) {
        Appointment a = appointment(status, NOW.plusMinutes(minutesFromNow), null);
        if (expected.equals("INVALID_TRANSITION")) {
            assertThatThrownBy(() -> a.closeTo(outcome, NOW))
                    .isInstanceOf(DomainRuleViolation.class).extracting("code").isEqualTo(expected);
        } else {
            assertThat(a.closeTo(outcome, NOW)).isEqualTo(AppointmentStatus.valueOf(expected));
        }
    }
}
