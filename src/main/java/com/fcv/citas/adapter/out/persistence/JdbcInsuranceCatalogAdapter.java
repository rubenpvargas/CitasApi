package com.fcv.citas.adapter.out.persistence;

import com.fcv.citas.application.exception.BusinessRuleException;
import com.fcv.citas.application.port.out.InsuranceCatalogRepositoryPort;
import com.fcv.citas.domain.model.Eps;
import com.fcv.citas.domain.model.EpsPlan;
import com.fcv.citas.domain.model.InsuranceRegime;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Component;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
public class JdbcInsuranceCatalogAdapter implements InsuranceCatalogRepositoryPort {
    private static final String PLAN_SELECT = "SELECT p.id, p.eps_id, e.code eps_code, e.name eps_name, e.active eps_active, "
            + "p.code, p.name, p.active, r.id regime_id, r.code regime_code, r.name regime_name "
            + "FROM eps_plans p JOIN eps e ON e.id = p.eps_id JOIN insurance_regimes r ON r.id = p.regime_id";
    private static final RowMapper<Eps> EPS = (rs, n) ->
            new Eps(rs.getLong("id"), rs.getString("code"), rs.getString("name"), rs.getBoolean("active"));
    private static final RowMapper<EpsPlan> PLAN = (rs, n) -> new EpsPlan(rs.getLong("id"), rs.getLong("eps_id"),
            rs.getString("eps_code"), rs.getString("eps_name"), rs.getString("code"), rs.getString("name"),
            rs.getBoolean("active"), new InsuranceRegime(rs.getLong("regime_id"), rs.getString("regime_code"),
            rs.getString("regime_name")), rs.getBoolean("eps_active"));

    private final JdbcTemplate jdbc;

    public JdbcInsuranceCatalogAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<Eps> findAllEps() {
        return jdbc.query("SELECT id, code, name, active FROM eps ORDER BY name, id", EPS);
    }

    @Override
    public Optional<Eps> findEps(long id) {
        return jdbc.query("SELECT id, code, name, active FROM eps WHERE id = ?", EPS, id).stream().findFirst();
    }

    @Override
    public boolean epsCodeExists(String code) {
        return count("SELECT COUNT(*) FROM eps WHERE code = ?", code) > 0;
    }

    @Override
    public Eps insertEps(String code, String name, Instant at) {
        var keys = new GeneratedKeyHolder();
        try {
            jdbc.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO eps(code, name, active, created_at, updated_at) VALUES (?, ?, TRUE, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, code);
                ps.setString(2, name);
                ps.setTimestamp(3, Timestamp.from(at));
                ps.setTimestamp(4, Timestamp.from(at));
                return ps;
            }, keys);
        } catch (DuplicateKeyException exception) {
            throw duplicate();
        }
        return findEps(keys.getKey().longValue()).orElseThrow();
    }

    @Override
    public void updateEps(long id, String name, boolean active, Instant at) {
        jdbc.update("UPDATE eps SET name = ?, active = ?, updated_at = ? WHERE id = ?", name, active, Timestamp.from(at), id);
    }

    @Override
    public List<EpsPlan> findPlans(Long epsId) {
        return epsId == null
                ? jdbc.query(PLAN_SELECT + " ORDER BY e.name, p.name, p.id", PLAN)
                : jdbc.query(PLAN_SELECT + " WHERE p.eps_id = ? ORDER BY p.name, p.id", PLAN, epsId);
    }

    @Override
    public Optional<EpsPlan> findPlan(long id) {
        return jdbc.query(PLAN_SELECT + " WHERE p.id = ?", PLAN, id).stream().findFirst();
    }

    @Override
    public boolean planCodeExists(long epsId, String code) {
        return count("SELECT COUNT(*) FROM eps_plans WHERE eps_id = ? AND code = ?", epsId, code) > 0;
    }

    @Override
    public Optional<InsuranceRegime> findRegime(long id) {
        return jdbc.query("SELECT id, code, name FROM insurance_regimes WHERE id = ?",
                (rs, n) -> new InsuranceRegime(rs.getLong("id"), rs.getString("code"), rs.getString("name")), id)
                .stream().findFirst();
    }

    @Override
    public EpsPlan insertPlan(long epsId, long regimeId, String code, String name) {
        var keys = new GeneratedKeyHolder();
        try {
            jdbc.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO eps_plans(eps_id, regime_id, code, name, active) VALUES (?, ?, ?, ?, TRUE)",
                        Statement.RETURN_GENERATED_KEYS);
                ps.setLong(1, epsId);
                ps.setLong(2, regimeId);
                ps.setString(3, code);
                ps.setString(4, name);
                return ps;
            }, keys);
        } catch (DuplicateKeyException exception) {
            throw duplicate();
        }
        return findPlan(keys.getKey().longValue()).orElseThrow();
    }

    @Override
    public void updatePlan(long id, String name, boolean active) {
        jdbc.update("UPDATE eps_plans SET name = ?, active = ? WHERE id = ?", name, active, id);
    }

    private int count(String sql, Object... args) {
        Integer value = jdbc.queryForObject(sql, Integer.class, args);
        return value == null ? 0 : value;
    }

    private static BusinessRuleException duplicate() {
        return new BusinessRuleException("DUPLICATE_CODE", "A catalog entry with this code already exists");
    }
}
