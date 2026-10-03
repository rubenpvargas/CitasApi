package com.fcv.citas.application.model;

import java.time.LocalDate;

/** HU-025 — filtros opcionales de la bandeja ADMIN. */
public record InboxFilter(String locationCode, Long professionalId, Long specialtyId, LocalDate from, LocalDate to) {
}
