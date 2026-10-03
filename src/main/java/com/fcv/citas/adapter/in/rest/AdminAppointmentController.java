package com.fcv.citas.adapter.in.rest;

import com.fcv.citas.application.port.in.AppointmentDecisionUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.LocalDateTime;

/** Olas D–F — decisiones ADMIN sobre citas y reprogramaciones, y bandeja de pendientes. */
@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAppointmentController {
    private final AppointmentDecisionUseCase decisions;
    private final Clock clock;

    public AdminAppointmentController(AppointmentDecisionUseCase decisions, Clock clock) {
        this.decisions = decisions;
        this.clock = clock;
    }

    @PostMapping("/appointments/{id}/decision")
    AppointmentResponse decide(@AuthenticationPrincipal Jwt jwt, @PathVariable long id,
                               @Valid @RequestBody DecisionRequest request) {
        return AppointmentResponse.from(decisions.decide(JwtSubject.userId(jwt), id, request.approve(), request.reason()),
                LocalDateTime.now(clock));
    }

    record DecisionRequest(@NotNull Boolean approve, @Size(max = 500) String reason) {
    }
}
