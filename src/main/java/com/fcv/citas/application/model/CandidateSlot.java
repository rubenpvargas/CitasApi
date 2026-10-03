package com.fcv.citas.application.model;

import java.time.LocalDateTime;

/**
 * Slot libre de un bloque activo cuyo profesional está activo, tiene la especialidad asignada y activa,
 * y la sede asignada y activa (filtros resueltos por el adaptador de persistencia).
 */
public record CandidateSlot(long blockId, LocalDateTime startAt, long professionalId, String professionalName,
                            String locationCode, String locationName) {
}
