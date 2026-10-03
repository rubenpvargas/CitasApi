package com.fcv.citas.domain.model;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Fecha y rango horario (hora local de pared) de un bloque de disponibilidad. El bloque no cruza la
 * medianoche (fin estrictamente posterior al inicio en el mismo día) y se discretiza en slots de 30 min.
 */
public record BlockSchedule(LocalDate date, LocalTime startTime, LocalTime endTime) {
    public static final int SLOT_MINUTES = 30;

    public LocalDateTime startAt() {
        return LocalDateTime.of(date, startTime);
    }

    public LocalDateTime endAt() {
        return LocalDateTime.of(date, endTime);
    }

    /** Intervalos semiabiertos [inicio, fin): dos bloques contiguos (08–09 y 09–10) no se solapan. */
    public boolean overlaps(BlockSchedule other) {
        return date.equals(other.date) && startTime.isBefore(other.endTime) && other.startTime.isBefore(endTime);
    }

    /** Slots consecutivos de 30 min desde el inicio; el último termina exactamente en el fin del bloque. */
    public List<SlotTime> slots() {
        List<SlotTime> slots = new ArrayList<>();
        LocalDateTime cursor = startAt();
        LocalDateTime end = endAt();
        while (!cursor.plusMinutes(SLOT_MINUTES).isAfter(end)) {
            slots.add(new SlotTime(cursor, cursor.plusMinutes(SLOT_MINUTES)));
            cursor = cursor.plusMinutes(SLOT_MINUTES);
        }
        return List.copyOf(slots);
    }

    public int slotCount() {
        return (int) (Duration.between(startTime, endTime).toMinutes() / SLOT_MINUTES);
    }
}
