package com.fcv.citas.domain.model;

import java.util.List;

public record FixedCatalogs(List<CatalogEntry> roles,
                            List<CatalogStatus> appointmentStatuses,
                            List<CatalogStatus> rescheduleRequestStatuses,
                            List<InsuranceRegime> insuranceRegimes,
                            List<CatalogLocation> locations) {
    public FixedCatalogs {
        roles = List.copyOf(roles);
        appointmentStatuses = List.copyOf(appointmentStatuses);
        rescheduleRequestStatuses = List.copyOf(rescheduleRequestStatuses);
        insuranceRegimes = List.copyOf(insuranceRegimes);
        locations = List.copyOf(locations);
    }
}
