package com.fcv.citas.domain.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HU-012 — reglas puras de un bloque de disponibilidad. Casos sintéticos de la prueba de escritorio
 * (ahora = 2026-10-05 10:00, hora local de Bogotá).
 */
class BlockValidatorTest {
    private static final LocalDate DAY = LocalDate.of(2026, 10, 5);
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 5, 10, 0);

    private static BlockSchedule block(LocalDate date, String start, String end) {
        return new BlockSchedule(date, LocalTime.parse(start), LocalTime.parse(end));
    }

    @Test
    void dc1ValidFutureBlockOnAnotherDayHasNoViolation() {
        assertThat(BlockValidator.validate(block(DAY.plusDays(1), "08:00", "09:00"), NOW, List.of(), null)).isEmpty();
    }

    @Test
    void dc2SameDayFutureBlockIsAllowed() {
        assertThat(BlockValidator.validate(block(DAY, "10:30", "12:00"), NOW, List.of(), null)).isEmpty();
    }

    @Test
    void dc3StartAtOrBeforeNowIsPastBlock() {
        assertThat(BlockValidator.validate(block(DAY, "10:00", "11:00"), NOW, List.of(), null))
                .contains(BlockViolation.PAST_BLOCK);
        assertThat(BlockValidator.validate(block(DAY.minusDays(1), "08:00", "09:00"), NOW, List.of(), null))
                .contains(BlockViolation.PAST_BLOCK);
    }

    @Test
    void dc4MisalignedTimesAreRejectedBeforeTemporalRules() {
        assertThat(BlockValidator.validate(block(DAY.plusDays(1), "08:15", "09:00"), NOW, List.of(), null))
                .contains(BlockViolation.START_NOT_ALIGNED);
        assertThat(BlockValidator.validate(block(DAY.plusDays(1), "08:00", "09:10"), NOW, List.of(), null))
                .contains(BlockViolation.END_NOT_ALIGNED);
        assertThat(BlockValidator.validate(new BlockSchedule(DAY.plusDays(1), LocalTime.of(8, 0, 30), LocalTime.of(9, 0)),
                NOW, List.of(), null)).contains(BlockViolation.START_NOT_ALIGNED);
    }

    @Test
    void dc5EndMustBeAfterStart() {
        assertThat(BlockValidator.validate(block(DAY.plusDays(1), "09:00", "09:00"), NOW, List.of(), null))
                .contains(BlockViolation.END_NOT_AFTER_START);
        assertThat(BlockValidator.validate(block(DAY.plusDays(1), "10:00", "09:00"), NOW, List.of(), null))
                .contains(BlockViolation.END_NOT_AFTER_START);
    }

    @Test
    void dc6OverlapWithAnActiveBlockInAnyLocationIsRejectedButTouchingIsNot() {
        // El bloque existente 7 está en otra sede (ICV): el solape se evalúa por profesional, no por sede.
        List<ExistingBlock> existing = List.of(new ExistingBlock(7L, block(DAY.plusDays(1), "08:00", "10:00")));

        assertThat(BlockValidator.validate(block(DAY.plusDays(1), "09:30", "11:00"), NOW, existing, null))
                .contains(BlockViolation.BLOCK_OVERLAP);
        assertThat(BlockValidator.validate(block(DAY.plusDays(1), "07:00", "08:30"), NOW, existing, null))
                .contains(BlockViolation.BLOCK_OVERLAP);
        assertThat(BlockValidator.validate(block(DAY.plusDays(1), "10:00", "11:00"), NOW, existing, null)).isEmpty();
        assertThat(BlockValidator.validate(block(DAY.plusDays(1), "07:00", "08:00"), NOW, existing, null)).isEmpty();
    }

    @Test
    void editingABlockIgnoresItselfWhenCheckingOverlap() {
        List<ExistingBlock> existing = List.of(new ExistingBlock(7L, block(DAY.plusDays(1), "08:00", "10:00")));

        Optional<BlockViolation> result = BlockValidator.validate(block(DAY.plusDays(1), "08:30", "10:30"), NOW, existing, 7L);

        assertThat(result).isEmpty();
    }
}
