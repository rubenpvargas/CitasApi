package com.fcv.citas.adapter.out.persistence;

import com.fcv.citas.application.model.CandidateSlot;
import com.fcv.citas.application.port.out.AvailabilityQueryPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class JdbcAvailabilityQueryAdapter implements AvailabilityQueryPort {
    private static final String SQL = "SELECT ps.availability_block_id, ps.start_at, p.id professional_id, "
            + "CONCAT(u.first_name, ' ', u.last_name) professional_name, l.code location_code, l.name location_name "
            + "FROM professional_slots ps "
            + "JOIN availability_blocks b ON b.id = ps.availability_block_id AND b.active = TRUE "
            + "JOIN professionals p ON p.id = b.professional_id AND p.active = TRUE "
            + "JOIN users u ON u.id = p.user_id AND u.active = TRUE "
            + "JOIN professional_specialties psp ON psp.professional_id = p.id AND psp.specialty_id = ? AND psp.active = TRUE "
            + "JOIN professional_locations pl ON pl.professional_id = p.id AND pl.location_id = b.location_id AND pl.active = TRUE "
            + "JOIN locations l ON l.id = b.location_id AND l.active = TRUE "
            + "WHERE ps.appointment_id IS NULL AND ps.reschedule_request_id IS NULL "
            + "AND b.available_date BETWEEN ? AND ?";

    private final JdbcTemplate jdbc;

    public JdbcAvailabilityQueryAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<CandidateSlot> findFreeSlots(long specialtyId, LocalDate from, LocalDate to, String locationCode,
                                             Long professionalId) {
        StringBuilder sql = new StringBuilder(SQL);
        List<Object> args = new ArrayList<>(List.of(specialtyId, from, to));
        if (locationCode != null) {
            sql.append(" AND l.code = ?");
            args.add(locationCode);
        }
        if (professionalId != null) {
            sql.append(" AND p.id = ?");
            args.add(professionalId);
        }
        sql.append(" ORDER BY ps.start_at, p.id");
        return jdbc.query(sql.toString(), (rs, n) -> new CandidateSlot(rs.getLong("availability_block_id"),
                rs.getObject("start_at", LocalDateTime.class), rs.getLong("professional_id"),
                rs.getString("professional_name"), rs.getString("location_code"), rs.getString("location_name")),
                args.toArray());
    }
}
