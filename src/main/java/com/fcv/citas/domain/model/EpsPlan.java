package com.fcv.citas.domain.model;

/** Plan de una EPS; depende funcionalmente de su EPS y referencia un régimen del catálogo fijo. */
public record EpsPlan(Long id, long epsId, String epsCode, String epsName, String code, String name,
                      boolean active, InsuranceRegime regime, boolean epsActive) {
    /** Un plan es seleccionable solo si él y su EPS están activos. */
    public boolean selectable() {
        return active && epsActive;
    }
}
