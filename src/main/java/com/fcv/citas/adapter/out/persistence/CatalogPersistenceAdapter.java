package com.fcv.citas.adapter.out.persistence;

import com.fcv.citas.application.port.out.CatalogRepositoryPort;
import com.fcv.citas.domain.model.CatalogEntry;
import com.fcv.citas.domain.model.CatalogLocation;
import com.fcv.citas.domain.model.CatalogStatus;
import com.fcv.citas.domain.model.FixedCatalogs;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class CatalogPersistenceAdapter implements CatalogRepositoryPort {
    private final JdbcTemplate jdbc;

    public CatalogPersistenceAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public FixedCatalogs getFixedCatalogs() {
        return new FixedCatalogs(
                jdbc.query("SELECT code, name FROM roles ORDER BY id", (rs, rowNum) ->
                        new CatalogEntry(rs.getString("code"), rs.getString("name"))),
                jdbc.query("SELECT code, name, is_terminal FROM appointment_statuses ORDER BY id", (rs, rowNum) ->
                        new CatalogStatus(rs.getString("code"), rs.getString("name"), rs.getBoolean("is_terminal"))),
                jdbc.query("SELECT code, name, is_terminal FROM reschedule_request_statuses ORDER BY id", (rs, rowNum) ->
                        new CatalogStatus(rs.getString("code"), rs.getString("name"), rs.getBoolean("is_terminal"))),
                jdbc.query("SELECT code, name FROM insurance_regimes ORDER BY id", (rs, rowNum) ->
                        new CatalogEntry(rs.getString("code"), rs.getString("name"))),
                jdbc.query("SELECT code, name, address, city, department, active FROM locations ORDER BY id", (rs, rowNum) ->
                        new CatalogLocation(rs.getString("code"), rs.getString("name"), rs.getString("address"),
                                rs.getString("city"), rs.getString("department"), rs.getBoolean("active")))
        );
    }
}
