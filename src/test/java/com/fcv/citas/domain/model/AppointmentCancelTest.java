package com.fcv.citas.domain.model;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static com.fcv.citas.domain.model.AppointmentFlagsTest.NOW;
import static com.fcv.citas.domain.model.AppointmentFlagsTest.appointment;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** HU-020 — tabla de cancelación: estado × futura/pasada (prueba de escritorio). */
class AppointmentCancelTest {

    @ParameterizedTest(name = "{0} con inicio en {1} min → {2}")
    @CsvSource({
            "REQUESTED, 60, OK",
            "APPROVED, 60, OK",
            "APPROVED, 0, INVALID_TRANSITION",
            "REQUESTED, -30, INVALID_TRANSITION",
            "CANCELLED, 60, INVALID_TRANSITION",
            "REJECTED, 60, INVALID_TRANSITION",
            "COMPLETED, -60, INVALID_TRANSITION",
            "NO_SHOW, -60, INVALID_TRANSITION"
    })
    void cancellationTable(AppointmentStatus status, long minutesFromNow, String expected) {
        Appointment a = appointment(status, NOW.plusMinutes(minutesFromNow), null);
        if ("OK".equals(expected)) {
            assertThatCode(() -> a.requireCancellable(NOW)).doesNotThrowAnyException();
        } else {
            assertThatThrownBy(() -> a.requireCancellable(NOW))
                    .isInstanceOf(DomainRuleViolation.class).extracting("code").isEqualTo(expected);
        }
    }
}
