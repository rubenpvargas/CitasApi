package com.fcv.citas.application.service;

import com.fcv.citas.application.exception.NotFoundException;
import com.fcv.citas.domain.model.AppointmentStatus;
import com.fcv.citas.domain.model.DomainRuleViolation;
import com.fcv.citas.domain.model.RescheduleRequest;
import com.fcv.citas.domain.model.RescheduleStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.fcv.citas.application.service.AvailabilityBlockServiceTest.CLOCK;
import static com.fcv.citas.application.service.CancelAppointmentServiceTest.FUTURE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * HU-022 — ciclo de vida de la reprogramación (prueba de escritorio): qué slots quedan libres,
 * retenidos o asignados al aprobar o rechazar.
 */
class RescheduleDecisionServiceTest {
    final MovingAppointments appointments = new MovingAppointments();
    final CancelAppointmentServiceTest.RecordingSlots slots = new TransferRecordingSlots();
    final CancelAppointmentServiceTest.RecordingReschedules reschedules = new CancelAppointmentServiceTest.RecordingReschedules();
    final RescheduleService service = new RescheduleService(appointments, reschedules, slots, null, null,
            new DirectTransactions(), CLOCK);
    static final LocalDateTime NEW_START = FUTURE.plusDays(1);

    @BeforeEach
    void seed() {
        appointments.items.add(MyAppointmentsServiceTest.appointment(1L, 7L, AppointmentStatus.APPROVED, FUTURE));
        reschedules.pending = new RescheduleRequest(9L, 1L, 7L, 2L, "ICV", RescheduleStatus.PENDING, FUTURE,
                FUTURE.plusMinutes(30), NEW_START, NEW_START.plusMinutes(30));
    }

    @Test
    void rl1ApproveFreesOldSlotsAssignsHeldSlotsAndMovesTheAppointment() {
        service.decide(99L, 9L, true, null);

        assertThat(slots.releasedAppointments).containsExactly(1L);
        assertThat(((TransferRecordingSlots) slots).transfers).containsExactly("9->1");
        assertThat(slots.releasedHolds).isEmpty();
        assertThat(appointments.moves).containsExactly("1@2:" + NEW_START);
        assertThat(reschedules.statuses).containsExactly(RescheduleStatus.APPROVED);
        assertThat(reschedules.sources).containsExactly("ADMIN");
        assertThat(appointments.historySources).containsExactly("ADMIN");
    }

    @Test
    void rl2RejectFreesTheHoldAndKeepsTheOriginalAssignment() {
        service.decide(99L, 9L, false, "Franja sintetica no disponible");

        assertThat(slots.releasedHolds).containsExactly(9L);
        assertThat(slots.releasedAppointments).isEmpty();
        assertThat(((TransferRecordingSlots) slots).transfers).isEmpty();
        assertThat(appointments.moves).isEmpty();
        assertThat(reschedules.statuses).containsExactly(RescheduleStatus.REJECTED);
    }

    @Test
    void rl3RejectWithoutReasonOrSecondDecisionChangesNothing() {
        assertThatThrownBy(() -> service.decide(99L, 9L, false, null))
                .isInstanceOf(DomainRuleViolation.class).extracting("code").isEqualTo("REJECTION_REASON_REQUIRED");
        reschedules.pending = new RescheduleRequest(9L, 1L, 7L, 2L, "ICV", RescheduleStatus.APPROVED, FUTURE,
                FUTURE.plusMinutes(30), NEW_START, NEW_START.plusMinutes(30));
        assertThatThrownBy(() -> service.decide(99L, 9L, true, null))
                .isInstanceOf(DomainRuleViolation.class).extracting("code").isEqualTo("INVALID_TRANSITION");
        assertThat(slots.releasedAppointments).isEmpty();
        assertThat(slots.releasedHolds).isEmpty();
        assertThat(reschedules.statuses).isEmpty();
    }

    @Test
    void unknownRequestIsNotFound() {
        reschedules.pending = null;
        assertThatThrownBy(() -> service.decide(99L, 9L, true, null)).isInstanceOf(NotFoundException.class);
    }

    static final class MovingAppointments extends MyAppointmentsServiceTest.InMemoryAppointments {
        final List<String> moves = new ArrayList<>();
        final List<String> historySources = new ArrayList<>();
        @Override public void moveTo(long id, long locationId, LocalDateTime startAt, LocalDateTime endAt, Instant at) {
            moves.add(id + "@" + locationId + ":" + startAt);
        }
        @Override public void addHistory(long id, AppointmentStatus status, Long actor, String source, String reason, Instant at) {
            historySources.add(source);
        }
    }

    static final class TransferRecordingSlots extends CancelAppointmentServiceTest.RecordingSlots {
        final List<String> transfers = new ArrayList<>();
        @Override public void transferHoldToAppointment(long requestId, long appointmentId) {
            transfers.add(requestId + "->" + appointmentId);
        }
    }
}
