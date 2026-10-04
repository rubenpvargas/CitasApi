package com.fcv.citas.application.service;

import com.fcv.citas.domain.model.Appointment;
import com.fcv.citas.domain.model.NotificationType;

/** Registra, dentro de la transacción de la transición, el evento de estado para WF-002. */
@FunctionalInterface
public interface StatusEventRecorder {
    void record(NotificationType type, Appointment appointmentAfterTransition, String reason);

    static StatusEventRecorder none() {
        return (type, appointment, reason) -> { };
    }
}
