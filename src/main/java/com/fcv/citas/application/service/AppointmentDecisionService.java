package com.fcv.citas.application.service;

import com.fcv.citas.application.exception.NotFoundException;
import com.fcv.citas.application.port.in.AppointmentDecisionUseCase;
import com.fcv.citas.application.port.out.AppointmentRepositoryPort;
import com.fcv.citas.application.port.out.SlotRepositoryPort;
import com.fcv.citas.application.port.out.TransactionPort;
import com.fcv.citas.domain.model.Appointment;
import com.fcv.citas.domain.model.AppointmentStatus;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * HU-018 — bloquea la cita, aplica la regla de dominio {@link Appointment#decide} y registra el historial
 * con fuente ADMIN; el rechazo libera los slots, la aprobación los conserva.
 */
public final class AppointmentDecisionService implements AppointmentDecisionUseCase {
    private final AppointmentRepositoryPort appointments;
    private final SlotRepositoryPort slots;
    private final TransactionPort transactions;
    private final Clock clock;

    public AppointmentDecisionService(AppointmentRepositoryPort appointments, SlotRepositoryPort slots,
                                      TransactionPort transactions, Clock clock) {
        this.appointments = appointments;
        this.slots = slots;
        this.transactions = transactions;
        this.clock = clock;
    }

    @Override
    public Appointment decide(long adminUserId, long appointmentId, boolean approve, String reason) {
        return transactions.required(() -> {
            Appointment current = appointments.lockById(appointmentId)
                    .orElseThrow(() -> new NotFoundException("Appointment not found"));
            AppointmentStatus target = current.decide(approve, reason);
            String cleanReason = reason == null || reason.isBlank() ? null : reason.trim();
            if (target == AppointmentStatus.APPROVED) {
                appointments.updateStatus(appointmentId, target, adminUserId, LocalDateTime.now(clock), clock.instant());
            } else {
                appointments.updateStatus(appointmentId, target, null, null, clock.instant());
                slots.releaseAppointment(appointmentId);
            }
            appointments.addHistory(appointmentId, target, adminUserId, "ADMIN", cleanReason, clock.instant());
            return appointments.findById(appointmentId).orElseThrow();
        });
    }
}
