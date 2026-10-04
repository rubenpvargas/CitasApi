package com.fcv.citas.application.port.in;

import com.fcv.citas.domain.model.Appointment;

import java.time.LocalDate;
import java.util.List;

/** HU-023/HU-024 — agenda de citas aprobadas propias y cierre de atención. */
public interface ProfessionalAgendaUseCase {
    List<Appointment> agenda(long userId, LocalDate from, LocalDate to, String locationCode);

    /** HU-024 — cierra una cita propia aplicable (APPROVED con inicio ≤ ahora). */
    Appointment close(long userId, long appointmentId, com.fcv.citas.domain.model.AppointmentStatus outcome);
}
