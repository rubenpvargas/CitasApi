package com.fcv.citas.application.model;

import java.time.LocalDateTime;

/** Cita aprobada próxima para el recordatorio de la automatización (se reemplazará en la ola G). */
public record ReminderItem(long id, LocalDateTime startAt, String status, String patientEmail) {
}
