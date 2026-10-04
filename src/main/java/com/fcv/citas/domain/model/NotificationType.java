package com.fcv.citas.domain.model;

/** Eventos de cambio de estado que se notifican a WF-002 mediante el outbox. */
public enum NotificationType {
    APPOINTMENT_APPROVED,
    APPOINTMENT_REJECTED,
    APPOINTMENT_CANCELLED,
    RESCHEDULE_APPROVED,
    RESCHEDULE_REJECTED
}
