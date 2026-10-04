package com.fcv.citas.application.service;

import com.fcv.citas.application.model.ReminderItem;
import com.fcv.citas.application.port.in.ReminderQueryUseCase;
import com.fcv.citas.application.port.out.AppointmentRepositoryPort;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/** Citas APPROVED que empiezan en las próximas {@code hours} horas (1..168), según el Clock zonificado. */
public final class ReminderQueryService implements ReminderQueryUseCase {
    private final AppointmentRepositoryPort appointments;
    private final Clock clock;

    public ReminderQueryService(AppointmentRepositoryPort appointments, Clock clock) {
        this.appointments = appointments;
        this.clock = clock;
    }

    @Override
    public List<ReminderItem> upcoming(int hours) {
        int window = Math.max(1, Math.min(hours, 168));
        LocalDateTime now = LocalDateTime.now(clock);
        return appointments.findApprovedStartingBetween(now, now.plusHours(window));
    }
}
