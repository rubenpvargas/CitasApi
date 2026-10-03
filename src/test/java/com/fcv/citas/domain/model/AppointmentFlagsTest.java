package com.fcv.citas.domain.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/** Indicadores calculados por el backend para la UI (AppointmentDto): cancellable, reschedulable, closable. */
class AppointmentFlagsTest {
    static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 5, 10, 0);

    static Appointment appointment(AppointmentStatus status, LocalDateTime start, PendingReschedule pending) {
        return new Appointment(1L, 7L, "Ana Sintetica", 3L, "Valeria Agenda", 4L, "Cardiologia", false, 5L, "HIC",
                "Sede HIC", status, start, start.plusMinutes(30), "Control", null, pending, null);
    }

    @Test
    void futureRequestedOrApprovedIsCancellable() {
        assertThat(appointment(AppointmentStatus.REQUESTED, NOW.plusHours(1), null).cancellableAt(NOW)).isTrue();
        assertThat(appointment(AppointmentStatus.APPROVED, NOW.plusHours(1), null).cancellableAt(NOW)).isTrue();
        assertThat(appointment(AppointmentStatus.APPROVED, NOW, null).cancellableAt(NOW)).isFalse();
        assertThat(appointment(AppointmentStatus.CANCELLED, NOW.plusHours(1), null).cancellableAt(NOW)).isFalse();
    }

    @Test
    void onlyFutureApprovedWithoutPendingRequestIsReschedulable() {
        PendingReschedule pending = new PendingReschedule(9L, NOW.plusDays(2), NOW.plusDays(2).plusMinutes(30), "ICV");
        assertThat(appointment(AppointmentStatus.APPROVED, NOW.plusHours(1), null).reschedulableAt(NOW)).isTrue();
        assertThat(appointment(AppointmentStatus.APPROVED, NOW.plusHours(1), pending).reschedulableAt(NOW)).isFalse();
        assertThat(appointment(AppointmentStatus.REQUESTED, NOW.plusHours(1), null).reschedulableAt(NOW)).isFalse();
        assertThat(appointment(AppointmentStatus.APPROVED, NOW.minusHours(1), null).reschedulableAt(NOW)).isFalse();
    }

    @Test
    void durationAndTerminalStates() {
        assertThat(appointment(AppointmentStatus.APPROVED, NOW, null).durationMinutes()).isEqualTo(30);
        assertThat(AppointmentStatus.REJECTED.terminal()).isTrue();
        assertThat(AppointmentStatus.NO_SHOW.terminal()).isTrue();
        assertThat(AppointmentStatus.REQUESTED.terminal()).isFalse();
    }
}
