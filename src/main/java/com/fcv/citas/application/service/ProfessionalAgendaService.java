package com.fcv.citas.application.service;

import com.fcv.citas.application.exception.ForbiddenException;
import com.fcv.citas.application.port.in.ProfessionalAgendaUseCase;
import com.fcv.citas.application.port.out.AppointmentRepositoryPort;
import com.fcv.citas.application.port.out.ProfessionalRepositoryPort;
import com.fcv.citas.application.port.out.TransactionPort;
import com.fcv.citas.domain.model.Appointment;
import com.fcv.citas.domain.model.ProfessionalAccount;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

/** HU-023 — solo citas APPROVED propias (profesional derivado del JWT) en [from, to] de máx. 31 días. */
public final class ProfessionalAgendaService implements ProfessionalAgendaUseCase {
    private final AppointmentRepositoryPort appointments;
    private final ProfessionalRepositoryPort professionals;
    private final TransactionPort transactions;
    private final Clock clock;

    public ProfessionalAgendaService(AppointmentRepositoryPort appointments, ProfessionalRepositoryPort professionals,
                                     TransactionPort transactions, Clock clock) {
        this.appointments = appointments;
        this.professionals = professionals;
        this.transactions = transactions;
        this.clock = clock;
    }

    @Override
    public List<Appointment> agenda(long userId, LocalDate from, LocalDate to, String locationCode) {
        ProfessionalAccount account = account(userId);
        DateRanges.require(from, to);
        String location = locationCode == null || locationCode.isBlank() ? null
                : locationCode.trim().toUpperCase(Locale.ROOT);
        return appointments.findAgenda(account.professionalId(), from, to, location);
    }

    ProfessionalAccount account(long userId) {
        return professionals.findAccountByUserId(userId)
                .orElseThrow(() -> new ForbiddenException("A professional profile is required"));
    }
}
