package com.fcv.citas.application.service;

import com.fcv.citas.application.exception.NotFoundException;
import com.fcv.citas.application.exception.RequestValidationException;
import com.fcv.citas.application.model.InboxFilter;
import com.fcv.citas.application.model.NewAppointment;
import com.fcv.citas.application.model.ReminderItem;
import com.fcv.citas.application.port.out.AppointmentRepositoryPort;
import com.fcv.citas.domain.model.Appointment;
import com.fcv.citas.domain.model.AppointmentStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.fcv.citas.application.service.AvailabilityBlockServiceTest.CLOCK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** HU-019 — consulta de citas propias: ownership, filtros válidos. */
class MyAppointmentsServiceTest {
    final InMemoryAppointments repo = new InMemoryAppointments();
    final MyAppointmentsService service = new MyAppointmentsService(repo, null, null, new DirectTransactions(), CLOCK);

    static Appointment appointment(long id, long patient, AppointmentStatus status, LocalDateTime start) {
        return new Appointment(id, patient, "Paciente", 3L, "Valeria Agenda", 4L, "Cardiologia", false, 5L, "HIC", "Sede HIC",
                status, start, start.plusMinutes(30), null, null, null, Instant.EPOCH);
    }

    @Test
    void ca03ForeignAppointmentDetailIsNotFound() {
        repo.items.add(appointment(1L, 7L, AppointmentStatus.APPROVED, LocalDateTime.of(2026, 10, 8, 8, 0)));

        assertThat(service.get(7L, 1L).id()).isEqualTo(1L);
        assertThatThrownBy(() -> service.get(8L, 1L)).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.get(7L, 99L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void ca02FiltersArePassedAndValidated() {
        service.list(7L, "approved", LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));

        assertThat(repo.lastStatus).isEqualTo(AppointmentStatus.APPROVED);
        assertThatThrownBy(() -> service.list(7L, "BOGUS", null, null))
                .isInstanceOf(RequestValidationException.class).extracting("field").isEqualTo("status");
        assertThatThrownBy(() -> service.list(7L, null, LocalDate.of(2026, 10, 2), LocalDate.of(2026, 10, 1)))
                .isInstanceOf(RequestValidationException.class).extracting("field").isEqualTo("to");
    }

    static class InMemoryAppointments implements AppointmentRepositoryPort {
        final List<Appointment> items = new ArrayList<>();
        AppointmentStatus lastStatus;

        @Override public long insert(NewAppointment appointment, Instant at) { throw new UnsupportedOperationException(); }
        @Override public Optional<Appointment> findById(long id) { return items.stream().filter(a -> a.id() == id).findFirst(); }
        @Override public Optional<Appointment> lockById(long id) { return findById(id); }
        @Override public List<Appointment> findByPatient(long patientUserId, AppointmentStatus status, LocalDate from, LocalDate to) {
            lastStatus = status;
            return items.stream().filter(a -> a.patientUserId() == patientUserId).toList();
        }
        @Override public void updateStatus(long id, AppointmentStatus status, Long by, LocalDateTime approvedAt, Instant at) { }
        @Override public void moveTo(long id, long locationId, LocalDateTime startAt, LocalDateTime endAt, Instant at) { }
        @Override public void addHistory(long id, AppointmentStatus status, Long actor, String source, String reason, Instant at) { }
        @Override public List<Appointment> findAgenda(long professionalId, LocalDate from, LocalDate to, String locationCode) { return List.of(); }
        @Override public List<Appointment> findRequested(InboxFilter filter) { return List.of(); }
        @Override public List<ReminderItem> findApprovedStartingBetween(LocalDateTime from, LocalDateTime to) { return List.of(); }
    }
}
