package com.fcv.citas.adapter.in.rest;

import com.fcv.citas.application.port.in.CatalogQueryUseCase;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/catalogs")
public class CatalogController {
    private final CatalogQueryUseCase catalogs;

    public CatalogController(CatalogQueryUseCase catalogs) {
        this.catalogs = catalogs;
    }

    @GetMapping
    FixedCatalogsResponse getFixedCatalogs() {
        return FixedCatalogsResponse.from(catalogs.getFixedCatalogs());
    }
}
