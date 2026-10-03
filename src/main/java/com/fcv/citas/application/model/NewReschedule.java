package com.fcv.citas.application.model;

import java.time.LocalDateTime;

/** Alta de una reprogramación PENDING ya validada. */
public record NewReschedule(long appointmentId, long requestedByUserId, long requestedLocationId,
                            LocalDateTime previousStartAt, LocalDateTime previousEndAt,
                            LocalDateTime requestedStartAt, LocalDateTime requestedEndAt) {
}
