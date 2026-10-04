package com.fcv.citas.domain.model;

import java.time.LocalDateTime;

/**
 * Solicitud de reprogramación: conserva la franja vigente de la cita (previous*) y la nueva franja
 * retenida (requested*) hasta la decisión ADMIN.
 */
public record RescheduleRequest(long id, long appointmentId, long requestedByUserId, long requestedLocationId,
                                String requestedLocationCode, RescheduleStatus status, LocalDateTime previousStartAt,
                                LocalDateTime previousEndAt, LocalDateTime requestedStartAt,
                                LocalDateTime requestedEndAt) {

    /** HU-022 — solo PENDING se decide; rechazar exige motivo. */
    public RescheduleStatus decide(boolean approve, String reason) {
        if (status != RescheduleStatus.PENDING) {
            throw new DomainRuleViolation("INVALID_TRANSITION", "Only pending reschedule requests can be decided");
        }
        if (!approve && (reason == null || reason.isBlank())) {
            throw new DomainRuleViolation("REJECTION_REASON_REQUIRED", "A rejection requires a reason");
        }
        return approve ? RescheduleStatus.APPROVED : RescheduleStatus.REJECTED;
    }
}
