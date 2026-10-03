package com.fcv.citas.domain.model;

import java.time.LocalDateTime;

/** Slot libre (sin cita ni retención) de 30 minutos perteneciente a un bloque activo. */
public record FreeSlot(long blockId, LocalDateTime startAt) {
}
