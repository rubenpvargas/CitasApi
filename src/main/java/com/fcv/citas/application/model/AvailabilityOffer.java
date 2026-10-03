package com.fcv.citas.application.model;

import java.time.LocalDateTime;

/** Franja reservable ofrecida al USER. */
public record AvailabilityOffer(long professionalId, String professionalName, long specialtyId, String specialtyName,
                                boolean general, String locationCode, String locationName, LocalDateTime startAt,
                                LocalDateTime endAt, int durationMinutes) {
}
