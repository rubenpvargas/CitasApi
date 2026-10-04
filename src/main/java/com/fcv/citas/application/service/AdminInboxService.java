package com.fcv.citas.application.service;

import com.fcv.citas.application.exception.RequestValidationException;
import com.fcv.citas.application.model.AdminInbox;
import com.fcv.citas.application.model.InboxFilter;
import com.fcv.citas.application.port.in.AdminInboxUseCase;
import com.fcv.citas.application.port.out.AppointmentRepositoryPort;
import com.fcv.citas.application.port.out.RescheduleRepositoryPort;

import java.util.Locale;

/** HU-025 — solo pendientes (nunca resueltas), filtrables por sede, profesional, especialidad y fecha. */
public final class AdminInboxService implements AdminInboxUseCase {
    private final AppointmentRepositoryPort appointments;
    private final RescheduleRepositoryPort reschedules;

    public AdminInboxService(AppointmentRepositoryPort appointments, RescheduleRepositoryPort reschedules) {
        this.appointments = appointments;
        this.reschedules = reschedules;
    }

    @Override
    public AdminInbox inbox(InboxFilter raw) {
        if (raw.from() != null && raw.to() != null && raw.to().isBefore(raw.from())) {
            throw new RequestValidationException("to", "must not be before from");
        }
        String location = raw.locationCode() == null || raw.locationCode().isBlank() ? null
                : raw.locationCode().trim().toUpperCase(Locale.ROOT);
        InboxFilter filter = new InboxFilter(location, raw.professionalId(), raw.specialtyId(), raw.from(), raw.to());
        return new AdminInbox(appointments.findRequested(filter), reschedules.findPending(filter));
    }
}
