package com.fcv.citas.adapter.in.rest;

import com.fcv.citas.application.port.in.SpecialtyUseCase;
import com.fcv.citas.domain.model.Specialty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** HU-009 — gestión ADMIN de especialidades y lectura de activas para cualquier usuario autenticado. */
@RestController
@RequestMapping("/api/v1")
public class SpecialtyController {
    private final SpecialtyUseCase specialties;

    public SpecialtyController(SpecialtyUseCase specialties) {
        this.specialties = specialties;
    }

    @GetMapping("/specialties")
    List<PublicSpecialtyResponse> active() {
        return specialties.listActive().stream().map(PublicSpecialtyResponse::from).toList();
    }

    @GetMapping("/admin/specialties")
    @PreAuthorize("hasRole('ADMIN')")
    List<SpecialtyResponse> all() {
        return specialties.listAll().stream().map(SpecialtyResponse::from).toList();
    }

    @PostMapping("/admin/specialties")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<SpecialtyResponse> create(@Valid @RequestBody SpecialtyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(SpecialtyResponse.from(specialties.create(
                request.code(), request.name(), request.durationMinutes(), request.general())));
    }

    @PatchMapping("/admin/specialties/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    SpecialtyResponse update(@PathVariable long id, @Valid @RequestBody SpecialtyUpdateRequest request) {
        return SpecialtyResponse.from(specialties.update(id, request.name(), request.durationMinutes(), request.active()));
    }

    record SpecialtyRequest(@NotBlank @Size(max = 60) @Pattern(regexp = InsuranceAdminController.CODE_PATTERN) String code,
                            @NotBlank @Size(max = 160) String name,
                            @NotNull Integer durationMinutes,
                            boolean general) {
    }

    record SpecialtyUpdateRequest(@NotBlank @Size(max = 160) String name, @NotNull Integer durationMinutes,
                                  @NotNull Boolean active) {
    }

    record SpecialtyResponse(Long id, String code, String name, int durationMinutes, boolean general,
                             boolean requiresAdminApproval, boolean active) {
        static SpecialtyResponse from(Specialty s) {
            return new SpecialtyResponse(s.id(), s.code(), s.name(), s.durationMinutes(), s.general(),
                    s.requiresAdminApproval(), s.active());
        }
    }

    record PublicSpecialtyResponse(Long id, String code, String name, int durationMinutes, boolean general) {
        static PublicSpecialtyResponse from(Specialty s) {
            return new PublicSpecialtyResponse(s.id(), s.code(), s.name(), s.durationMinutes(), s.general());
        }
    }
}
