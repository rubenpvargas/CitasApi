package com.fcv.citas.domain.model;

import java.time.LocalDateTime;

/** Franja discreta de 30 minutos en hora local de pared. */
public record SlotTime(LocalDateTime startAt, LocalDateTime endAt) {
}
