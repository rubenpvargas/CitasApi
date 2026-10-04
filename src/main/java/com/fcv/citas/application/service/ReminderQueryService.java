package com.fcv.citas.application.service;

import com.fcv.citas.application.exception.RequestValidationException;
import com.fcv.citas.application.model.ReminderItem;
import com.fcv.citas.application.port.in.ReminderQueryUseCase;
import com.fcv.citas.application.port.out.AppointmentRepositoryPort;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * WF-001 sin duplicados — con disparo horario y windowMinutes = 60, cada cita cae en una única ventana
 * semiabierta [ahora + hours − windowMinutes, ahora + hours) calculada con el Clock zonificado.
 */
public final class ReminderQueryService implements ReminderQueryUseCase {
    private final AppointmentRepositoryPort appointments;
    private final Clock clock;

    public ReminderQueryService(AppointmentRepositoryPort appointments, Clock clock) {
        this.appointments = appointments;
        this.clock = clock;
    }

    @Override
    public List<ReminderItem> upcoming(int hours, int windowMinutes) {
        if (hours < 1 || hours > 72) {
            throw new RequestValidationException("hours", "must be between 1 and 72");
        }
        if (windowMinutes < 15 || windowMinutes > 120) {
            throw new RequestValidationException("windowMinutes", "must be between 15 and 120");
        }
        LocalDateTime end = LocalDateTime.now(clock).withSecond(0).withNano(0).plusHours(hours);
        return appointments.findApprovedStartingBetween(end.minusMinutes(windowMinutes), end);
    }
}
