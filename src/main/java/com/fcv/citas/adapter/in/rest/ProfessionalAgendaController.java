package com.fcv.citas.adapter.in.rest;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fcv.citas.application.model.BlockCommand;
import com.fcv.citas.application.model.CalendarEntry;
import com.fcv.citas.application.port.in.AvailabilityBlockUseCase;
import com.fcv.citas.application.port.in.ProfessionalAgendaUseCase;
import com.fcv.citas.domain.model.AvailabilityBlock;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** HU-012..HU-014 — agenda propia del PROFESSIONAL; el profesional se deriva del sub del JWT. */
@RestController
@RequestMapping("/api/v1/professional")
@PreAuthorize("hasRole('PROFESSIONAL')")
public class ProfessionalAgendaController {
    private final AvailabilityBlockUseCase blocks;
    private final ProfessionalAgendaUseCase agenda;
    private final java.time.Clock clock;

    public ProfessionalAgendaController(AvailabilityBlockUseCase blocks, ProfessionalAgendaUseCase agenda,
                                        java.time.Clock clock) {
        this.blocks = blocks;
        this.agenda = agenda;
        this.clock = clock;
    }

    @GetMapping("/agenda")
    List<AgendaItemResponse> agenda(@AuthenticationPrincipal Jwt jwt, @RequestParam LocalDate from,
                                    @RequestParam LocalDate to, @RequestParam(required = false) String locationCode) {
        java.time.LocalDateTime now = java.time.LocalDateTime.now(clock);
        return agenda.agenda(JwtSubject.userId(jwt), from, to, locationCode).stream()
                .map(a -> AgendaItemResponse.from(a, now)).toList();
    }

    @PostMapping("/appointments/{id}/close")
    AppointmentResponse close(@AuthenticationPrincipal Jwt jwt, @PathVariable long id, @Valid @RequestBody CloseRequest request) {
        return AppointmentResponse.from(agenda.close(JwtSubject.userId(jwt), id,
                com.fcv.citas.domain.model.AppointmentStatus.valueOf(request.outcome())), java.time.LocalDateTime.now(clock));
    }

    record CloseRequest(@NotBlank @jakarta.validation.constraints.Pattern(regexp = "COMPLETED|NO_SHOW") String outcome) {
    }

    /** HU-023 — datos mínimos: solo el nombre del paciente (sin documento, email ni teléfono). */
    record AgendaItemResponse(long id,
                              @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") java.time.LocalDateTime startAt,
                              @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") java.time.LocalDateTime endAt,
                              String locationCode, String specialtyName, String patientName, boolean closable) {
        static AgendaItemResponse from(com.fcv.citas.domain.model.Appointment a, java.time.LocalDateTime now) {
            return new AgendaItemResponse(a.id(), a.startAt(), a.endAt(), a.locationCode(), a.specialtyName(),
                    a.patientName(), a.closableAt(now));
        }
    }

    @PostMapping("/blocks")
    ResponseEntity<BlockResponse> create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody BlockRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BlockResponse.from(blocks.create(JwtSubject.userId(jwt), request.toCommand())));
    }

    @PatchMapping("/blocks/{id}")
    BlockResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable long id, @Valid @RequestBody BlockRequest request) {
        return BlockResponse.from(blocks.update(JwtSubject.userId(jwt), id, request.toCommand()));
    }

    @DeleteMapping("/blocks/{id}")
    ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable long id) {
        blocks.delete(JwtSubject.userId(jwt), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/calendar")
    List<CalendarItemResponse> calendar(@AuthenticationPrincipal Jwt jwt, @RequestParam LocalDate from,
                                        @RequestParam LocalDate to, @RequestParam(required = false) String locationCode) {
        return blocks.calendar(JwtSubject.userId(jwt), from, to, locationCode).stream()
                .map(CalendarItemResponse::from).toList();
    }

    /** HU-014 — sin datos de pacientes; notEditableReason es aditivo (PAST_BLOCK | BLOCK_COMMITTED | null). */
    record CalendarItemResponse(long id, LocalDate date, LocalDate availableDate,
                                @JsonFormat(pattern = "HH:mm") LocalTime startTime,
                                @JsonFormat(pattern = "HH:mm") LocalTime endTime,
                                String locationCode, String locationName, int totalSlots, int committedSlots,
                                boolean editable, String notEditableReason) {
        static CalendarItemResponse from(CalendarEntry entry) {
            AvailabilityBlock b = entry.block();
            return new CalendarItemResponse(b.id(), b.schedule().date(), b.schedule().date(), b.schedule().startTime(),
                    b.schedule().endTime(), b.locationCode(), b.locationName(), b.totalSlots(), b.committedSlots(),
                    entry.editable(), entry.notEditableReason().map(Enum::name).orElse(null));
        }
    }

    record BlockRequest(@NotNull LocalDate date, @NotNull LocalTime startTime, @NotNull LocalTime endTime,
                        @NotBlank @Size(max = 30) String locationCode) {
        BlockCommand toCommand() {
            return new BlockCommand(date, startTime, endTime, locationCode);
        }
    }

    /** {@code availableDate} se conserva por compatibilidad; {@code date} es el campo del contrato. */
    record BlockResponse(long id, LocalDate date, LocalDate availableDate,
                         @JsonFormat(pattern = "HH:mm") LocalTime startTime,
                         @JsonFormat(pattern = "HH:mm") LocalTime endTime,
                         String locationCode, String locationName, int totalSlots, int committedSlots) {
        static BlockResponse from(AvailabilityBlock b) {
            return new BlockResponse(b.id(), b.schedule().date(), b.schedule().date(), b.schedule().startTime(),
                    b.schedule().endTime(), b.locationCode(), b.locationName(), b.totalSlots(), b.committedSlots());
        }
    }
}
