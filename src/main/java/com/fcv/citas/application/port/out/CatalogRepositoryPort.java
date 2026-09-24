package com.fcv.citas.application.port.out;

import com.fcv.citas.domain.model.FixedCatalogs;

public interface CatalogRepositoryPort {
    FixedCatalogs getFixedCatalogs();
}
