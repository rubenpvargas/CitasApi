package com.fcv.citas.adapter.in.rest;

import com.fcv.citas.application.port.in.InsuranceCatalogUseCase;
import com.fcv.citas.domain.model.Eps;
import com.fcv.citas.domain.model.EpsPlan;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** HU-008 — gestión ADMIN de EPS y planes; sin borrado físico (retirar = active=false). */
@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class InsuranceAdminController {
    static final String CODE_PATTERN = "^[A-Za-z0-9_-]+$";

    private final InsuranceCatalogUseCase catalog;

    public InsuranceAdminController(InsuranceCatalogUseCase catalog) {
        this.catalog = catalog;
    }

    @GetMapping("/eps")
    List<EpsResponse> eps() {
        return catalog.listEps().stream().map(EpsResponse::from).toList();
    }

    @PostMapping("/eps")
    ResponseEntity<EpsResponse> createEps(@Valid @RequestBody EpsRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(EpsResponse.from(catalog.createEps(request.code(), request.name())));
    }

    @PatchMapping("/eps/{id}")
    EpsResponse updateEps(@PathVariable long id, @Valid @RequestBody CatalogUpdateRequest request) {
        return EpsResponse.from(catalog.updateEps(id, request.name(), request.active()));
    }

    @GetMapping("/plans")
    List<PlanResponse> plans(@RequestParam(required = false) Long epsId) {
        return catalog.listPlans(epsId).stream().map(PlanResponse::from).toList();
    }

    @PostMapping("/eps/{epsId}/plans")
    ResponseEntity<PlanResponse> createPlan(@PathVariable long epsId, @Valid @RequestBody PlanRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(PlanResponse.from(
                catalog.createPlan(epsId, request.regimeId(), request.code(), request.name())));
    }

    @PatchMapping("/plans/{id}")
    PlanResponse updatePlan(@PathVariable long id, @Valid @RequestBody CatalogUpdateRequest request) {
        return PlanResponse.from(catalog.updatePlan(id, request.name(), request.active()));
    }

    record EpsRequest(@NotBlank @Size(max = 40) @Pattern(regexp = CODE_PATTERN) String code,
                      @NotBlank @Size(max = 160) String name) {
    }

    record PlanRequest(@NotNull Long regimeId,
                       @NotBlank @Size(max = 50) @Pattern(regexp = CODE_PATTERN) String code,
                       @NotBlank @Size(max = 160) String name) {
    }

    record CatalogUpdateRequest(@NotBlank @Size(max = 160) String name, @NotNull Boolean active) {
    }

    record EpsResponse(Long id, String code, String name, boolean active) {
        static EpsResponse from(Eps eps) {
            return new EpsResponse(eps.id(), eps.code(), eps.name(), eps.active());
        }
    }

    record PlanResponse(Long id, String code, String name, boolean active, long epsId, String epsCode,
                        String epsName, Long regimeId, String regimeCode, String regimeName) {
        static PlanResponse from(EpsPlan plan) {
            return new PlanResponse(plan.id(), plan.code(), plan.name(), plan.active(), plan.epsId(), plan.epsCode(),
                    plan.epsName(), plan.regime().id(), plan.regime().code(), plan.regime().name());
        }
    }
}
