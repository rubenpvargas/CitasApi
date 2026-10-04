package com.fcv.citas.application.model;

import com.fcv.citas.domain.model.NotificationType;

import java.time.LocalDateTime;

/**
 * Evento de estado para WF-002 (payload del contrato): sin JWT, password ni documento.
 * {@code reason} es opcional (rechazos y cancelaciones).
 */
public record StatusEvent(String eventId, String correlationId, NotificationType type, LocalDateTime occurredAt,
                          long appointmentId, String status, LocalDateTime startAt, String recipientFirstName,
                          String recipientEmail, String reason) {
}
