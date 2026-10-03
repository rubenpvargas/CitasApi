package com.fcv.citas.application.service;

import com.fcv.citas.application.exception.NotFoundException;
import com.fcv.citas.application.exception.RequestValidationException;
import com.fcv.citas.application.port.in.MyAppointmentsUseCase;
import com.fcv.citas.application.port.out.AppointmentRepositoryPort;
import com.fcv.citas.application.port.out.TransactionPort;
import com.fcv.citas.domain.model.Appointment;
import com.fcv.citas.domain.model.AppointmentStatus;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

/** HU-019 — consulta de citas propias con filtros de estado y fecha; el titular es siempre el sub del JWT. */
public final class MyAppointmentsService implements MyAppointmentsUseCase {
    private final AppointmentRepositoryPort appointments;
    private final TransactionPort transactions;
    private final Clock clock;

    public MyAppointmentsService(AppointmentRepositoryPort appointments, TransactionPort transactions, Clock clock) {
        this.appointments = appointments;
        this.transactions = transactions;
        this.clock = clock;
    }

    @Override
    public List<Appointment> list(long userId, String status, LocalDate from, LocalDate to) {
        AppointmentStatus parsed = parseStatus(status);
        if (from != null && to != null && to.isBefore(from)) {
            throw new RequestValidationException("to", "must not be before from");
        }
        return appointments.findByPatient(userId, parsed, from, to);
    }

    @Override
    public Appointment get(long userId, long appointmentId) {
        return appointments.findById(appointmentId)
                .filter(a -> a.patientUserId() == userId)
                .orElseThrow(() -> new NotFoundException("Appointment not found"));
    }

    private static AppointmentStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return AppointmentStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new RequestValidationException("status", "is not a valid appointment status");
        }
    }
}
