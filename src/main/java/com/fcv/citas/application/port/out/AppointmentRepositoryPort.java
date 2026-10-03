package com.fcv.citas.application.port.out;

import com.fcv.citas.application.model.InboxFilter;
import com.fcv.citas.application.model.NewAppointment;
import com.fcv.citas.application.model.ReminderItem;
import com.fcv.citas.domain.model.Appointment;
import com.fcv.citas.domain.model.AppointmentStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AppointmentRepositoryPort {
    long insert(NewAppointment appointment, Instant at);

    Optional<Appointment> findById(long id);

    /** Bloquea la fila de la cita hasta el fin de la transacción y devuelve su vista vigente. */
    Optional<Appointment> lockById(long id);

    List<Appointment> findByPatient(long patientUserId, AppointmentStatus status, LocalDate from, LocalDate to);

    void updateStatus(long id, AppointmentStatus status, Long approvedByUserId, LocalDateTime approvedAt, Instant at);

    void moveTo(long id, long locationId, LocalDateTime startAt, LocalDateTime endAt, Instant at);

    /** Historial append-only de transiciones de la cita. */
    void addHistory(long id, AppointmentStatus status, Long actorUserId, String source, String reason, Instant at);

    /** Citas APPROVED del profesional cuyo inicio cae en [from, to]. */
    List<Appointment> findAgenda(long professionalId, LocalDate from, LocalDate to, String locationCode);

    List<Appointment> findRequested(InboxFilter filter);

    List<ReminderItem> findApprovedStartingBetween(LocalDateTime from, LocalDateTime to);
}
