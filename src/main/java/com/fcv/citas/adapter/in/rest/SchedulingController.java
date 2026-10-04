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


    @GetMapping("/admin/inbox") @PreAuthorize("hasRole('ADMIN')")
    List<Map<String,Object>> inbox(){return service.adminInbox();}
    @GetMapping("/admin/automation/appointments/reminders") @PreAuthorize("hasRole('ADMIN')")
    List<Map<String,Object>> reminderAppointments(@RequestParam(defaultValue = "24") int hours){return service.upcomingReminderAppointments(hours);}

    private long userId(Jwt jwt){return Long.parseLong(jwt.getSubject());}

    record CloseRequest(@NotBlank String outcome){}
}
