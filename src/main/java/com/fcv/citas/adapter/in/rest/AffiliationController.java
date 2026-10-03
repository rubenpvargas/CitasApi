package com.fcv.citas.adapter.in.rest;

import com.fcv.citas.application.port.in.AffiliationUseCase;
import com.fcv.citas.application.port.in.InsuranceCatalogUseCase;
import com.fcv.citas.domain.model.Affiliation;
import com.fcv.citas.domain.model.EpsPlan;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** HU-006 — afiliación del USER autenticado y catálogo activo de EPS/planes para seleccionarla. */
@RestController
@RequestMapping("/api/v1")
public class AffiliationController {
    private final AffiliationUseCase affiliations;
    private final InsuranceCatalogUseCase catalog;

    public AffiliationController(AffiliationUseCase affiliations, InsuranceCatalogUseCase catalog) {
        this.affiliations = affiliations;
        this.catalog = catalog;
    }

    @GetMapping("/me/affiliations")
    @PreAuthorize("hasRole('USER')")
    List<AffiliationResponse> current(@AuthenticationPrincipal Jwt jwt) {
        return affiliations.current(JwtSubject.userId(jwt)).stream().map(AffiliationResponse::from).toList();
    }

    @PutMapping("/me/affiliations")
    @PreAuthorize("hasRole('USER')")
    AffiliationResponse save(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody AffiliationRequest request) {
        return AffiliationResponse.from(affiliations.save(JwtSubject.userId(jwt), request.planId(), request.membershipNumber()));
    }

    @GetMapping("/insurance/eps")
    List<InsuranceEpsResponse> activeInsurance() {
        return catalog.activeCatalog().stream().map(entry -> new InsuranceEpsResponse(entry.eps().id(),
                entry.eps().code(), entry.eps().name(), entry.plans().stream().map(InsurancePlanResponse::from).toList()))
                .toList();
    }

    record AffiliationRequest(@NotNull Long planId, @NotBlank @Size(max = 80) String membershipNumber) {
    }

    record AffiliationResponse(Long id, String membershipNumber, boolean current, Long planId, String planCode,
                               String planName, long epsId, String epsCode, String epsName, String regimeCode,
                               String regimeName) {
        static AffiliationResponse from(Affiliation a) {
            EpsPlan p = a.plan();
            return new AffiliationResponse(a.id(), a.membershipNumber(), a.current(), p.id(), p.code(), p.name(),
                    p.epsId(), p.epsCode(), p.epsName(), p.regime().code(), p.regime().name());
        }
    }

    record RegimeResponse(String code, String name) {
    }

    record InsurancePlanResponse(Long id, String code, String name, RegimeResponse regime) {
        static InsurancePlanResponse from(EpsPlan p) {
            return new InsurancePlanResponse(p.id(), p.code(), p.name(), new RegimeResponse(p.regime().code(), p.regime().name()));
        }
    }

    record InsuranceEpsResponse(Long id, String code, String name, List<InsurancePlanResponse> plans) {
    }
}
