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

    /**
     * HU-018 — solo una cita REQUESTED se decide; aprobar → APPROVED (conserva slots), rechazar exige
     * motivo → REJECTED (el caso de uso libera los slots).
     */
    public AppointmentStatus decide(boolean approve, String reason) {
        if (status != AppointmentStatus.REQUESTED) {
            throw invalidTransition("Only requested appointments can be decided");
        }
        if (!approve && (reason == null || reason.isBlank())) {
            throw new DomainRuleViolation("REJECTION_REASON_REQUIRED", "A rejection requires a reason");
        }
        return approve ? AppointmentStatus.APPROVED : AppointmentStatus.REJECTED;
    }

    static DomainRuleViolation invalidTransition(String message) {
        return new DomainRuleViolation("INVALID_TRANSITION", message);
    }

    /** HU-020 — propia (resuelto por el caso de uso), futura y en REQUESTED|APPROVED. */
    public boolean cancellableAt(LocalDateTime now) {
        return (status == AppointmentStatus.REQUESTED || status == AppointmentStatus.APPROVED) && startAt.isAfter(now);
    }

    /** HU-020 — fuera de REQUESTED|APPROVED o si ya comenzó → INVALID_TRANSITION (no hay reactivación). */
    public void requireCancellable(LocalDateTime now) {
        if (!cancellableAt(now)) {
            throw invalidTransition("Only future requested or approved appointments can be cancelled");
        }
    }

    /** HU-021 — no APPROVED o no futura → INVALID_TRANSITION; con otra PENDING → RESCHEDULE_ALREADY_PENDING. */
    public void requireReschedulable(LocalDateTime now) {
        if (status != AppointmentStatus.APPROVED || !startAt.isAfter(now)) {
            throw invalidTransition("Only approved future appointments can be rescheduled");
        }
        if (pendingReschedule != null) {
            throw new DomainRuleViolation("RESCHEDULE_ALREADY_PENDING", "A reschedule request is already pending");
        }
    }

    /** HU-021 — solo APPROVED futura y sin otra reprogramación PENDING. */
    public boolean reschedulableAt(LocalDateTime now) {
        return status == AppointmentStatus.APPROVED && startAt.isAfter(now) && pendingReschedule == null;
    }
}
