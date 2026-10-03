package com.fcv.citas.application.service;

import com.fcv.citas.application.exception.BusinessRuleException;
import com.fcv.citas.application.exception.NotFoundException;
import com.fcv.citas.application.port.out.InsuranceCatalogRepositoryPort;
import com.fcv.citas.domain.model.Eps;
import com.fcv.citas.domain.model.EpsPlan;
import com.fcv.citas.domain.model.InsuranceRegime;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InsuranceCatalogServiceTest {
    private final InMemoryInsurance repo = new InMemoryInsurance();
    private final InsuranceCatalogService service = new InsuranceCatalogService(repo, new DirectTransactions(),
            Clock.fixed(Instant.parse("2026-10-02T15:00:00Z"), ZoneId.of("America/Bogota")));

    @Test
    void createsEpsWithNormalizedCodeAndRejectsDuplicateCode() {
        Eps created = service.createEps(" eps_nueva ", " EPS Nueva ");

        assertThat(created.code()).isEqualTo("EPS_NUEVA");
        assertThat(created.name()).isEqualTo("EPS Nueva");
        assertThat(created.active()).isTrue();
        assertThatThrownBy(() -> service.createEps("EPS_NUEVA", "Otra"))
                .isInstanceOf(BusinessRuleException.class).extracting("code").isEqualTo("DUPLICATE_CODE");
    }

    @Test
    void retiringEpsIsLogicalNeverPhysical() {
        Eps eps = service.createEps("EPS_X", "EPS X");

        Eps retired = service.updateEps(eps.id(), "EPS X", false);

        assertThat(retired.active()).isFalse();
        assertThat(service.listEps()).extracting(Eps::id).contains(eps.id());
    }

    @Test
    void planDependsOnExistingEpsAndRegimeAndCodeIsUniquePerEps() {
        Eps a = service.createEps("EPS_A", "EPS A");
        Eps b = service.createEps("EPS_B", "EPS B");

        EpsPlan plan = service.createPlan(a.id(), 1L, "plan-1", "Plan 1");

        assertThat(plan.epsId()).isEqualTo(a.id());
        assertThat(plan.code()).isEqualTo("PLAN-1");
        assertThat(plan.regime().code()).isEqualTo("CONTRIBUTIVO");
        assertThat(service.createPlan(b.id(), 1L, "PLAN-1", "Plan 1 en B").epsId()).isEqualTo(b.id());
        assertThatThrownBy(() -> service.createPlan(a.id(), 1L, "PLAN-1", "Repetido"))
                .isInstanceOf(BusinessRuleException.class).extracting("code").isEqualTo("DUPLICATE_CODE");
        assertThatThrownBy(() -> service.createPlan(999L, 1L, "P", "P")).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.createPlan(a.id(), 99L, "P2", "P")).isInstanceOf(NotFoundException.class);
    }

    @Test
    void updatingUnknownEpsOrPlanIsNotFound() {
        assertThatThrownBy(() -> service.updateEps(404L, "x", true)).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.updatePlan(404L, "x", true)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void activeCatalogOnlyListsActiveEpsWithActivePlans() {
        Eps active = service.createEps("EPS_ON", "On");
        Eps inactive = service.createEps("EPS_OFF", "Off");
        EpsPlan on = service.createPlan(active.id(), 1L, "ON1", "Plan on");
        EpsPlan off = service.createPlan(active.id(), 1L, "OFF1", "Plan off");
        service.createPlan(inactive.id(), 1L, "X1", "Plan of inactive eps");
        service.updatePlan(off.id(), "Plan off", false);
        service.updateEps(inactive.id(), "Off", false);

        var catalog = service.activeCatalog();

        assertThat(catalog).hasSize(1);
        assertThat(catalog.getFirst().eps().id()).isEqualTo(active.id());
        assertThat(catalog.getFirst().plans()).extracting(EpsPlan::id).containsExactly(on.id());
    }

    static final class InMemoryInsurance implements InsuranceCatalogRepositoryPort {
        private final Map<Long, Eps> eps = new LinkedHashMap<>();
        private final Map<Long, EpsPlan> plans = new LinkedHashMap<>();
        private final Map<Long, InsuranceRegime> regimes = Map.of(1L, new InsuranceRegime(1L, "CONTRIBUTIVO", "Contributivo"));
        private long next = 1;

        @Override public List<Eps> findAllEps() { return new ArrayList<>(eps.values()); }
        @Override public Optional<Eps> findEps(long id) { return Optional.ofNullable(eps.get(id)); }
        @Override public boolean epsCodeExists(String code) { return eps.values().stream().anyMatch(e -> e.code().equals(code)); }
        @Override public Eps insertEps(String code, String name, Instant at) {
            Eps e = new Eps(next++, code, name, true); eps.put(e.id(), e); return e;
        }
        @Override public void updateEps(long id, String name, boolean active, Instant at) {
            Eps e = eps.get(id); eps.put(id, new Eps(id, e.code(), name, active));
        }
        @Override public List<EpsPlan> findPlans(Long epsId) {
            return plans.values().stream().filter(p -> epsId == null || p.epsId() == epsId)
                    .map(this::refresh).sorted(Comparator.comparing(EpsPlan::id)).toList();
        }
        @Override public Optional<EpsPlan> findPlan(long id) { return Optional.ofNullable(plans.get(id)).map(this::refresh); }
        @Override public boolean planCodeExists(long epsId, String code) {
            return plans.values().stream().anyMatch(p -> p.epsId() == epsId && p.code().equals(code));
        }
        @Override public Optional<InsuranceRegime> findRegime(long id) { return Optional.ofNullable(regimes.get(id)); }
        @Override public EpsPlan insertPlan(long epsId, long regimeId, String code, String name) {
            Eps e = eps.get(epsId);
            EpsPlan p = new EpsPlan(next++, epsId, e.code(), e.name(), code, name, true, regimes.get(regimeId), e.active());
            plans.put(p.id(), p); return p;
        }
        @Override public void updatePlan(long id, String name, boolean active) {
            EpsPlan p = plans.get(id);
            plans.put(id, new EpsPlan(id, p.epsId(), p.epsCode(), p.epsName(), p.code(), name, active, p.regime(), p.epsActive()));
        }
        private EpsPlan refresh(EpsPlan p) {
            Eps e = eps.get(p.epsId());
            return new EpsPlan(p.id(), p.epsId(), e.code(), e.name(), p.code(), p.name(), p.active(), p.regime(), e.active());
        }
    }
}
