package com.fcv.citas.application.service;

import com.fcv.citas.application.exception.BusinessRuleException;
import com.fcv.citas.application.exception.NotFoundException;
import com.fcv.citas.application.port.out.AffiliationRepositoryPort;
import com.fcv.citas.domain.model.Affiliation;
import com.fcv.citas.domain.model.Eps;
import com.fcv.citas.domain.model.EpsPlan;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AffiliationServiceTest {
    // 2026-10-03T02:00Z es todavía 2026-10-02 en Bogotá: la fecha de vigencia usa la zona del Clock.
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-03T02:00:00Z"), ZoneId.of("America/Bogota"));
    private final InsuranceCatalogServiceTest.InMemoryInsurance catalog = new InsuranceCatalogServiceTest.InMemoryInsurance();
    private final InMemoryAffiliations affiliations = new InMemoryAffiliations();
    private AffiliationService service;
    private EpsPlan planA;
    private EpsPlan planB;

    @BeforeEach
    void setUp() {
        Eps eps = catalog.insertEps("EPS_A", "EPS A", CLOCK.instant());
        planA = catalog.insertPlan(eps.id(), 1L, "A", "Plan A");
        planB = catalog.insertPlan(eps.id(), 1L, "B", "Plan B");
        service = new AffiliationService(affiliations, catalog, new DirectTransactions(), CLOCK);
    }

    @Test
    void savingMakesItTheOnlyCurrentAffiliation() {
        service.save(7L, planA.id(), " 123 ");
        Affiliation current = service.save(7L, planB.id(), "456");

        assertThat(current.plan().id()).isEqualTo(planB.id());
        assertThat(current.membershipNumber()).isEqualTo("456");
        assertThat(affiliations.rows).filteredOn(r -> r.current).hasSize(1);
        assertThat(affiliations.rows.getFirst().validTo).isEqualTo(LocalDate.of(2026, 10, 2));
        assertThat(service.current(7L)).extracting(a -> a.plan().id()).containsExactly(planB.id());
    }

    @Test
    void reselectingAPreviousCombinationReactivatesTheRowInsteadOfDuplicating() {
        service.save(7L, planA.id(), "123");
        service.save(7L, planB.id(), "456");

        Affiliation again = service.save(7L, planA.id(), "123");

        assertThat(affiliations.rows).hasSize(2);
        assertThat(again.id()).isEqualTo(affiliations.rows.getFirst().id);
        assertThat(affiliations.rows.getFirst().current).isTrue();
        assertThat(affiliations.rows.getFirst().validTo).isNull();
    }

    @Test
    void inactivePlanOrEpsIsRejectedKeepingThePreviousAffiliation() {
        service.save(7L, planA.id(), "123");
        catalog.updatePlan(planB.id(), "Plan B", false);

        assertThatThrownBy(() -> service.save(7L, planB.id(), "456"))
                .isInstanceOf(BusinessRuleException.class).extracting("code").isEqualTo("CATALOG_INACTIVE");
        catalog.updatePlan(planB.id(), "Plan B", true);
        catalog.updateEps(planB.epsId(), "EPS A", false, CLOCK.instant());
        assertThatThrownBy(() -> service.save(7L, planB.id(), "456"))
                .isInstanceOf(BusinessRuleException.class).extracting("code").isEqualTo("CATALOG_INACTIVE");

        assertThat(service.current(7L)).extracting(a -> a.plan().id()).containsExactly(planA.id());
    }

    @Test
    void unknownPlanIsNotFoundAndNoAffiliationIsEmpty() {
        assertThat(service.current(7L)).isEmpty();
        assertThatThrownBy(() -> service.save(7L, 999L, "1")).isInstanceOf(NotFoundException.class);
    }

    private final class InMemoryAffiliations implements AffiliationRepositoryPort {
        final List<Row> rows = new ArrayList<>();
        private long next = 1;

        @Override public void lockOwner(long userId) { }
        @Override public List<Affiliation> findCurrent(long userId) {
            return rows.stream().filter(r -> r.userId == userId && r.current)
                    .map(r -> new Affiliation(r.id, r.membership, true, catalog.findPlan(r.planId).orElseThrow())).toList();
        }
        @Override public Optional<Long> findId(long userId, long planId, String membershipNumber) {
            return rows.stream().filter(r -> r.userId == userId && r.planId == planId && r.membership.equals(membershipNumber))
                    .map(r -> r.id).findFirst();
        }
        @Override public void closeCurrent(long userId, LocalDate validTo) {
            rows.stream().filter(r -> r.userId == userId && r.current).forEach(r -> { r.current = false; r.validTo = validTo; });
        }
        @Override public void reactivate(long id, LocalDate validFrom) {
            rows.stream().filter(r -> r.id == id).forEach(r -> { r.current = true; r.validTo = null; r.validFrom = validFrom; });
        }
        @Override public void insert(long userId, long planId, String membershipNumber, LocalDate validFrom, Instant at) {
            Row row = new Row();
            row.id = next++; row.userId = userId; row.planId = planId; row.membership = membershipNumber;
            row.current = true; row.validFrom = validFrom;
            rows.add(row);
        }
    }

    private static final class Row {
        long id;
        long userId;
        long planId;
        String membership;
        boolean current;
        LocalDate validFrom;
        LocalDate validTo;
    }
}
