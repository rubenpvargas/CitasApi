package com.fcv.citas.application.service;

import com.fcv.citas.application.exception.BusinessRuleException;
import com.fcv.citas.application.exception.NotFoundException;
import com.fcv.citas.application.port.in.AffiliationUseCase;
import com.fcv.citas.application.port.out.AffiliationRepositoryPort;
import com.fcv.citas.application.port.out.InsuranceCatalogRepositoryPort;
import com.fcv.citas.application.port.out.TransactionPort;
import com.fcv.citas.domain.model.Affiliation;
import com.fcv.citas.domain.model.EpsPlan;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/**
 * HU-006 — una sola afiliación vigente por usuario. Reelegir una combinación previa (plan + número)
 * reactiva esa fila en lugar de violar la unicidad; todo ocurre en una transacción, de modo que un
 * rechazo conserva la afiliación previa.
 */
public final class AffiliationService implements AffiliationUseCase {
    private final AffiliationRepositoryPort affiliations;
    private final InsuranceCatalogRepositoryPort catalog;
    private final TransactionPort transactions;
    private final Clock clock;

    public AffiliationService(AffiliationRepositoryPort affiliations, InsuranceCatalogRepositoryPort catalog,
                              TransactionPort transactions, Clock clock) {
        this.affiliations = affiliations;
        this.catalog = catalog;
        this.transactions = transactions;
        this.clock = clock;
    }

    @Override
    public List<Affiliation> current(long userId) {
        return affiliations.findCurrent(userId);
    }

    @Override
    public Affiliation save(long userId, long planId, String membershipNumber) {
        String membership = membershipNumber.trim();
        return transactions.required(() -> {
            EpsPlan plan = catalog.findPlan(planId).orElseThrow(() -> new NotFoundException("Plan not found"));
            if (!plan.selectable()) {
                throw new BusinessRuleException("CATALOG_INACTIVE", "The selected plan or EPS is inactive");
            }
            affiliations.lockOwner(userId);
            LocalDate today = LocalDate.now(clock);
            affiliations.closeCurrent(userId, today);
            affiliations.findId(userId, planId, membership).ifPresentOrElse(
                    id -> affiliations.reactivate(id, today),
                    () -> affiliations.insert(userId, planId, membership, today, clock.instant()));
            return affiliations.findCurrent(userId).getFirst();
        });
    }
}
