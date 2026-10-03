package com.fcv.citas.domain.model;

import java.time.LocalDateTime;

/** Slot libre leído con bloqueo de fila (SELECT … FOR UPDATE) dentro de la transacción de reserva. */
public record LockedSlot(long slotId, long blockId, LocalDateTime startAt) {
}
