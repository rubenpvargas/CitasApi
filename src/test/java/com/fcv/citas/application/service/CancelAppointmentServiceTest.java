package com.fcv.citas.application.service;

import com.fcv.citas.application.exception.NotFoundException;
import com.fcv.citas.application.model.InboxFilter;
import com.fcv.citas.application.model.NewReschedule;
import com.fcv.citas.application.model.RescheduleInboxItem;
import com.fcv.citas.application.port.out.RescheduleRepositoryPort;
import com.fcv.citas.application.port.out.SlotRepositoryPort;
import com.fcv.citas.domain.model.Appointment;
import com.fcv.citas.domain.model.AppointmentStatus;
import com.fcv.citas.domain.model.DomainRuleViolation;
import com.fcv.citas.domain.model.LockedSlot;
import com.fcv.citas.domain.model.RescheduleRequest;
import com.fcv.citas.domain.model.RescheduleStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.fcv.citas.application.service.AvailabilityBlockServiceTest.CLOCK;
import static com.fcv.citas.application.service.AvailabilityBlockServiceTest.TODAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** HU-020 — cancelación: libera slots y cierra la reprogramación pendiente liberando su retención. */
class CancelAppointmentServiceTest {
    final MyAppointmentsServiceTest.InMemoryAppointments appointments = new RecordingAppointments();
    final RecordingSlots slots = new RecordingSlots();
    final RecordingReschedules reschedules = new RecordingReschedules();
    final MyAppointmentsService service = new MyAppointmentsService(appointments, slots, reschedules,
            new DirectTransactions(), CLOCK);
    static final LocalDateTime FUTURE = TODAY.plusDays(2).atTime(8, 0);

    @Test
    void ca01CancelsReleasesSlotsAndClosesPendingRescheduleAsSystem() {
        appointments.items.add(MyAppointmentsServiceTest.appointment(1L, 7L, AppointmentStatus.APPROVED, FUTURE));
        reschedules.pending = new RescheduleRequest(9L, 1L, 7L, 2L, "ICV", RescheduleStatus.PENDING, FUTURE,
                FUTURE.plusMinutes(30), FUTURE.plusDays(1), FUTURE.plusDays(1).plusMinutes(30));

        service.cancel(7L, 1L);

        assertThat(((RecordingAppointments) appointments).statuses).containsExactly(AppointmentStatus.CANCELLED);
        assertThat(((RecordingAppointments) appointments).sources).containsExactly("USER");
        assertThat(slots.releasedAppointments).containsExactly(1L);
        assertThat(slots.releasedHolds).containsExactly(9L);
        assertThat(reschedules.statuses).containsExactly(RescheduleStatus.CANCELLED);
        assertThat(reschedules.sources).containsExactly("SYSTEM");
    }

    @Test
    void ca02ForeignPastOrTerminalAppointmentsAreRejectedWithoutTransition() {
        appointments.items.add(MyAppointmentsServiceTest.appointment(1L, 7L, AppointmentStatus.APPROVED, FUTURE));
        appointments.items.add(MyAppointmentsServiceTest.appointment(2L, 7L, AppointmentStatus.APPROVED, TODAY.atTime(9, 0)));
        appointments.items.add(MyAppointmentsServiceTest.appointment(3L, 7L, AppointmentStatus.CANCELLED, FUTURE));

        assertThatThrownBy(() -> service.cancel(8L, 1L)).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.cancel(7L, 2L)).isInstanceOf(DomainRuleViolation.class);
        assertThatThrownBy(() -> service.cancel(7L, 3L)).isInstanceOf(DomainRuleViolation.class)
                .extracting("code").isEqualTo("INVALID_TRANSITION");
        assertThat(((RecordingAppointments) appointments).statuses).isEmpty();
        assertThat(slots.releasedAppointments).isEmpty();
    }

    static final class RecordingAppointments extends MyAppointmentsServiceTest.InMemoryAppointments {
        final List<AppointmentStatus> statuses = new ArrayList<>();
        final List<String> sources = new ArrayList<>();
        @Override public void updateStatus(long id, AppointmentStatus status, Long by, LocalDateTime approvedAt, Instant at) {
            statuses.add(status);
        }
        @Override public void addHistory(long id, AppointmentStatus status, Long actor, String source, String reason, Instant at) {
            sources.add(source);
        }
    }

    static class RecordingSlots implements SlotRepositoryPort {
        final List<Long> releasedAppointments = new ArrayList<>();
        final List<Long> releasedHolds = new ArrayList<>();
        @Override public List<LockedSlot> lockFreeSlots(long p, long l, LocalDateTime from, LocalDateTime to) { return List.of(); }
        @Override public void assignToAppointment(List<Long> slotIds, long appointmentId) { }
        @Override public void holdForReschedule(List<Long> slotIds, long requestId) { }
        @Override public void releaseAppointment(long appointmentId) { releasedAppointments.add(appointmentId); }
        @Override public void releaseHold(long requestId) { releasedHolds.add(requestId); }
        @Override public void transferHoldToAppointment(long requestId, long appointmentId) { }
    }

    static final class RecordingReschedules implements RescheduleRepositoryPort {
        RescheduleRequest pending;
        final List<RescheduleStatus> statuses = new ArrayList<>();
        final List<String> sources = new ArrayList<>();
        @Override public long insert(NewReschedule r, Instant at) { return 1L; }
        @Override public Optional<RescheduleRequest> findPendingForAppointment(long appointmentId) {
            return Optional.ofNullable(pending).filter(p -> p.appointmentId() == appointmentId);
        }
        @Override public Optional<RescheduleRequest> lockById(long id) { return Optional.ofNullable(pending); }
        @Override public void updateStatus(long id, RescheduleStatus status, Long by, String reason, LocalDateTime at) {
            statuses.add(status);
        }
        @Override public void addHistory(long id, RescheduleStatus status, Long actor, String source, String reason, Instant at) {
            sources.add(source);
        }
        @Override public List<RescheduleInboxItem> findPending(InboxFilter filter) { return List.of(); }
    }
}
