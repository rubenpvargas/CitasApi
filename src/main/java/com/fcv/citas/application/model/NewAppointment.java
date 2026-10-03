package com.fcv.citas.application.model;

import com.fcv.citas.domain.model.AppointmentStatus;

import java.time.LocalDateTime;

/** Datos de alta de una cita ya validada dentro de la transacción de reserva. */
public record NewAppointment(long patientUserId, long professionalId, long locationId, long specialtyId,
                             AppointmentStatus status, String reason, LocalDateTime startAt, LocalDateTime endAt,
                             LocalDateTime approvedAt) {
}
