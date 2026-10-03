package com.fcv.citas.application.model;

import java.time.LocalDate;
import java.time.LocalTime;

/** Datos de un bloque; el profesional nunca viene en el cuerpo, se deriva del JWT. */
public record BlockCommand(LocalDate date, LocalTime startTime, LocalTime endTime, String locationCode) {
}
