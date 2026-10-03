package com.fcv.citas.domain.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * HU-015 CA-02 — cálculo puro de inicios reservables: para una duración d (30 o 60) se requieren
 * n = d/30 slots libres consecutivos <b>del mismo bloque</b> (cada inicio = inicio anterior + 30 min)
 * y el inicio debe ser estrictamente posterior a ahora. Los slots reservados o retenidos no llegan
 * como libres, por lo que rompen la consecutividad.
 */
public final class AvailabilityCalculator {
    private AvailabilityCalculator() {
    }

    public static List<AvailableStart> calculate(List<FreeSlot> freeSlots, int durationMinutes, LocalDateTime now) {
        if (durationMinutes <= 0 || durationMinutes % BlockSchedule.SLOT_MINUTES != 0) {
            return List.of();
        }
        int required = durationMinutes / BlockSchedule.SLOT_MINUTES;
        Map<Long, List<LocalDateTime>> byBlock = new TreeMap<>();
        for (FreeSlot slot : freeSlots) {
            byBlock.computeIfAbsent(slot.blockId(), id -> new ArrayList<>()).add(slot.startAt());
        }
        List<AvailableStart> result = new ArrayList<>();
        byBlock.forEach((blockId, starts) -> {
            starts.sort(Comparator.naturalOrder());
            for (int i = 0; i + required <= starts.size(); i++) {
                LocalDateTime start = starts.get(i);
                if (start.isAfter(now) && consecutive(starts, i, required)) {
                    result.add(new AvailableStart(blockId, start, start.plusMinutes(durationMinutes)));
                }
            }
        });
        result.sort(Comparator.comparing(AvailableStart::startAt).thenComparing(AvailableStart::blockId));
        return List.copyOf(result);
    }

    private static boolean consecutive(List<LocalDateTime> starts, int from, int required) {
        for (int k = 1; k < required; k++) {
            if (!starts.get(from + k).equals(starts.get(from).plusMinutes((long) k * BlockSchedule.SLOT_MINUTES))) {
                return false;
            }
        }
        return true;
    }
}
