package com.fcv.citas.domain.model;

/**
 * Bloque activo de un profesional con su sede y ocupación: committedSlots cuenta slots reservados por
 * una cita o retenidos por una reprogramación pendiente.
 */
public record AvailabilityBlock(long id, long professionalId, String locationCode, String locationName,
                                BlockSchedule schedule, int totalSlots, int committedSlots) {
}
