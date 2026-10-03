package com.fcv.citas.application.model;

import java.util.List;

/** HU-011 — especialidades (≥1, con una primaria entre ellas), sedes (≥1) y estado operativo. */
public record CapabilitiesCommand(List<Long> specialtyIds, Long primarySpecialtyId, List<Long> locationIds,
                                  boolean active) {
}
