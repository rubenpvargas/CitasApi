package com.fcv.citas.application.service;

import com.fcv.citas.application.port.in.CatalogQueryUseCase;
import com.fcv.citas.application.port.out.CatalogRepositoryPort;
import com.fcv.citas.domain.model.FixedCatalogs;

public class CatalogQueryService implements CatalogQueryUseCase {
    private final CatalogRepositoryPort catalogs;

    public CatalogQueryService(CatalogRepositoryPort catalogs) {
        this.catalogs = catalogs;
    }

    @Override
    public FixedCatalogs getFixedCatalogs() {
        return catalogs.getFixedCatalogs();
    }
}
