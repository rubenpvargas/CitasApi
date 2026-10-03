package com.fcv.citas.domain.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

/** HU-013/HU-014 — un bloque es editable solo si es futuro y no tiene slots comprometidos. */
class AvailabilityBlockTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 5, 10, 0);

    private static AvailabilityBlock block(LocalDate date, String start, int committed) {
        return new AvailabilityBlock(1L, 2L, "HIC", "Sede HIC",
                new BlockSchedule(date, LocalTime.parse(start), LocalTime.parse(start).plusHours(1)), 2, committed);
    }

    @Test
    void futureFreeBlockIsEditable() {
        AvailabilityBlock b = block(NOW.toLocalDate().plusDays(1), "08:00", 0);

        assertThat(b.notEditableReason(NOW)).isEmpty();
        assertThat(b.editableAt(NOW)).isTrue();
    }

    @Test
    void startedOrPastBlockIsNotEditable() {
        assertThat(block(NOW.toLocalDate(), "10:00", 0).notEditableReason(NOW)).contains(BlockLockReason.PAST_BLOCK);
        assertThat(block(NOW.toLocalDate().minusDays(1), "08:00", 0).editableAt(NOW)).isFalse();
    }

    @Test
    void committedFutureBlockIsNotEditableAndPastTakesPrecedence() {
        assertThat(block(NOW.toLocalDate().plusDays(1), "08:00", 1).notEditableReason(NOW))
                .contains(BlockLockReason.BLOCK_COMMITTED);
        assertThat(block(NOW.toLocalDate().minusDays(1), "08:00", 1).notEditableReason(NOW))
                .contains(BlockLockReason.PAST_BLOCK);
    }
}
