package com.fcv.citas.application.port.in;

import com.fcv.citas.domain.model.Appointment;

/** HU-018 — decisión ADMIN sobre solicitudes especializadas. */
public interface AppointmentDecisionUseCase {
    Appointment decide(long adminUserId, long appointmentId, boolean approve, String reason);
}
