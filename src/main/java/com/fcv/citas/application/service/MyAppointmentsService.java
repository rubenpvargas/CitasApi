package com.fcv.citas.application.service;

import com.fcv.citas.application.exception.NotFoundException;
import com.fcv.citas.application.exception.RequestValidationException;
import com.fcv.citas.application.port.in.MyAppointmentsUseCase;
import com.fcv.citas.application.port.out.AppointmentRepositoryPort;
import com.fcv.citas.application.port.out.RescheduleRepositoryPort;
import com.fcv.citas.application.port.out.SlotRepositoryPort;
import com.fcv.citas.application.port.out.TransactionPort;
import com.fcv.citas.domain.model.Appointment;
import com.fcv.citas.domain.model.AppointmentStatus;
import com.fcv.citas.domain.model.RescheduleStatus;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

/**
 * HU-019 — consulta de citas propias con filtros; HU-020 — cancelación de una cita propia futura en
 * REQUESTED|APPROVED: libera sus slots y cierra la reprogramación pendiente (fuente SYSTEM) liberando su
 * retención, todo bajo el bloqueo de fila de la cita.
 */
public final class MyAppointmentsService implements MyAppointmentsUseCase {
    private final AppointmentRepositoryPort appointments;
    private final SlotRepositoryPort slots;
    private final RescheduleRepositoryPort reschedules;
    private final TransactionPort transactions;
    private final Clock clock;

    public MyAppointmentsService(AppointmentRepositoryPort appointments, SlotRepositoryPort slots,
                                 RescheduleRepositoryPort reschedules, TransactionPort transactions, Clock clock) {
        this.appointments = appointments;
        this.slots = slots;
        this.reschedules = reschedules;
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
                .orElseThrow(MyAppointmentsService::notFound);
    }

    @Override
    public Appointment cancel(long userId, long appointmentId) {
        return transactions.required(() -> {
            Appointment current = appointments.lockById(appointmentId)
                    .filter(a -> a.patientUserId() == userId)
                    .orElseThrow(MyAppointmentsService::notFound);
            current.requireCancellable(LocalDateTime.now(clock));
            appointments.updateStatus(appointmentId, AppointmentStatus.CANCELLED, null, null, clock.instant());
            appointments.addHistory(appointmentId, AppointmentStatus.CANCELLED, userId, "USER",
                    "Cancelled by patient", clock.instant());
            slots.releaseAppointment(appointmentId);
            reschedules.findPendingForAppointment(appointmentId).ifPresent(pending -> {
                String reason = "Closed because the appointment was cancelled";
                reschedules.updateStatus(pending.id(), RescheduleStatus.CANCELLED, null, reason, LocalDateTime.now(clock));
                reschedules.addHistory(pending.id(), RescheduleStatus.CANCELLED, null, "SYSTEM", reason, clock.instant());
                slots.releaseHold(pending.id());
            });
            return appointments.findById(appointmentId).orElseThrow();
        });
    }

    private static NotFoundException notFound() {
        return new NotFoundException("Appointment not found");
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
