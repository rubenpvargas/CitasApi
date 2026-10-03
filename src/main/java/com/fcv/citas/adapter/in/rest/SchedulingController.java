package com.fcv.citas.adapter.in.rest;

import com.fcv.citas.application.service.SchedulingService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class SchedulingController {
    private final SchedulingService service;

    public SchedulingController(SchedulingService service) { this.service = service; }





    @GetMapping("/professional/agenda") @PreAuthorize("hasRole('PROFESSIONAL')")
    List<Map<String,Object>> agenda(@AuthenticationPrincipal Jwt jwt,@RequestParam LocalDate from,@RequestParam LocalDate to,@RequestParam(required=false) String locationCode){return service.professionalAgenda(userId(jwt),from,to,locationCode);}
    @PostMapping("/professional/appointments/{id}/close") @PreAuthorize("hasRole('PROFESSIONAL')")
    void close(@AuthenticationPrincipal Jwt jwt,@PathVariable long id,@Valid @RequestBody CloseRequest r){service.closeAppointment(userId(jwt),id,r.outcome());}

    @PostMapping("/appointments/general")
    Map<String,Object> bookGeneral(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody BookingRequest r){return service.book(userId(jwt),null,r.professionalId(),r.locationCode(),LocalDateTime.parse(r.startAt()),r.reason(),true);}
    @PostMapping("/appointments/specialized")
    Map<String,Object> bookSpecialized(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody BookingRequest r){return service.book(userId(jwt),r.specialtyId(),r.professionalId(),r.locationCode(),LocalDateTime.parse(r.startAt()),r.reason(),false);}
    @GetMapping("/appointments")
    List<Map<String,Object>> appointments(@AuthenticationPrincipal Jwt jwt,@RequestParam(required=false) String status,@RequestParam(required=false) LocalDate from,@RequestParam(required=false) LocalDate to){return service.appointments(userId(jwt),status,from,to);}
    @PostMapping("/appointments/{id}/cancel")
    void cancel(@AuthenticationPrincipal Jwt jwt,@PathVariable long id){service.cancel(userId(jwt),id);}
    @PostMapping("/appointments/{id}/reschedule")
    Map<String,Object> reschedule(@AuthenticationPrincipal Jwt jwt,@PathVariable long id,@Valid @RequestBody RescheduleRequest r){return service.requestReschedule(userId(jwt),id,LocalDateTime.parse(r.startAt()),r.locationCode());}

    @GetMapping("/admin/inbox") @PreAuthorize("hasRole('ADMIN')")
    List<Map<String,Object>> inbox(){return service.adminInbox();}
    @GetMapping("/admin/automation/appointments/reminders") @PreAuthorize("hasRole('ADMIN')")
    List<Map<String,Object>> reminderAppointments(@RequestParam(defaultValue = "24") int hours){return service.upcomingReminderAppointments(hours);}
    @PostMapping("/admin/appointments/{id}/decision") @PreAuthorize("hasRole('ADMIN')")
    void appointmentDecision(@AuthenticationPrincipal Jwt jwt,@PathVariable long id,@Valid @RequestBody DecisionRequest r){service.decideAppointment(userId(jwt),id,r.approve(),r.reason());}
    @PostMapping("/admin/reschedules/{id}/decision") @PreAuthorize("hasRole('ADMIN')")
    void rescheduleDecision(@AuthenticationPrincipal Jwt jwt,@PathVariable long id,@Valid @RequestBody DecisionRequest r){service.decideReschedule(userId(jwt),id,r.approve(),r.reason());}

    private long userId(Jwt jwt){return Long.parseLong(jwt.getSubject());}

    record BookingRequest(Long specialtyId,@NotNull Long professionalId,@NotBlank String locationCode,@NotBlank String startAt,String reason){}
    record RescheduleRequest(@NotBlank String startAt,@NotBlank String locationCode){}
    record DecisionRequest(boolean approve,String reason){}
    record CloseRequest(@NotBlank String outcome){}
}
