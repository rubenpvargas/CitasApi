package com.fcv.citas.domain.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * HU-016/HU-017 — elige los slots exactos de una reserva entre los slots libres ya bloqueados con
 * {@code SELECT … FOR UPDATE}. Reutiliza {@link AvailabilityCalculator}: la reserva solo procede si el
 * inicio pedido es uno de los inicios ofrecibles (futuro y con duración/30 slots libres consecutivos
 * del mismo bloque). Quien pierde una carrera relee tras el bloqueo y ya no ve los slots como libres.
 */
public final class BookingSlots {
    private BookingSlots() {
    }

    public static Optional<List<Long>> select(List<LockedSlot> lockedFree, LocalDateTime startAt, int durationMinutes,
                                              LocalDateTime now) {
        List<FreeSlot> free = lockedFree.stream().map(s -> new FreeSlot(s.blockId(), s.startAt())).toList();
        return AvailabilityCalculator.calculate(free, durationMinutes, now).stream()
                .filter(start -> start.startAt().equals(startAt))
                .findFirst()
                .map(start -> lockedFree.stream()
                        .filter(s -> s.blockId() == start.blockId())
                        .filter(s -> !s.startAt().isBefore(start.startAt()) && s.startAt().isBefore(start.endAt()))
                        .sorted(java.util.Comparator.comparing(LockedSlot::startAt))
                        .map(LockedSlot::slotId)
                        .toList());
    }
}
