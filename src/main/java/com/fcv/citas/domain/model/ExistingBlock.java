package com.fcv.citas.domain.model;

/** Bloque activo ya publicado por el mismo profesional (en cualquier sede). */
public record ExistingBlock(long id, BlockSchedule schedule) {
}
