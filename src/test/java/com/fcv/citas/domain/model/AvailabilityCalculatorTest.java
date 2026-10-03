package com.fcv.citas.domain.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HU-015 CA-02 — inicios ofrecibles con duración/30 slots libres consecutivos dentro del mismo bloque.
 * Casos sintéticos de la prueba de escritorio (día 2026-10-06; ahora = 2026-10-05 10:00 salvo dc5).
 */
class AvailabilityCalculatorTest {
    private static final LocalDate DAY = LocalDate.of(2026, 10, 6);
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 5, 10, 0);

    private static FreeSlot free(long block, String start) {
        return new FreeSlot(block, LocalDateTime.of(DAY, LocalTime.parse(start)));
    }

    private static List<String> starts(List<AvailableStart> result) {
        return result.stream().map(s -> s.startAt().toLocalTime() + "-" + s.endAt().toLocalTime() + "@" + s.blockId()).toList();
    }

    @Test
    void dc1OneHourBlockOffersExactlyOneSixtyMinuteStart() {
        var result = AvailabilityCalculator.calculate(List.of(free(1, "08:00"), free(1, "08:30")), 60, NOW);

        assertThat(starts(result)).containsExactly("08:00-09:00@1");
    }

    @Test
    void dc2BookedMiddleSlotBreaksConsecutivenessForSixtyButNotForThirty() {
        // Bloque 08:00–10:00 con el slot 08:30 reservado: libres 08:00, 09:00, 09:30.
        List<FreeSlot> slots = List.of(free(1, "08:00"), free(1, "09:00"), free(1, "09:30"));

        assertThat(starts(AvailabilityCalculator.calculate(slots, 60, NOW))).containsExactly("09:00-10:00@1");
        assertThat(starts(AvailabilityCalculator.calculate(slots, 30, NOW)))
                .containsExactly("08:00-08:30@1", "09:00-09:30@1", "09:30-10:00@1");
    }

    @Test
    void dc3LastSlotOfTheBlockCannotStartASixtyMinuteAppointment() {
        var result = AvailabilityCalculator.calculate(List.of(free(1, "08:00"), free(1, "08:30"), free(1, "09:00")), 60, NOW);

        assertThat(starts(result)).containsExactly("08:00-09:00@1", "08:30-09:30@1");
    }

    @Test
    void dc4ConsecutiveSlotsOfDifferentBlocksDoNotCombine() {
        var result = AvailabilityCalculator.calculate(List.of(free(1, "08:00"), free(2, "08:30")), 60, NOW);

        assertThat(result).isEmpty();
        assertThat(starts(AvailabilityCalculator.calculate(List.of(free(2, "08:30"), free(1, "08:00")), 30, NOW)))
                .containsExactly("08:00-08:30@1", "08:30-09:00@2");
    }

    @Test
    void dc5StartsAtOrBeforeNowAreExcluded() {
        LocalDateTime now = LocalDateTime.of(DAY, LocalTime.of(8, 30));
        List<FreeSlot> slots = List.of(free(1, "08:00"), free(1, "08:30"), free(1, "09:00"), free(1, "09:30"));

        assertThat(starts(AvailabilityCalculator.calculate(slots, 60, now))).containsExactly("09:00-10:00@1");
    }

    @Test
    void dc6NoFreeSlotsOrInvalidDurationOfferNothing() {
        assertThat(AvailabilityCalculator.calculate(List.of(), 30, NOW)).isEmpty();
        assertThat(AvailabilityCalculator.calculate(List.of(free(1, "08:00")), 0, NOW)).isEmpty();
        assertThat(AvailabilityCalculator.calculate(List.of(free(1, "08:00"), free(1, "08:30")), 45, NOW)).isEmpty();
    }
}
