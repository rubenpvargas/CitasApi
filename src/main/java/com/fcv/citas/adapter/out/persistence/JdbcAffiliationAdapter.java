package com.fcv.citas.adapter.out.persistence;

import com.fcv.citas.application.port.out.AffiliationRepositoryPort;
import com.fcv.citas.domain.model.Affiliation;
import com.fcv.citas.domain.model.EpsPlan;
import com.fcv.citas.domain.model.InsuranceRegime;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
public class JdbcAffiliationAdapter implements AffiliationRepositoryPort {
    private final JdbcTemplate jdbc;

    public JdbcAffiliationAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void lockOwner(long userId) {
        jdbc.queryForList("SELECT id FROM users WHERE id = ? FOR UPDATE", Long.class, userId);
    }

    @Override
    public List<Affiliation> findCurrent(long userId) {
        return jdbc.query("SELECT a.id, a.membership_number, a.is_current, p.id plan_id, p.code plan_code, "
                        + "p.name plan_name, p.active plan_active, e.id eps_id, e.code eps_code, e.name eps_name, "
                        + "e.active eps_active, r.id regime_id, r.code regime_code, r.name regime_name "
                        + "FROM user_insurance_affiliations a JOIN eps_plans p ON p.id = a.plan_id "
                        + "JOIN eps e ON e.id = p.eps_id JOIN insurance_regimes r ON r.id = p.regime_id "
                        + "WHERE a.user_id = ? AND a.is_current = TRUE ORDER BY a.id DESC",
                (rs, n) -> new Affiliation(rs.getLong("id"), rs.getString("membership_number"), rs.getBoolean("is_current"),
                        new EpsPlan(rs.getLong("plan_id"), rs.getLong("eps_id"), rs.getString("eps_code"),
                                rs.getString("eps_name"), rs.getString("plan_code"), rs.getString("plan_name"),
                                rs.getBoolean("plan_active"), new InsuranceRegime(rs.getLong("regime_id"),
                                rs.getString("regime_code"), rs.getString("regime_name")), rs.getBoolean("eps_active"))),
                userId);
    }

    @Override
    public Optional<Long> findId(long userId, long planId, String membershipNumber) {
        return jdbc.queryForList("SELECT id FROM user_insurance_affiliations WHERE user_id = ? AND plan_id = ? "
                + "AND membership_number = ?", Long.class, userId, planId, membershipNumber).stream().findFirst();
    }

    @Override
    public void closeCurrent(long userId, LocalDate validTo) {
        jdbc.update("UPDATE user_insurance_affiliations SET is_current = FALSE, valid_to = ? "
                + "WHERE user_id = ? AND is_current = TRUE", validTo, userId);
    }

    @Override
    public void reactivate(long id, LocalDate validFrom) {
        jdbc.update("UPDATE user_insurance_affiliations SET is_current = TRUE, valid_from = ?, valid_to = NULL "
                + "WHERE id = ?", validFrom, id);
    }

    @Override
    public void insert(long userId, long planId, String membershipNumber, LocalDate validFrom, Instant at) {
        jdbc.update("INSERT INTO user_insurance_affiliations(user_id, plan_id, membership_number, is_current, "
                + "valid_from, created_at) VALUES (?, ?, ?, TRUE, ?, ?)", userId, planId, membershipNumber, validFrom,
                Timestamp.from(at));
    }
}
