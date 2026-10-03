package com.fcv.citas.application.model;

import java.time.LocalDate;

/** HU-015 — filtros de disponibilidad: especialidad y rango obligatorios; sede y profesional opcionales. */
public record AvailabilityQuery(long specialtyId, LocalDate from, LocalDate to, String locationCode,
                                Long professionalId) {
}
