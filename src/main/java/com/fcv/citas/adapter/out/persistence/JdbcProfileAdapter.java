package com.fcv.citas.adapter.out.persistence;

import com.fcv.citas.application.port.out.ProfileRepositoryPort;
import com.fcv.citas.domain.model.UserProfile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

@Component
public class JdbcProfileAdapter implements ProfileRepositoryPort {
    private final JdbcTemplate jdbc;

    public JdbcProfileAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<UserProfile> findActiveById(long userId) {
        List<UserProfile> rows = jdbc.query("SELECT id, first_name, last_name, email, document_type, document_number, "
                        + "phone FROM users WHERE id = ? AND active = TRUE",
                (rs, n) -> new UserProfile(rs.getLong("id"), rs.getString("first_name"), rs.getString("last_name"),
                        rs.getString("email"), rs.getString("document_type"), rs.getString("document_number"),
                        rs.getString("phone"), null), userId);
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        UserProfile base = rows.getFirst();
        var roles = new HashSet<>(jdbc.queryForList("SELECT r.code FROM user_roles ur JOIN roles r ON r.id = ur.role_id "
                + "WHERE ur.user_id = ?", String.class, userId));
        return Optional.of(new UserProfile(base.id(), base.firstName(), base.lastName(), base.email(),
                base.documentType(), base.documentNumber(), base.phone(), roles));
    }

    @Override
    public boolean updateContact(long userId, String firstName, String lastName, String phone, Instant at) {
        return jdbc.update("UPDATE users SET first_name = ?, last_name = ?, phone = ?, updated_at = ? "
                + "WHERE id = ? AND active = TRUE", firstName, lastName, phone, Timestamp.from(at), userId) == 1;
    }
}
