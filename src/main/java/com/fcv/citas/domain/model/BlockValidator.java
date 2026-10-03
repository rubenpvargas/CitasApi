package com.fcv.citas.domain.model;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Reglas puras de publicación de un bloque (HU-012, reutilizadas en la edición de HU-013). Se evalúan
 * en orden: forma (alineación :00/:30 y fin &gt; inicio, lo que implica duración múltiplo de 30),
 * luego tiempo (inicio estrictamente posterior a ahora; el mismo día es válido) y por último solape
 * con cualquier bloque activo del profesional en cualquier sede.
 */
public final class BlockValidator {
    private BlockValidator() {
    }

    public static Optional<BlockViolation> validateShape(BlockSchedule candidate) {
        if (!aligned(candidate.startTime())) {
            return Optional.of(BlockViolation.START_NOT_ALIGNED);
        }
        if (!aligned(candidate.endTime())) {
            return Optional.of(BlockViolation.END_NOT_ALIGNED);
        }
        if (!candidate.endTime().isAfter(candidate.startTime())) {
            return Optional.of(BlockViolation.END_NOT_AFTER_START);
        }
        return Optional.empty();
    }

    public static Optional<BlockViolation> validate(BlockSchedule candidate, LocalDateTime now,
                                                    List<ExistingBlock> existing, Long excludedBlockId) {
        Optional<BlockViolation> shape = validateShape(candidate);
        if (shape.isPresent()) {
            return shape;
        }
        if (!candidate.startAt().isAfter(now)) {
            return Optional.of(BlockViolation.PAST_BLOCK);
        }
        boolean overlap = existing.stream()
                .filter(block -> !Objects.equals(block.id(), excludedBlockId))
                .anyMatch(block -> block.schedule().overlaps(candidate));
        return overlap ? Optional.of(BlockViolation.BLOCK_OVERLAP) : Optional.empty();
    }

    private static boolean aligned(LocalTime time) {
        return time.getSecond() == 0 && time.getNano() == 0
                && time.getMinute() % BlockSchedule.SLOT_MINUTES == 0;
    }
}
