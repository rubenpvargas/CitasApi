package com.fcv.citas.adapter.in.rest;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fcv.citas.application.model.BlockCommand;
import com.fcv.citas.application.port.in.AvailabilityBlockUseCase;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalTime;

/** HU-012..HU-014 — agenda propia del PROFESSIONAL; el profesional se deriva del sub del JWT. */
@RestController
@RequestMapping("/api/v1/professional")
@PreAuthorize("hasRole('PROFESSIONAL')")
public class ProfessionalAgendaController {
    private final AvailabilityBlockUseCase blocks;

    public ProfessionalAgendaController(AvailabilityBlockUseCase blocks) {
        this.blocks = blocks;
    }

    @PostMapping("/blocks")
    ResponseEntity<BlockResponse> create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody BlockRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BlockResponse.from(blocks.create(JwtSubject.userId(jwt), request.toCommand())));
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
