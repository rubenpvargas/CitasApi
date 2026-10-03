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
}
