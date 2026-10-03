package com.fcv.citas.application.port.out;

import com.fcv.citas.domain.model.Eps;
import com.fcv.citas.domain.model.EpsPlan;
import com.fcv.citas.domain.model.InsuranceRegime;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface InsuranceCatalogRepositoryPort {
    List<Eps> findAllEps();

    Optional<Eps> findEps(long id);

    boolean epsCodeExists(String code);

    Eps insertEps(String code, String name, Instant at);

    void updateEps(long id, String name, boolean active, Instant at);

    List<EpsPlan> findPlans(Long epsId);

    Optional<EpsPlan> findPlan(long id);

    boolean planCodeExists(long epsId, String code);

    Optional<InsuranceRegime> findRegime(long id);

    EpsPlan insertPlan(long epsId, long regimeId, String code, String name);

    void updatePlan(long id, String name, boolean active);
}
