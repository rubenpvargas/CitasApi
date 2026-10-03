package com.fcv.citas.adapter.in.rest;

import com.fcv.citas.application.model.BookingCommand;
import com.fcv.citas.application.port.in.BookingUseCase;
import com.fcv.citas.application.port.in.MyAppointmentsUseCase;
import com.fcv.citas.application.port.in.RescheduleUseCase;
import com.fcv.citas.domain.model.RescheduleRequest;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fcv.citas.domain.model.Appointment;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

/** Olas D/E — citas del USER autenticado (ownership por sub). */
@RestController
@RequestMapping("/api/v1/appointments")
@PreAuthorize("hasRole('USER')")
public class AppointmentController {
    private final BookingUseCase booking;
    private final MyAppointmentsUseCase mine;
    private final RescheduleUseCase reschedules;
    private final Clock clock;

    public AppointmentController(BookingUseCase booking, MyAppointmentsUseCase mine, RescheduleUseCase reschedules,
                                 Clock clock) {
        this.booking = booking;
        this.mine = mine;
        this.reschedules = reschedules;
        this.clock = clock;
    }

    @PostMapping("/general")
    ResponseEntity<AppointmentResponse> bookGeneral(@AuthenticationPrincipal Jwt jwt,
                                                    @Valid @RequestBody GeneralBookingRequest r) {
        return created(booking.bookGeneral(JwtSubject.userId(jwt),
                new BookingCommand(null, r.professionalId(), r.locationCode(), r.startAt(), r.reason())));
    }

    @PostMapping("/specialized")
    ResponseEntity<AppointmentResponse> bookSpecialized(@AuthenticationPrincipal Jwt jwt,
                                                        @Valid @RequestBody SpecializedBookingRequest r) {
        return created(booking.bookSpecialized(JwtSubject.userId(jwt),
                new BookingCommand(r.specialtyId(), r.professionalId(), r.locationCode(), r.startAt(), r.reason())));
    }

    @GetMapping
    List<AppointmentResponse> list(@AuthenticationPrincipal Jwt jwt, @RequestParam(required = false) String status,
                                   @RequestParam(required = false) LocalDate from,
                                   @RequestParam(required = false) LocalDate to) {
        return mine.list(JwtSubject.userId(jwt), status, from, to).stream().map(this::response).toList();
    }

    @GetMapping("/{id}")
    AppointmentResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable long id) {
        return response(mine.get(JwtSubject.userId(jwt), id));
    }

    @PostMapping("/{id}/cancel")
    AppointmentResponse cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable long id) {
        return response(mine.cancel(JwtSubject.userId(jwt), id));
    }

    @PostMapping("/{id}/reschedule")
    ResponseEntity<RescheduleResponse> reschedule(@AuthenticationPrincipal Jwt jwt, @PathVariable long id,
                                                  @Valid @RequestBody RescheduleBody body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(RescheduleResponse.from(
                reschedules.request(JwtSubject.userId(jwt), id, body.startAt(), body.locationCode())));
    }

    private ResponseEntity<AppointmentResponse> created(Appointment appointment) {
        return ResponseEntity.status(HttpStatus.CREATED).body(response(appointment));
    }

    AppointmentResponse response(Appointment appointment) {
        return AppointmentResponse.from(appointment, LocalDateTime.now(clock));
    }

    /** Profesional y especialidad se conservan: no se aceptan en el cuerpo. */
    record RescheduleBody(@NotNull LocalDateTime startAt, @NotBlank @Size(max = 30) String locationCode) {
    }

    record RescheduleResponse(long id, long appointmentId, String status,
                              @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime requestedStartAt,
                              @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime requestedEndAt,
                              String locationCode) {
        static RescheduleResponse from(RescheduleRequest r) {
            return new RescheduleResponse(r.id(), r.appointmentId(), r.status().name(), r.requestedStartAt(),
                    r.requestedEndAt(), r.requestedLocationCode());
        }
    }

    record GeneralBookingRequest(@NotNull Long professionalId, @NotBlank @Size(max = 30) String locationCode,
                                 @NotNull LocalDateTime startAt, @Size(max = 500) String reason) {
    }

    record SpecializedBookingRequest(@NotNull Long specialtyId, @NotNull Long professionalId,
                                     @NotBlank @Size(max = 30) String locationCode, @NotNull LocalDateTime startAt,
                                     @Size(max = 500) String reason) {
    }
}
