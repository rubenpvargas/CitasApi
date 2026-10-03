package com.fcv.citas.domain.model;

import java.time.LocalDateTime;

/** Inicio reservable: [startAt, endAt) cubierto por slots libres consecutivos del mismo bloque. */
public record AvailableStart(long blockId, LocalDateTime startAt, LocalDateTime endAt) {
}
