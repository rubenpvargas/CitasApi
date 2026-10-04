package com.fcv.citas.application.model;

import java.time.LocalDateTime;

/** WF-001 — datos mínimos de una cita APPROVED próxima para el recordatorio (sin documento, teléfono ni ids de usuario). */
public record ReminderItem(long appointmentId, LocalDateTime startAt, LocalDateTime endAt, String locationName,
                           String specialtyName, String professionalName, String recipientFirstName,
                           String recipientEmail) {
}
