package com.fcv.citas.application.service;

import com.fcv.citas.application.model.StatusEvent;
import com.fcv.citas.application.port.out.NotificationOutboxPort;
import com.fcv.citas.application.port.out.ProfileRepositoryPort;
import com.fcv.citas.domain.model.Appointment;
import com.fcv.citas.domain.model.NotificationType;
import com.fcv.citas.domain.model.UserProfile;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Construye el payload del contrato (destinatario = paciente: nombre y email) y lo inserta en el outbox en
 * la misma transacción de la transición; si ésta hace rollback, no hay evento.
 */
public final class OutboxStatusEventRecorder implements StatusEventRecorder {
    private final NotificationOutboxPort outbox;
    private final ProfileRepositoryPort profiles;
    private final Clock clock;

    public OutboxStatusEventRecorder(NotificationOutboxPort outbox, ProfileRepositoryPort profiles, Clock clock) {
        this.outbox = outbox;
        this.profiles = profiles;
        this.clock = clock;
    }

    @Override
    public void record(NotificationType type, Appointment appointment, String reason) {
        UserProfile recipient = profiles.findActiveById(appointment.patientUserId()).orElse(null);
        StatusEvent event = new StatusEvent(UUID.randomUUID().toString(), "appointment-" + appointment.id(), type,
                LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS), appointment.id(), appointment.status().name(),
                appointment.startAt(), recipient == null ? null : recipient.firstName(),
                recipient == null ? null : recipient.email(), reason == null || reason.isBlank() ? null : reason.trim());
        outbox.enqueue(event, clock.instant());
    }
}
