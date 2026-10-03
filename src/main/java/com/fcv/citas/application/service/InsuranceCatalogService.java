package com.fcv.citas.application.service;

import com.fcv.citas.application.exception.BusinessRuleException;
import com.fcv.citas.application.exception.NotFoundException;
import com.fcv.citas.application.model.EpsWithPlans;
import com.fcv.citas.application.port.in.InsuranceCatalogUseCase;
import com.fcv.citas.application.port.out.InsuranceCatalogRepositoryPort;
import com.fcv.citas.application.port.out.TransactionPort;
import com.fcv.citas.domain.model.Eps;
import com.fcv.citas.domain.model.EpsPlan;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** HU-008 — EPS y planes sin borrado físico: retirar es desactivar. */
public final class InsuranceCatalogService implements InsuranceCatalogUseCase {
    static final String DUPLICATE_CODE = "DUPLICATE_CODE";

    private final InsuranceCatalogRepositoryPort repository;
    private final TransactionPort transactions;
    private final Clock clock;

    public InsuranceCatalogService(InsuranceCatalogRepositoryPort repository, TransactionPort transactions, Clock clock) {
        this.repository = repository;
        this.transactions = transactions;
        this.clock = clock;
    }

    @Override
    public List<Eps> listEps() {
        return repository.findAllEps();
    }

    @Override
    public Eps createEps(String code, String name) {
        String normalized = CatalogCodes.normalize(code);
        return transactions.required(() -> {
            if (repository.epsCodeExists(normalized)) {
                throw duplicate();
            }
            return repository.insertEps(normalized, name.trim(), clock.instant());
        });
    }

    @Override
    public Eps updateEps(long id, String name, boolean active) {
        return transactions.required(() -> {
            repository.findEps(id).orElseThrow(() -> new NotFoundException("EPS not found"));
            repository.updateEps(id, name.trim(), active, clock.instant());
            return repository.findEps(id).orElseThrow();
        });
    }

    @Override
    public List<EpsPlan> listPlans(Long epsId) {
        if (epsId != null) {
            repository.findEps(epsId).orElseThrow(() -> new NotFoundException("EPS not found"));
        }
        return repository.findPlans(epsId);
    }

    @Override
    public EpsPlan createPlan(long epsId, long regimeId, String code, String name) {
        String normalized = CatalogCodes.normalize(code);
        return transactions.required(() -> {
            repository.findEps(epsId).orElseThrow(() -> new NotFoundException("EPS not found"));
            repository.findRegime(regimeId).orElseThrow(() -> new NotFoundException("Insurance regime not found"));
            if (repository.planCodeExists(epsId, normalized)) {
                throw duplicate();
            }
            return repository.insertPlan(epsId, regimeId, normalized, name.trim());
        });
    }

    @Override
    public EpsPlan updatePlan(long id, String name, boolean active) {
        return transactions.required(() -> {
            repository.findPlan(id).orElseThrow(() -> new NotFoundException("Plan not found"));
            repository.updatePlan(id, name.trim(), active);
            return repository.findPlan(id).orElseThrow();
        });
    }

    @Override
    public List<EpsWithPlans> activeCatalog() {
        Map<Long, List<EpsPlan>> plansByEps = repository.findPlans(null).stream()
                .filter(EpsPlan::selectable)
                .collect(Collectors.groupingBy(EpsPlan::epsId));
        return repository.findAllEps().stream()
                .filter(Eps::active)
                .map(eps -> new EpsWithPlans(eps, plansByEps.getOrDefault(eps.id(), List.of())))
                .toList();
    }

    private static BusinessRuleException duplicate() {
        return new BusinessRuleException(DUPLICATE_CODE, "A catalog entry with this code already exists");
    }
}
