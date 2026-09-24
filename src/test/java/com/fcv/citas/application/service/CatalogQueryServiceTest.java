package com.fcv.citas.application.service;

import com.fcv.citas.application.port.out.CatalogRepositoryPort;
import com.fcv.citas.domain.model.CatalogEntry;
import com.fcv.citas.domain.model.CatalogLocation;
import com.fcv.citas.domain.model.CatalogStatus;
import com.fcv.citas.domain.model.FixedCatalogs;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogQueryServiceTest {
    @Test
    void returnsAllFixedCatalogsWithoutOfferingMutation() {
        FixedCatalogs expected = new FixedCatalogs(
                List.of(new CatalogEntry("USER", "Usuario"), new CatalogEntry("ADMIN", "Administrador")),
                List.of(new CatalogStatus("APPROVED", "Aprobada", false)),
                List.of(new CatalogStatus("PENDING", "Pendiente", false)),
                List.of(new CatalogEntry("CONTRIBUTIVO", "Contributivo")),
                List.of(new CatalogLocation("HIC", "Hospital Internacional de Colombia (HIC)",
                        "Direccion publica", "Piedecuesta", "Santander", true)));
        CatalogRepositoryPort repository = () -> expected;

        FixedCatalogs actual = new CatalogQueryService(repository).getFixedCatalogs();

        assertThat(actual).isEqualTo(expected);
        assertThat(actual.roles()).extracting(CatalogEntry::code).contains("USER", "ADMIN");
        assertThat(actual.locations()).extracting(CatalogLocation::code).containsExactly("HIC");
    }
}
