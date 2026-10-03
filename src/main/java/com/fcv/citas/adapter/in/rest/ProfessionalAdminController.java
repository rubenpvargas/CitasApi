package com.fcv.citas.adapter.in.rest;

import com.fcv.citas.application.model.CapabilitiesCommand;
import com.fcv.citas.application.model.NewProfessionalCommand;
import com.fcv.citas.application.port.in.ProfessionalAdminUseCase;
import com.fcv.citas.domain.model.Location;
import com.fcv.citas.domain.model.ProfessionalCapabilitySpecialty;
import com.fcv.citas.domain.model.ProfessionalSummary;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** HU-010/HU-011 — administración de profesionales (solo ADMIN). Nunca devuelve password ni hash. */
@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class ProfessionalAdminController {
    private final ProfessionalAdminUseCase professionals;

    public ProfessionalAdminController(ProfessionalAdminUseCase professionals) {
        this.professionals = professionals;
    }

    @PostMapping("/professionals")
    ResponseEntity<ProfessionalResponse> create(@Valid @RequestBody ProfessionalRequest r) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ProfessionalResponse.from(professionals.create(
                new NewProfessionalCommand(r.firstName(), r.lastName(), r.documentType(), r.documentNumber(), r.email(),
                        r.phone(), r.password(), r.professionalCode(), r.licenseNumber()))));
    }

    @GetMapping("/professionals")
    List<ProfessionalResponse> list() {
        return professionals.list().stream().map(ProfessionalResponse::from).toList();
    }

    @PutMapping("/professionals/{id}/capabilities")
    ProfessionalResponse configure(@PathVariable long id, @Valid @RequestBody CapabilitiesRequest r) {
        return ProfessionalResponse.from(professionals.configure(id,
                new CapabilitiesCommand(r.specialtyIds(), r.primarySpecialtyId(), r.locationIds(), r.active())));
    }

    @GetMapping("/locations")
    List<LocationResponse> locations() {
        return professionals.locations().stream().map(LocationResponse::from).toList();
    }

    record CapabilitiesRequest(@NotNull @Size(min = 1) List<@NotNull Long> specialtyIds,
                               @NotNull Long primarySpecialtyId,
                               @NotNull @Size(min = 1) List<@NotNull Long> locationIds,
                               @NotNull Boolean active) {
    }

    record LocationResponse(long id, String code, String name, String address, String city, String department,
                            boolean active) {
        static LocationResponse from(Location l) {
            return new LocationResponse(l.id(), l.code(), l.name(), l.address(), l.city(), l.department(), l.active());
        }
    }

    record ProfessionalRequest(@NotBlank @Size(max = 100) String firstName,
                               @NotBlank @Size(max = 100) String lastName,
                               @NotBlank @Size(max = 32) String documentType,
                               @NotBlank @Size(max = 64) String documentNumber,
                               @NotBlank @Email @Size(max = 254) String email,
                               @NotBlank @Size(max = 32) String phone,
                               @NotBlank @StrongPassword String password,
                               @NotBlank @Size(max = 50) String professionalCode,
                               @NotBlank @Size(max = 80) String licenseNumber) {
        @Override
        public String toString() {
            return "ProfessionalRequest[email=" + email + ", password=***]";
        }
    }

    record ProfessionalResponse(long id, long userId, String firstName, String lastName, String email, String phone,
                                String professionalCode, String licenseNumber, boolean active,
                                List<Long> specialtyIds, Long primarySpecialtyId, List<Long> locationIds,
                                List<String> specialtyCodes, List<String> locationCodes) {
        static ProfessionalResponse from(ProfessionalSummary p) {
            return new ProfessionalResponse(p.id(), p.userId(), p.firstName(), p.lastName(), p.email(), p.phone(),
                    p.professionalCode(), p.licenseNumber(), p.active(),
                    p.specialties().stream().map(ProfessionalCapabilitySpecialty::id).toList(), p.primarySpecialtyId(),
                    p.locations().stream().map(Location::id).toList(),
                    p.specialties().stream().map(ProfessionalCapabilitySpecialty::code).toList(),
                    p.locations().stream().map(Location::code).toList());
        }
    }
}
