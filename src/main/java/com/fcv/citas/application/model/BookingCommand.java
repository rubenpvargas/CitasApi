package com.fcv.citas.application.model;

import java.time.LocalDateTime;

/** HU-016/HU-017 — specialtyId es nulo para la cita general (se usa la especialidad general activa). */
public record BookingCommand(Long specialtyId, long professionalId, String locationCode, LocalDateTime startAt,
                             String reason) {
}
