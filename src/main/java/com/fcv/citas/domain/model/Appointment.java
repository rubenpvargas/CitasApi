package com.fcv.citas.domain.model;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;

/**
 * Cita con sus referencias resueltas (vista de lectura) y las reglas de su ciclo de vida.
 * Fechas en hora local de pared; {@code createdAt} es un instante.
 */
public record Appointment(long id, long patientUserId, String patientName, long professionalId, String professionalName,
                          long specialtyId, String specialtyName, boolean generalSpecialty, long locationId,
                          String locationCode, String locationName, AppointmentStatus status, LocalDateTime startAt,
                          LocalDateTime endAt, String reason, String rejectionReason,
                          PendingReschedule pendingReschedule, Instant createdAt) {

    public int durationMinutes() {
        return (int) Duration.between(startAt, endAt).toMinutes();
    }

    /** HU-020 — propia (resuelto por el caso de uso), futura y en REQUESTED|APPROVED. */
    public boolean cancellableAt(LocalDateTime now) {
        return (status == AppointmentStatus.REQUESTED || status == AppointmentStatus.APPROVED) && startAt.isAfter(now);
    }

    /** HU-021 — solo APPROVED futura y sin otra reprogramación PENDING. */
    public boolean reschedulableAt(LocalDateTime now) {
        return status == AppointmentStatus.APPROVED && startAt.isAfter(now) && pendingReschedule == null;
    }
}
