package com.fcv.citas.domain.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

/** HU-012 — discretización de un bloque en slots de 30 minutos (prueba de escritorio). */
class BlockScheduleTest {
    private static final LocalDate DAY = LocalDate.of(2026, 10, 6);

    @Test
    void ds1OneHourBlockProducesTwoConsecutiveSlots() {
        var slots = new BlockSchedule(DAY, LocalTime.of(8, 0), LocalTime.of(9, 0)).slots();

        assertThat(slots).containsExactly(
                new SlotTime(LocalDateTime.of(DAY, LocalTime.of(8, 0)), LocalDateTime.of(DAY, LocalTime.of(8, 30))),
                new SlotTime(LocalDateTime.of(DAY, LocalTime.of(8, 30)), LocalDateTime.of(DAY, LocalTime.of(9, 0))));
    }

    @Test
    void ds2MinimalBlockProducesOneSlot() {
        var schedule = new BlockSchedule(DAY, LocalTime.of(14, 30), LocalTime.of(15, 0));

        assertThat(schedule.slots()).hasSize(1);
        assertThat(schedule.slotCount()).isOne();
    }

    @Test
    void ds3MorningBlockProducesEightSlotsAndLastEndsAtBlockEnd() {
        var slots = new BlockSchedule(DAY, LocalTime.of(8, 0), LocalTime.of(12, 0)).slots();

        assertThat(slots).hasSize(8);
        assertThat(slots.getLast().startAt()).isEqualTo(LocalDateTime.of(DAY, LocalTime.of(11, 30)));
        assertThat(slots.getLast().endAt()).isEqualTo(LocalDateTime.of(DAY, LocalTime.of(12, 0)));
    }

    @Test
    void ds4BlockEndingAtLastHalfHourOfTheDayDoesNotCrossMidnight() {
        var slots = new BlockSchedule(DAY, LocalTime.of(23, 0), LocalTime.of(23, 30)).slots();

        assertThat(slots).containsExactly(new SlotTime(LocalDateTime.of(DAY, LocalTime.of(23, 0)),
                LocalDateTime.of(DAY, LocalTime.of(23, 30))));
    }

    @Test
    void overlapIsHalfOpenAndSameDayOnly() {
        var a = new BlockSchedule(DAY, LocalTime.of(8, 0), LocalTime.of(10, 0));

        assertThat(a.overlaps(new BlockSchedule(DAY, LocalTime.of(9, 0), LocalTime.of(11, 0)))).isTrue();
        assertThat(a.overlaps(new BlockSchedule(DAY, LocalTime.of(10, 0), LocalTime.of(11, 0)))).isFalse();
        assertThat(a.overlaps(new BlockSchedule(DAY.plusDays(1), LocalTime.of(8, 0), LocalTime.of(10, 0)))).isFalse();
    }
}
