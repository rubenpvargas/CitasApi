package com.fcv.citas.adapter.out.persistence;

import com.fcv.citas.application.model.NewProfessionalCommand;
import com.fcv.citas.application.port.out.ProfessionalRepositoryPort;
import com.fcv.citas.domain.model.Location;
import com.fcv.citas.domain.model.ProfessionalAccount;
import com.fcv.citas.domain.model.ProfessionalCapabilitySpecialty;
import com.fcv.citas.domain.model.ProfessionalSummary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Component;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
public class JdbcProfessionalAdapter implements ProfessionalRepositoryPort {
    private static final String LOCATION_COLUMNS = "l.id, l.code, l.name, l.address, l.city, l.department, l.active";
    private static final RowMapper<Location> LOCATION = (rs, n) -> new Location(rs.getLong("id"), rs.getString("code"),
            rs.getString("name"), rs.getString("address"), rs.getString("city"), rs.getString("department"),
            rs.getBoolean("active"));
    private static final String PROFESSIONAL_SELECT = "SELECT p.id, p.user_id, p.professional_code, p.license_number, "
            + "p.active, u.first_name, u.last_name, u.email, u.phone FROM professionals p JOIN users u ON u.id = p.user_id";

    private final JdbcTemplate jdbc;

    public JdbcProfessionalAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean emailExists(String email) {
        return count("SELECT COUNT(*) FROM users WHERE email = ?", email) > 0;
    }

    @Override
    public boolean documentExists(String documentType, String documentNumber) {
        return count("SELECT COUNT(*) FROM users WHERE document_type = ? AND document_number = ?",
                documentType, documentNumber) > 0;
    }

    @Override
    public boolean professionalCodeExists(String code) {
        return count("SELECT COUNT(*) FROM professionals WHERE professional_code = ?", code) > 0;
    }

    @Override
    public boolean licenseExists(String license) {
        return count("SELECT COUNT(*) FROM professionals WHERE license_number = ?", license) > 0;
    }

