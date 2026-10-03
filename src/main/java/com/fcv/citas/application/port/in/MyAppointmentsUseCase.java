package com.fcv.citas.application.port.in;

import com.fcv.citas.domain.model.Appointment;

import java.time.LocalDate;
import java.util.List;

/** HU-019/HU-020 — citas propias del USER autenticado. */
public interface MyAppointmentsUseCase {
    List<Appointment> list(long userId, String status, LocalDate from, LocalDate to);

    /** Cita propia; ajena o inexistente → NOT_FOUND. */
    Appointment get(long userId, long appointmentId);
}
