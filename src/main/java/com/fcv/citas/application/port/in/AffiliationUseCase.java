package com.fcv.citas.application.port.in;

import com.fcv.citas.domain.model.Affiliation;

import java.util.List;

/** HU-006 — afiliación vigente del usuario autenticado. */
public interface AffiliationUseCase {
    /** Afiliación vigente (0 o 1 elemento). */
    List<Affiliation> current(long userId);

    Affiliation save(long userId, long planId, String membershipNumber);
}
