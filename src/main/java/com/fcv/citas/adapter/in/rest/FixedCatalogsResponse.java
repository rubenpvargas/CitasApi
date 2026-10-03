package com.fcv.citas.adapter.in.rest;

import com.fcv.citas.domain.model.CatalogEntry;
import com.fcv.citas.domain.model.CatalogLocation;
import com.fcv.citas.domain.model.CatalogStatus;
import com.fcv.citas.domain.model.FixedCatalogs;
import com.fcv.citas.domain.model.InsuranceRegime;

import java.util.List;

public record FixedCatalogsResponse(List<EntryResponse> roles,
                                    List<StatusResponse> appointmentStatuses,
                                    List<StatusResponse> rescheduleRequestStatuses,
                                    List<RegimeResponse> insuranceRegimes,
                                    List<LocationResponse> locations) {
    static FixedCatalogsResponse from(FixedCatalogs catalogs) {
        return new FixedCatalogsResponse(
                catalogs.roles().stream().map(EntryResponse::from).toList(),
                catalogs.appointmentStatuses().stream().map(StatusResponse::from).toList(),
                catalogs.rescheduleRequestStatuses().stream().map(StatusResponse::from).toList(),
                catalogs.insuranceRegimes().stream().map(RegimeResponse::from).toList(),
                catalogs.locations().stream().map(LocationResponse::from).toList());
    }

    record EntryResponse(String code, String name) {
        static EntryResponse from(CatalogEntry entry) {
            return new EntryResponse(entry.code(), entry.name());
        }
    }

    /** Incluye id (aditivo) para que el cliente envíe regimeId al crear planes. */
    record RegimeResponse(Long id, String code, String name) {
        static RegimeResponse from(InsuranceRegime regime) {
            return new RegimeResponse(regime.id(), regime.code(), regime.name());
        }
    }

    record StatusResponse(String code, String name, boolean terminal) {
        static StatusResponse from(CatalogStatus status) {
            return new StatusResponse(status.code(), status.name(), status.terminal());
        }
    }

    record LocationResponse(String code, String name, String address, String city,
                            String department, boolean active) {
        static LocationResponse from(CatalogLocation location) {
            return new LocationResponse(location.code(), location.name(), location.address(), location.city(),
                    location.department(), location.active());
        }
    }
}
