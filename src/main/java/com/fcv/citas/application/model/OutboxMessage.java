package com.fcv.citas.application.model;

/** Mensaje pendiente del outbox listo para despachar. */
public record OutboxMessage(long id, String eventId, String payloadJson, int attempts) {
}
