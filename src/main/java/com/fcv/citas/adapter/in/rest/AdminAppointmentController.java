package com.fcv.citas.adapter.in.rest;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fcv.citas.application.model.AdminInbox;
import com.fcv.citas.application.model.InboxFilter;
import com.fcv.citas.application.port.in.AdminInboxUseCase;
import com.fcv.citas.application.port.in.AppointmentDecisionUseCase;
import com.fcv.citas.application.port.in.RescheduleUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Olas D–F — decisiones ADMIN sobre citas y reprogramaciones, y bandeja de pendientes. */
@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAppointmentController {
    private final AppointmentDecisionUseCase decisions;
    private final RescheduleUseCase reschedules;
    private final AdminInboxUseCase inbox;
    private final Clock clock;

    public AdminAppointmentController(AppointmentDecisionUseCase decisions, RescheduleUseCase reschedules,
                                      AdminInboxUseCase inbox, Clock clock) {
        this.decisions = decisions;
        this.reschedules = reschedules;
        this.inbox = inbox;
        this.clock = clock;
    }

    @PostMapping("/appointments/{id}/decision")
    AppointmentResponse decide(@AuthenticationPrincipal Jwt jwt, @PathVariable long id,
                               @Valid @RequestBody DecisionRequest request) {
        return AppointmentResponse.from(decisions.decide(JwtSubject.userId(jwt), id, request.approve(), request.reason()),
                LocalDateTime.now(clock));
    }

    @PostMapping("/reschedules/{id}/decision")
    AppointmentResponse decideReschedule(@AuthenticationPrincipal Jwt jwt, @PathVariable long id,
                                         @Valid @RequestBody DecisionRequest request) {
        return AppointmentResponse.from(reschedules.decide(JwtSubject.userId(jwt), id, request.approve(), request.reason()),
                LocalDateTime.now(clock));
    }

    @GetMapping("/inbox")
    InboxResponse inbox(@RequestParam(required = false) String locationCode,
                        @RequestParam(required = false) Long professionalId,
                        @RequestParam(required = false) Long specialtyId,
                        @RequestParam(required = false) LocalDate from,
                        @RequestParam(required = false) LocalDate to) {
        AdminInbox inbox = this.inbox.inbox(new InboxFilter(locationCode, professionalId, specialtyId, from, to));
        return new InboxResponse(inbox.appointments().stream().map(a -> new InboxAppointment(a.id(), a.patientName(),
                        a.professionalId(), a.professionalName(), a.specialtyId(), a.specialtyName(), a.locationCode(),
                        a.startAt(), a.endAt(), wall(a.createdAt()))).toList(),
                inbox.reschedules().stream().map(r -> new InboxReschedule(r.id(), r.appointmentId(), r.patientName(),
                        r.professionalId(), r.professionalName(), r.specialtyId(), r.specialtyName(), r.locationCode(),
                        r.currentStartAt(), r.currentEndAt(), r.requestedStartAt(), r.requestedEndAt(),
                        wall(r.createdAt()))).toList());
    }

    private LocalDateTime wall(java.time.Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, clock.getZone());
    }

    record InboxResponse(List<InboxAppointment> appointments, List<InboxReschedule> reschedules) {
    }

    record InboxAppointment(long id, String patientName, long professionalId, String professionalName, long specialtyId,
                            String specialtyName, String locationCode,
                            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime startAt,
                            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime endAt,
                            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime createdAt) {
    }

    record InboxReschedule(long id, long appointmentId, String patientName, long professionalId, String professionalName,
                           long specialtyId, String specialtyName, String locationCode,
                           @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime currentStartAt,
                           @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime currentEndAt,
                           @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime requestedStartAt,
                           @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime requestedEndAt,
                           @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime createdAt) {
    }

    record DecisionRequest(@NotNull Boolean approve, @Size(max = 500) String reason) {
    }
}
