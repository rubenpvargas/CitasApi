package com.fcv.citas.application.port.in;

import com.fcv.citas.domain.model.Appointment;
import com.fcv.citas.domain.model.RescheduleRequest;

import java.time.LocalDateTime;

/** HU-021/HU-022 — solicitud de reprogramación del USER y decisión ADMIN. */
public interface RescheduleUseCase {
    RescheduleRequest request(long userId, long appointmentId, LocalDateTime startAt, String locationCode);

    Appointment decide(long adminUserId, long requestId, boolean approve, String reason);
}
