package com.fcv.citas.application.port.in;

import com.fcv.citas.application.model.EpsWithPlans;
import com.fcv.citas.domain.model.Eps;
import com.fcv.citas.domain.model.EpsPlan;

import java.util.List;

/** HU-008 — gestión ADMIN de EPS/planes y lectura de catálogo activo para USER. */
public interface InsuranceCatalogUseCase {
    List<Eps> listEps();

    Eps createEps(String code, String name);

    Eps updateEps(long id, String name, boolean active);

    List<EpsPlan> listPlans(Long epsId);

    EpsPlan createPlan(long epsId, long regimeId, String code, String name);

    EpsPlan updatePlan(long id, String name, boolean active);

    /** EPS activas con sus planes activos. */
    List<EpsWithPlans> activeCatalog();
}
