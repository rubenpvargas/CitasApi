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

    @GetMapping("/me/affiliations")
    List<Map<String,Object>> affiliations(@AuthenticationPrincipal Jwt jwt) { return service.affiliations(userId(jwt)); }

    @PutMapping("/me/affiliations")
    Map<String,Object> affiliation(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody AffiliationRequest request) {
        return service.saveAffiliation(userId(jwt), request.planId(), request.membershipNumber());
    }

    @GetMapping("/admin/eps") @PreAuthorize("hasRole('ADMIN')")
    List<Map<String,Object>> eps() { return service.eps(); }
    @GetMapping("/admin/locations") @PreAuthorize("hasRole('ADMIN')")
    List<Map<String,Object>> locations() { return service.locations(); }
    @PostMapping("/admin/eps") @PreAuthorize("hasRole('ADMIN')")
    Map<String,Object> createEps(@Valid @RequestBody EpsRequest r) { return service.createEps(r.code(),r.name()); }
    @PatchMapping("/admin/eps/{id}") @PreAuthorize("hasRole('ADMIN')")
    Map<String,Object> updateEps(@PathVariable long id,@Valid @RequestBody CatalogUpdateRequest r){return service.updateEps(id,r.name(),r.active());}

    @GetMapping("/admin/plans") @PreAuthorize("hasRole('ADMIN')")
    List<Map<String,Object>> plans(@RequestParam(required=false) Long epsId){return service.plans(epsId);}
    @PostMapping("/admin/eps/{epsId}/plans") @PreAuthorize("hasRole('ADMIN')")
    Map<String,Object> createPlan(@PathVariable long epsId,@Valid @RequestBody PlanRequest r){return service.createPlan(epsId,r.regimeId(),r.code(),r.name());}
    @PatchMapping("/admin/plans/{id}") @PreAuthorize("hasRole('ADMIN')")
    Map<String,Object> updatePlan(@PathVariable long id,@Valid @RequestBody CatalogUpdateRequest r){return service.updatePlan(id,r.name(),r.active());}

    @GetMapping("/admin/specialties")
    List<Map<String,Object>> specialties(){return service.specialties();}
    @PostMapping("/admin/specialties") @PreAuthorize("hasRole('ADMIN')")
    Map<String,Object> createSpecialty(@Valid @RequestBody SpecialtyRequest r){return service.createSpecialty(r.code(),r.name(),r.durationMinutes(),r.general());}
    @PatchMapping("/admin/specialties/{id}") @PreAuthorize("hasRole('ADMIN')")
    Map<String,Object> updateSpecialty(@PathVariable long id,@Valid @RequestBody SpecialtyUpdateRequest r){return service.updateSpecialty(id,r.name(),r.durationMinutes(),r.active());}

    @GetMapping("/admin/professionals") @PreAuthorize("hasRole('ADMIN')")
    List<Map<String,Object>> professionals(){return service.professionals();}
    @PostMapping("/admin/professionals") @PreAuthorize("hasRole('ADMIN')")
    Map<String,Object> createProfessional(@Valid @RequestBody ProfessionalRequest r){return service.createProfessional(r.firstName(),r.lastName(),r.documentType(),r.documentNumber(),r.email(),r.phone(),r.password(),r.professionalCode(),r.licenseNumber());}
    @PutMapping("/admin/professionals/{id}/capabilities") @PreAuthorize("hasRole('ADMIN')")
    Map<String,Object> configureProfessional(@PathVariable long id,@Valid @RequestBody CapabilitiesRequest r){return service.configureProfessional(id,r.specialtyIds(),r.primarySpecialtyId(),r.locationIds(),r.active());}

    @PostMapping("/professional/blocks") @PreAuthorize("hasRole('PROFESSIONAL')")
    Map<String,Object> createBlock(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody BlockRequest r){return service.createBlock(userId(jwt),r.date(),r.startTime(),r.endTime(),r.locationCode());}
    @PatchMapping("/professional/blocks/{id}") @PreAuthorize("hasRole('PROFESSIONAL')")
    Map<String,Object> updateBlock(@AuthenticationPrincipal Jwt jwt,@PathVariable long id,@Valid @RequestBody BlockRequest r){return service.updateBlock(userId(jwt),id,r.date(),r.startTime(),r.endTime(),r.locationCode());}
    @DeleteMapping("/professional/blocks/{id}") @PreAuthorize("hasRole('PROFESSIONAL')")
    void deleteBlock(@AuthenticationPrincipal Jwt jwt,@PathVariable long id){service.deleteBlock(userId(jwt),id);}
    @GetMapping("/professional/calendar") @PreAuthorize("hasRole('PROFESSIONAL')")
    List<Map<String,Object>> calendar(@AuthenticationPrincipal Jwt jwt,@RequestParam LocalDate from,@RequestParam LocalDate to){return service.calendar(userId(jwt),from,to);}
    @GetMapping("/professional/agenda") @PreAuthorize("hasRole('PROFESSIONAL')")
    List<Map<String,Object>> agenda(@AuthenticationPrincipal Jwt jwt,@RequestParam LocalDate from,@RequestParam LocalDate to,@RequestParam(required=false) String locationCode){return service.professionalAgenda(userId(jwt),from,to,locationCode);}
    @PostMapping("/professional/appointments/{id}/close") @PreAuthorize("hasRole('PROFESSIONAL')")
    void close(@AuthenticationPrincipal Jwt jwt,@PathVariable long id,@Valid @RequestBody CloseRequest r){service.closeAppointment(userId(jwt),id,r.outcome());}

    @GetMapping("/availability")
    List<Map<String,Object>> availability(@RequestParam(required=false) String locationCode,@RequestParam(required=false) Long specialtyId,@RequestParam(required=false) Long professionalId,@RequestParam(required=false) LocalDate date){return service.availability(locationCode,specialtyId,professionalId,date);}
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

    record AffiliationRequest(@NotNull Long planId,@NotBlank @Size(max=80) String membershipNumber){}
    record EpsRequest(@NotBlank @Size(max=40) String code,@NotBlank @Size(max=160) String name){}
    record CatalogUpdateRequest(@NotBlank @Size(max=160) String name,boolean active){}
    record PlanRequest(@NotNull Long regimeId,@NotBlank String code,@NotBlank String name){}
    record SpecialtyRequest(@NotBlank String code,@NotBlank String name,@NotNull Integer durationMinutes,boolean general){}
    record SpecialtyUpdateRequest(@NotBlank String name,@NotNull Integer durationMinutes,boolean active){}
    record ProfessionalRequest(@NotBlank String firstName,@NotBlank String lastName,@NotBlank String documentType,@NotBlank String documentNumber,@Email @NotBlank String email,@NotBlank String phone,@NotBlank @Size(min=8,max=72) String password,@NotBlank String professionalCode,@NotBlank String licenseNumber){}
    record CapabilitiesRequest(@NotNull List<Long> specialtyIds,Long primarySpecialtyId,@NotNull List<Long> locationIds,boolean active){}
    record BlockRequest(@NotNull @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate date,@NotNull LocalTime startTime,@NotNull LocalTime endTime,@NotBlank String locationCode){}
    record BookingRequest(Long specialtyId,@NotNull Long professionalId,@NotBlank String locationCode,@NotBlank String startAt,String reason){}
    record RescheduleRequest(@NotBlank String startAt,@NotBlank String locationCode){}
    record DecisionRequest(boolean approve,String reason){}
    record CloseRequest(@NotBlank String outcome){}
}
