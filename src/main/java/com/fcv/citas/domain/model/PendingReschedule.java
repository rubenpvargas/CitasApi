package com.fcv.citas.domain.model;

import java.time.LocalDateTime;

/** Reprogramación PENDING de una cita: franja retenida a la espera de decisión ADMIN. */
public record PendingReschedule(long id, LocalDateTime requestedStartAt, LocalDateTime requestedEndAt,
                                String locationCode) {
}
