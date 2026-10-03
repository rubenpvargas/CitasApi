package com.fcv.citas.domain.model;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Bloque activo de un profesional con su sede y ocupación: committedSlots cuenta slots reservados por
 * una cita o retenidos por una reprogramación pendiente.
 */
public record AvailabilityBlock(long id, long professionalId, String locationCode, String locationName,
                                BlockSchedule schedule, int totalSlots, int committedSlots) {

    /** Pasado tiene precedencia sobre comprometido: un bloque iniciado es inmutable en cualquier caso. */
    public Optional<BlockLockReason> notEditableReason(LocalDateTime now) {
        if (!schedule.startAt().isAfter(now)) {
            return Optional.of(BlockLockReason.PAST_BLOCK);
        }
        if (committedSlots > 0) {
            return Optional.of(BlockLockReason.BLOCK_COMMITTED);
        }
        return Optional.empty();
    }

    public boolean editableAt(LocalDateTime now) {
        return notEditableReason(now).isEmpty();
    }
}
