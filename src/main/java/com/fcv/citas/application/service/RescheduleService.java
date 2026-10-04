package com.fcv.citas.application.service;

import com.fcv.citas.application.exception.BusinessRuleException;
import com.fcv.citas.application.exception.NotFoundException;
import com.fcv.citas.application.model.NewReschedule;
import com.fcv.citas.application.port.in.RescheduleUseCase;
import com.fcv.citas.application.port.out.AppointmentRepositoryPort;
import com.fcv.citas.application.port.out.ProfessionalRepositoryPort;
import com.fcv.citas.application.port.out.RescheduleRepositoryPort;
import com.fcv.citas.application.port.out.SlotRepositoryPort;
import com.fcv.citas.application.port.out.SpecialtyRepositoryPort;
import com.fcv.citas.application.port.out.TransactionPort;
import com.fcv.citas.domain.model.Appointment;
import com.fcv.citas.domain.model.BookingSlots;
import com.fcv.citas.domain.model.Location;
import com.fcv.citas.domain.model.ProfessionalSummary;
import com.fcv.citas.domain.model.RescheduleRequest;
import com.fcv.citas.domain.model.RescheduleStatus;
import com.fcv.citas.domain.model.Specialty;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

/**
 * HU-021 — solicitud: la cita original no cambia; la nueva franja (mismo profesional y especialidad,
 * sede asignada) queda retenida con FOR UPDATE sobre sus slots. Una sola PENDING por cita, serializada
 * por el bloqueo de fila de la cita.
 */
public final class RescheduleService implements RescheduleUseCase {
    private final AppointmentRepositoryPort appointments;
    private final RescheduleRepositoryPort reschedules;
    private final SlotRepositoryPort slots;
    private final ProfessionalRepositoryPort professionals;
    private final SpecialtyRepositoryPort specialties;
    private final TransactionPort transactions;
    private final Clock clock;
    private final StatusEventRecorder events;

    public RescheduleService(AppointmentRepositoryPort appointments, RescheduleRepositoryPort reschedules,
                             SlotRepositoryPort slots, ProfessionalRepositoryPort professionals,
                             SpecialtyRepositoryPort specialties, TransactionPort transactions, Clock clock,
                             StatusEventRecorder events) {
        this.events = events;
        this.appointments = appointments;
        this.reschedules = reschedules;
        this.slots = slots;
        this.professionals = professionals;
        this.specialties = specialties;
        this.transactions = transactions;
        this.clock = clock;
    }

    @Override
    public RescheduleRequest request(long userId, long appointmentId, LocalDateTime startAt, String locationCode) {
        return transactions.required(() -> {
            Appointment appointment = appointments.lockById(appointmentId)
                    .filter(a -> a.patientUserId() == userId)
                    .orElseThrow(() -> new NotFoundException("Appointment not found"));
            LocalDateTime now = LocalDateTime.now(clock);
            appointment.requireReschedulable(now);
            ProfessionalSummary professional = professionals.findById(appointment.professionalId()).orElseThrow();
            if (!professional.active()) {
                throw new BusinessRuleException("PROFESSIONAL_INACTIVE", "The professional is not active");
            }
            Specialty specialty = specialties.findById(appointment.specialtyId()).orElseThrow();
            if (!specialty.active()) {
                throw new BusinessRuleException("CATALOG_INACTIVE", "The specialty is inactive");
            }
            Location location = professionals.findLocationByCode(locationCode.trim().toUpperCase(Locale.ROOT))
                    .orElseThrow(() -> new NotFoundException("Location not found"));
            if (!professionals.isLocationAssigned(professional.id(), location.id())) {
                throw new BusinessRuleException("LOCATION_NOT_ASSIGNED", "The professional is not enabled at this location");
            }
            LocalDateTime endAt = startAt.plusMinutes(specialty.durationMinutes());
            List<Long> slotIds = BookingSlots.select(slots.lockFreeSlots(professional.id(), location.id(), startAt, endAt),
                    startAt, specialty.durationMinutes(), now).orElseThrow(SlotRules::unavailable);
            long requestId = reschedules.insert(new NewReschedule(appointmentId, userId, location.id(),
                    appointment.startAt(), appointment.endAt(), startAt, endAt), clock.instant());
            slots.holdForReschedule(slotIds, requestId);
            reschedules.addHistory(requestId, com.fcv.citas.domain.model.RescheduleStatus.PENDING, userId, "USER",
                    null, clock.instant());
            return reschedules.findPendingForAppointment(appointmentId).orElseThrow();
        });
    }

    /**
     * HU-022 — bloquea solicitud y cita. Aprobar: se liberan los slots antiguos, los retenidos pasan a la
     * cita y ésta adopta la nueva franja/sede. Rechazar (con motivo): se libera la retención y la cita
     * conserva su franja. Historial ADMIN en la solicitud y, al aprobar, también en la cita.
     */
    @Override
    public Appointment decide(long adminUserId, long requestId, boolean approve, String reason) {
        return transactions.required(() -> {
            RescheduleRequest request = reschedules.lockById(requestId)
                    .orElseThrow(() -> new NotFoundException("Reschedule request not found"));
            RescheduleStatus target = request.decide(approve, reason);
            Appointment appointment = appointments.lockById(request.appointmentId()).orElseThrow();
            String cleanReason = reason == null || reason.isBlank() ? null : reason.trim();
            if (target == RescheduleStatus.APPROVED) {
                if (appointment.status() != com.fcv.citas.domain.model.AppointmentStatus.APPROVED) {
                    throw new com.fcv.citas.domain.model.DomainRuleViolation("INVALID_TRANSITION",
                            "The appointment is no longer approved");
                }
                slots.releaseAppointment(appointment.id());
                slots.transferHoldToAppointment(requestId, appointment.id());
                appointments.moveTo(appointment.id(), request.requestedLocationId(), request.requestedStartAt(),
                        request.requestedEndAt(), clock.instant());
                appointments.addHistory(appointment.id(), com.fcv.citas.domain.model.AppointmentStatus.APPROVED,
                        adminUserId, "ADMIN", "Reschedule approved", clock.instant());
            } else {
                slots.releaseHold(requestId);
            }
            reschedules.updateStatus(requestId, target, adminUserId, cleanReason, LocalDateTime.now(clock));
            reschedules.addHistory(requestId, target, adminUserId, "ADMIN", cleanReason, clock.instant());
            Appointment after = appointments.findById(appointment.id()).orElseThrow();
            events.record(target == RescheduleStatus.APPROVED ? com.fcv.citas.domain.model.NotificationType.RESCHEDULE_APPROVED
                    : com.fcv.citas.domain.model.NotificationType.RESCHEDULE_REJECTED, after, cleanReason);
            return after;
        });
    }
}
