package com.fcv.citas.application.model;

import java.time.Instant;
import java.time.LocalDateTime;

/** HU-025/HU-022 — reprogramación PENDING con la franja vigente y la retenida para comparar. */
public record RescheduleInboxItem(long id, long appointmentId, String patientName, long professionalId,
                                  String professionalName, long specialtyId, String specialtyName, String locationCode,
                                  LocalDateTime currentStartAt, LocalDateTime currentEndAt,
                                  LocalDateTime requestedStartAt, LocalDateTime requestedEndAt, Instant createdAt) {
}
