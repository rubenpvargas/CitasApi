package com.fcv.citas.application.service;

import com.fcv.citas.application.exception.BusinessRuleException;
import com.fcv.citas.application.exception.NotFoundException;
import com.fcv.citas.application.exception.RequestValidationException;
import com.fcv.citas.application.model.BookingCommand;
import com.fcv.citas.application.model.NewAppointment;
import com.fcv.citas.application.port.in.BookingUseCase;
import com.fcv.citas.application.port.out.AppointmentRepositoryPort;
import com.fcv.citas.application.port.out.ProfessionalRepositoryPort;
import com.fcv.citas.application.port.out.SlotRepositoryPort;
import com.fcv.citas.application.port.out.SpecialtyRepositoryPort;
import com.fcv.citas.application.port.out.TransactionPort;
import com.fcv.citas.domain.model.Appointment;
import com.fcv.citas.domain.model.AppointmentStatus;
import com.fcv.citas.domain.model.BookingSlots;
import com.fcv.citas.domain.model.Location;
import com.fcv.citas.domain.model.ProfessionalSummary;
import com.fcv.citas.domain.model.Specialty;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

/**
 * HU-016/HU-017 — reserva transaccional. Revalida al confirmar (profesional activo, especialidad activa
 * asignada, sede asignada, inicio futuro) y bloquea con FOR UPDATE los slots libres de la ventana; los N
 * slots consecutivos del mismo bloque se eligen con la regla de dominio {@link BookingSlots}.
 */
public final class BookingService implements BookingUseCase {
    static final String SOURCE_USER = "USER";

    private final AppointmentRepositoryPort appointments;
    private final SlotRepositoryPort slots;
    private final ProfessionalRepositoryPort professionals;
    private final SpecialtyRepositoryPort specialties;
    private final TransactionPort transactions;
    private final Clock clock;

    public BookingService(AppointmentRepositoryPort appointments, SlotRepositoryPort slots,
                          ProfessionalRepositoryPort professionals, SpecialtyRepositoryPort specialties,
                          TransactionPort transactions, Clock clock) {
        this.appointments = appointments;
        this.slots = slots;
        this.professionals = professionals;
        this.specialties = specialties;
        this.transactions = transactions;
        this.clock = clock;
    }

    @Override
    public Appointment bookGeneral(long userId, BookingCommand command) {
        Specialty general = specialties.findAll().stream().filter(s -> s.general() && s.active()).findFirst()
                .orElseThrow(() -> new BusinessRuleException("CATALOG_INACTIVE", "No active general specialty"));
        return book(userId, command, general);
    }

    @Override
    public Appointment bookSpecialized(long userId, BookingCommand command) {
        if (command.specialtyId() == null) {
            throw new RequestValidationException("specialtyId", "is required");
        }
        Specialty specialty = specialties.findById(command.specialtyId())
                .orElseThrow(() -> new NotFoundException("Specialty not found"));
        if (specialty.general()) {
            throw new RequestValidationException("specialtyId", "must be a specialized specialty");
        }
        if (!specialty.active()) {
            throw new BusinessRuleException("CATALOG_INACTIVE", "The specialty is inactive");
        }
        return book(userId, command, specialty);
    }

    private Appointment book(long userId, BookingCommand command, Specialty specialty) {
        return transactions.required(() -> {
            ProfessionalSummary professional = professionals.findById(command.professionalId())
                    .orElseThrow(() -> new NotFoundException("Professional not found"));
            if (!professional.active()) {
                throw new BusinessRuleException("PROFESSIONAL_INACTIVE", "The professional is not active");
            }
            if (professional.specialties().stream().noneMatch(s -> s.id() == specialty.id())) {
                throw new BusinessRuleException("SPECIALTY_NOT_ASSIGNED", "The professional does not provide this specialty");
            }
            long locationId = assignedLocation(professional.id(), command.locationCode());
            LocalDateTime startAt = command.startAt();
            LocalDateTime endAt = startAt.plusMinutes(specialty.durationMinutes());
            List<Long> slotIds = BookingSlots.select(slots.lockFreeSlots(professional.id(), locationId, startAt, endAt),
                            startAt, specialty.durationMinutes(), LocalDateTime.now(clock))
                    .orElseThrow(SlotRules::unavailable);
            AppointmentStatus status = specialty.general() ? AppointmentStatus.APPROVED : AppointmentStatus.REQUESTED;
            long id = appointments.insert(new NewAppointment(userId, professional.id(), locationId, specialty.id(), status,
                    blankToNull(command.reason()), startAt, endAt,
                    status == AppointmentStatus.APPROVED ? LocalDateTime.now(clock) : null), clock.instant());
            slots.assignToAppointment(slotIds, id);
            appointments.addHistory(id, status, userId, SOURCE_USER, status == AppointmentStatus.APPROVED
                    ? "General appointment automatically approved" : "Specialized appointment requested", clock.instant());
            return appointments.findById(id).orElseThrow();
        });
    }

    long assignedLocation(long professionalId, String locationCode) {
        Location location = professionals.findLocationByCode(locationCode.trim().toUpperCase(Locale.ROOT))
                .orElseThrow(() -> new NotFoundException("Location not found"));
        if (!professionals.isLocationAssigned(professionalId, location.id())) {
            throw new BusinessRuleException("LOCATION_NOT_ASSIGNED", "The professional is not enabled at this location");
        }
        return location.id();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
