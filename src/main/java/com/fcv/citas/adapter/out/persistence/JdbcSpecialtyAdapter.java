package com.fcv.citas.adapter.out.persistence;

import com.fcv.citas.application.exception.BusinessRuleException;
import com.fcv.citas.application.port.out.SpecialtyRepositoryPort;
import com.fcv.citas.domain.model.Specialty;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Component;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

@Component
public class JdbcSpecialtyAdapter implements SpecialtyRepositoryPort {
    private static final String SELECT = "SELECT id, code, name, appointment_duration_minutes, is_general, active FROM specialties";
    private static final RowMapper<Specialty> ROW = (rs, n) -> new Specialty(rs.getLong("id"), rs.getString("code"),
            rs.getString("name"), rs.getInt("appointment_duration_minutes"), rs.getBoolean("is_general"),
            rs.getBoolean("active"));

    private final JdbcTemplate jdbc;

    public JdbcSpecialtyAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<Specialty> findAll() {
        return jdbc.query(SELECT + " ORDER BY name, id", ROW);
    }

    @Override
    public Optional<Specialty> findById(long id) {
        return jdbc.query(SELECT + " WHERE id = ?", ROW, id).stream().findFirst();
    }

    @Override
    public boolean codeExists(String code) {
        return count("SELECT COUNT(*) FROM specialties WHERE code = ?", code) > 0;
    }

    @Override
    public boolean nameExists(String name, Long excludingId) {
        return count("SELECT COUNT(*) FROM specialties WHERE name = ? AND id <> ?", name,
                excludingId == null ? -1L : excludingId) > 0;
    }

    @Override
    public boolean activeGeneralExists(Long excludingId) {
        // FOR UPDATE serializa altas/activaciones concurrentes de la especialidad general.
        return !jdbc.queryForList("SELECT id FROM specialties WHERE is_general = TRUE AND active = TRUE AND id <> ? FOR UPDATE",
                Long.class, excludingId == null ? -1L : excludingId).isEmpty();
    }

    @Override
    public Specialty insert(String code, String name, int durationMinutes, boolean general) {
        var keys = new GeneratedKeyHolder();
        try {
            jdbc.update(connection -> {
                PreparedStatement ps = connection.prepareStatement("INSERT INTO specialties(code, name, "
                                + "appointment_duration_minutes, is_general, requires_admin_approval, active) VALUES (?, ?, ?, ?, ?, TRUE)",
                        Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, code);
                ps.setString(2, name);
                ps.setInt(3, durationMinutes);
                ps.setBoolean(4, general);
                ps.setBoolean(5, !general);
                return ps;
            }, keys);
        } catch (DuplicateKeyException exception) {
            throw new BusinessRuleException("DUPLICATE_CODE", "A specialty with this code or name already exists");
        }
        return findById(keys.getKey().longValue()).orElseThrow();
    }

    @Override
    public void update(long id, String name, int durationMinutes, boolean active) {
        jdbc.update("UPDATE specialties SET name = ?, appointment_duration_minutes = ?, active = ? WHERE id = ?",
                name, durationMinutes, active, id);
    }

    private int count(String sql, Object... args) {
        Integer value = jdbc.queryForObject(sql, Integer.class, args);
        return value == null ? 0 : value;
    }
}