    @Override
    public long create(NewProfessionalCommand c, String passwordHash, Instant at) {
        Timestamp now = Timestamp.from(at);
        var userKey = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement("INSERT INTO users(first_name, last_name, document_type, "
                    + "document_number, email, phone, password_hash, active, created_at, updated_at) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, TRUE, ?, ?)", Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, c.firstName());
            ps.setString(2, c.lastName());
            ps.setString(3, c.documentType());
            ps.setString(4, c.documentNumber());
            ps.setString(5, c.email());
            ps.setString(6, c.phone());
            ps.setString(7, passwordHash);
            ps.setTimestamp(8, now);
            ps.setTimestamp(9, now);
            return ps;
        }, userKey);
        long userId = userKey.getKey().longValue();
        jdbc.update("INSERT INTO user_roles(user_id, role_id) SELECT ?, id FROM roles WHERE code = 'PROFESSIONAL'", userId);
        var professionalKey = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement("INSERT INTO professionals(user_id, professional_code, "
                    + "license_number, active, created_at, updated_at) VALUES (?, ?, ?, TRUE, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, userId);
            ps.setString(2, c.professionalCode());
            ps.setString(3, c.licenseNumber());
            ps.setTimestamp(4, now);
            ps.setTimestamp(5, now);
            return ps;
        }, professionalKey);
        return professionalKey.getKey().longValue();
    }

    @Override
    public List<ProfessionalSummary> findAll() {
        return assemble(jdbc.query(PROFESSIONAL_SELECT + " ORDER BY u.last_name, u.first_name, p.id", BaseRow.MAPPER));
    }

    @Override
    public Optional<ProfessionalSummary> findById(long id) {
        return assemble(jdbc.query(PROFESSIONAL_SELECT + " WHERE p.id = ?", BaseRow.MAPPER, id)).stream().findFirst();
    }

    @Override
    public Optional<ProfessionalAccount> findAccountByUserId(long userId) {
        return jdbc.query("SELECT id, user_id, active FROM professionals WHERE user_id = ?",
                (rs, n) -> new ProfessionalAccount(rs.getLong("id"), rs.getLong("user_id"), rs.getBoolean("active")),
                userId).stream().findFirst();
    }

    @Override
    public void lock(long professionalId) {
        jdbc.queryForList("SELECT id FROM professionals WHERE id = ? FOR UPDATE", Long.class, professionalId);
    }

    @Override
    public void replaceCapabilities(long id, Set<Long> specialtyIds, long primarySpecialtyId, Set<Long> locationIds,
                                    boolean active, Instant at) {
        jdbc.update("UPDATE professionals SET active = ?, updated_at = ? WHERE id = ?", active, Timestamp.from(at), id);
        jdbc.update("UPDATE professional_specialties SET active = FALSE, is_primary = FALSE WHERE professional_id = ?", id);
        for (Long specialtyId : specialtyIds) {
            jdbc.update("INSERT INTO professional_specialties(professional_id, specialty_id, is_primary, active) "
                    + "VALUES (?, ?, ?, TRUE) ON DUPLICATE KEY UPDATE is_primary = VALUES(is_primary), active = TRUE",
                    id, specialtyId, specialtyId == primarySpecialtyId);
        }
        jdbc.update("UPDATE professional_locations SET active = FALSE WHERE professional_id = ?", id);
        for (Long locationId : locationIds) {
            jdbc.update("INSERT INTO professional_locations(professional_id, location_id, active) VALUES (?, ?, TRUE) "
                    + "ON DUPLICATE KEY UPDATE active = TRUE", id, locationId);
        }
    }

    @Override
    public boolean isLocationAssigned(long professionalId, long locationId) {
        return count("SELECT COUNT(*) FROM professional_locations pl JOIN locations l ON l.id = pl.location_id "
                + "WHERE pl.professional_id = ? AND pl.location_id = ? AND pl.active = TRUE AND l.active = TRUE",
                professionalId, locationId) > 0;
    }

    @Override
    public List<Location> findAllLocations() {
        return jdbc.query("SELECT " + LOCATION_COLUMNS + " FROM locations l ORDER BY l.name", LOCATION);
    }

    @Override
    public Optional<Location> findLocation(long id) {
        return jdbc.query("SELECT " + LOCATION_COLUMNS + " FROM locations l WHERE l.id = ?", LOCATION, id).stream().findFirst();
    }

    @Override
    public Optional<Location> findLocationByCode(String code) {
        return jdbc.query("SELECT " + LOCATION_COLUMNS + " FROM locations l WHERE l.code = ?", LOCATION, code)
                .stream().findFirst();
    }

    private List<ProfessionalSummary> assemble(List<BaseRow> rows) {
        if (rows.isEmpty()) {
            return List.of();
        }
        Map<Long, List<ProfessionalCapabilitySpecialty>> specialties = new HashMap<>();
        jdbc.query("SELECT ps.professional_id, s.id, s.code, s.name, ps.is_primary FROM professional_specialties ps "
                + "JOIN specialties s ON s.id = ps.specialty_id WHERE ps.active = TRUE ORDER BY s.name", rs -> {
            specialties.computeIfAbsent(rs.getLong("professional_id"), k -> new ArrayList<>())
                    .add(new ProfessionalCapabilitySpecialty(rs.getLong("id"), rs.getString("code"), rs.getString("name"),
                            rs.getBoolean("is_primary")));
        });
        Map<Long, List<Location>> locations = new HashMap<>();
        jdbc.query("SELECT pl.professional_id, " + LOCATION_COLUMNS + " FROM professional_locations pl "
                + "JOIN locations l ON l.id = pl.location_id WHERE pl.active = TRUE ORDER BY l.code", rs -> {
            locations.computeIfAbsent(rs.getLong("professional_id"), k -> new ArrayList<>())
                    .add(LOCATION.mapRow(rs, 0));
        });
        return rows.stream().map(r -> new ProfessionalSummary(r.id, r.userId, r.firstName, r.lastName, r.email, r.phone,
                r.code, r.license, r.active, specialties.getOrDefault(r.id, List.of()),
                locations.getOrDefault(r.id, List.of()))).toList();
    }

    private int count(String sql, Object... args) {
        Integer value = jdbc.queryForObject(sql, Integer.class, args);
        return value == null ? 0 : value;
    }

    private record BaseRow(long id, long userId, String firstName, String lastName, String email, String phone,
                           String code, String license, boolean active) {
        static final RowMapper<BaseRow> MAPPER = (rs, n) -> new BaseRow(rs.getLong("id"), rs.getLong("user_id"),
                rs.getString("first_name"), rs.getString("last_name"), rs.getString("email"), rs.getString("phone"),
                rs.getString("professional_code"), rs.getString("license_number"), rs.getBoolean("active"));
    }
}
