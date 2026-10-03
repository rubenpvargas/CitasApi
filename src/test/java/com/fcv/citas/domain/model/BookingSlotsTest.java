package com.fcv.citas.domain.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HU-016/HU-017 — selección de los N slots consecutivos bloqueados (FOR UPDATE) para una reserva.
 * Prueba de escritorio con datos sintéticos: día 2026-10-06, ahora = 2026-10-05 10:00.
 */
class BookingSlotsTest {
    private static final LocalDate DAY = LocalDate.of(2026, 10, 6);
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 5, 10, 0);

    private static LockedSlot slot(long id, long block, String time) {
        return new LockedSlot(id, block, LocalDateTime.of(DAY, LocalTime.parse(time)));
    }

    private static LocalDateTime at(String time) {
        return LocalDateTime.of(DAY, LocalTime.parse(time));
    }

    @Test
    void bk1GeneralThirtyMinutesTakesExactlyTheRequestedSlot() {
        var result = BookingSlots.select(List.of(slot(11, 1, "08:00"), slot(12, 1, "08:30")), at("08:00"), 30, NOW);

        assertThat(result).contains(List.of(11L));
    }

    @Test
    void bk2SixtyMinutesTakesTwoConsecutiveSlotsOfTheSameBlock() {
        var result = BookingSlots.select(List.of(slot(11, 1, "08:00"), slot(12, 1, "08:30")), at("08:00"), 60, NOW);

        assertThat(result).contains(List.of(11L, 12L));
    }

    @Test
    void bk3SixtyMinutesWithSecondSlotAlreadyTakenIsUnavailable() {
        // 08:30 ya pertenece a otra cita: no llega en la lectura bloqueante de slots libres.
        assertThat(BookingSlots.select(List.of(slot(11, 1, "08:00")), at("08:00"), 60, NOW)).isEmpty();
    }

    @Test
    void bk4SixtyMinutesAcrossTwoBlocksIsUnavailable() {
        assertThat(BookingSlots.select(List.of(slot(11, 1, "08:00"), slot(21, 2, "08:30")), at("08:00"), 60, NOW)).isEmpty();
    }

    @Test
    void bk5StartAtOrBeforeNowIsUnavailable() {
        LocalDateTime now = at("08:00");
        assertThat(BookingSlots.select(List.of(slot(11, 1, "08:00"), slot(12, 1, "08:30")), at("08:00"), 30, now)).isEmpty();
    }

    @Test
    void bk6StartNotOnASlotBoundaryOrRaceLoserGetsNothing() {
        assertThat(BookingSlots.select(List.of(slot(11, 1, "08:00"), slot(12, 1, "08:30")), at("08:15"), 30, NOW)).isEmpty();
        // Perdedor de la carrera: tras esperar el bloqueo, la relectura ya no devuelve el slot libre.
        assertThat(BookingSlots.select(List.of(), at("08:00"), 30, NOW)).isEmpty();
    }
}
