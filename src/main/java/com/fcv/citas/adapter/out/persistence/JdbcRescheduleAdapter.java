package com.fcv.citas.adapter.out.persistence;

import com.fcv.citas.application.model.InboxFilter;
import com.fcv.citas.application.model.NewReschedule;
import com.fcv.citas.application.model.RescheduleInboxItem;
import com.fcv.citas.application.port.out.RescheduleRepositoryPort;
import com.fcv.citas.domain.model.RescheduleRequest;
import com.fcv.citas.domain.model.RescheduleStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Component;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class JdbcRescheduleAdapter implements RescheduleRepositoryPort {
    private static final String SELECT = "SELECT rr.id, rr.appointment_id, rr.requested_by_user_id, rr.requested_location_id, "
            + "l.code location_code, rs.code status, rr.previous_start_at, rr.previous_end_at, rr.requested_start_at, "
            + "rr.requested_end_at FROM reschedule_requests rr "
            + "JOIN reschedule_request_statuses rs ON rs.id = rr.status_id JOIN locations l ON l.id = rr.requested_location_id ";
    private static final RowMapper<RescheduleRequest> ROW = (rs, n) -> new RescheduleRequest(rs.getLong("id"),
            rs.getLong("appointment_id"), rs.getLong("requested_by_user_id"), rs.getLong("requested_location_id"),
            rs.getString("location_code"), RescheduleStatus.valueOf(rs.getString("status")),
            rs.getObject("previous_start_at", LocalDateTime.class), rs.getObject("previous_end_at", LocalDateTime.class),
            rs.getObject("requested_start_at", LocalDateTime.class), rs.getObject("requested_end_at", LocalDateTime.class));

    private final JdbcTemplate jdbc;

    public JdbcRescheduleAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public long insert(NewReschedule r, Instant at) {
        var keys = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement("INSERT INTO reschedule_requests(appointment_id, "
                    + "requested_by_user_id, requested_location_id, status_id, previous_start_at, previous_end_at, "
                    + "requested_start_at, requested_end_at, created_at) VALUES (?, ?, ?, "
                    + "(SELECT id FROM reschedule_request_statuses WHERE code = 'PENDING'), ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, r.appointmentId());
            ps.setLong(2, r.requestedByUserId());
            ps.setLong(3, r.requestedLocationId());
            ps.setObject(4, r.previousStartAt());
            ps.setObject(5, r.previousEndAt());
            ps.setObject(6, r.requestedStartAt());
            ps.setObject(7, r.requestedEndAt());
            ps.setTimestamp(8, Timestamp.from(at));
            return ps;
        }, keys);
        return keys.getKey().longValue();
    }

    @Override
    public Optional<RescheduleRequest> findPendingForAppointment(long appointmentId) {
        return jdbc.query(SELECT + "WHERE rr.appointment_id = ? AND rs.code = 'PENDING'", ROW, appointmentId)
                .stream().findFirst();
    }

    @Override
    public Optional<RescheduleRequest> lockById(long id) {
        if (jdbc.queryForList("SELECT id FROM reschedule_requests WHERE id = ? FOR UPDATE", Long.class, id).isEmpty()) {
            return Optional.empty();
        }
        return jdbc.query(SELECT + "WHERE rr.id = ?", ROW, id).stream().findFirst();
    }

    @Override
    public void updateStatus(long id, RescheduleStatus status, Long decidedByUserId, String reason, LocalDateTime decidedAt) {
        jdbc.update("UPDATE reschedule_requests SET status_id = (SELECT id FROM reschedule_request_statuses WHERE code = ?), "
                + "decided_by_user_id = ?, decision_reason = ?, decided_at = ? WHERE id = ?",
                status.name(), decidedByUserId, reason, decidedAt, id);
    }

    @Override
    public void addHistory(long id, RescheduleStatus status, Long actorUserId, String source, String reason, Instant at) {
        jdbc.update("INSERT INTO reschedule_request_history(reschedule_request_id, status_id, changed_by_user_id, "
                + "change_source, reason, changed_at) VALUES (?, (SELECT id FROM reschedule_request_statuses WHERE code = ?), "
                + "?, ?, ?, ?)", id, status.name(), actorUserId, source, reason, Timestamp.from(at));
    }

    @Override
    public List<RescheduleInboxItem> findPending(InboxFilter f) {
        StringBuilder sql = new StringBuilder("SELECT rr.id, rr.appointment_id, CONCAT(u.first_name, ' ', u.last_name) "
                + "patient_name, a.professional_id, CONCAT(pu.first_name, ' ', pu.last_name) professional_name, "
                + "a.specialty_id, s.name specialty_name, l.code location_code, a.scheduled_start_at, a.scheduled_end_at, "
                + "rr.requested_start_at, rr.requested_end_at, rr.created_at FROM reschedule_requests rr "
                + "JOIN reschedule_request_statuses rs ON rs.id = rr.status_id "
                + "JOIN appointments a ON a.id = rr.appointment_id "
                + "JOIN users u ON u.id = a.patient_user_id "
                + "JOIN professionals p ON p.id = a.professional_id JOIN users pu ON pu.id = p.user_id "
                + "JOIN specialties s ON s.id = a.specialty_id "
                + "JOIN locations l ON l.id = rr.requested_location_id "
                + "WHERE rs.code = 'PENDING'");
        List<Object> args = new ArrayList<>();
        if (f.locationCode() != null) {
            sql.append(" AND l.code = ?");
            args.add(f.locationCode());
        }
        if (f.professionalId() != null) {
            sql.append(" AND a.professional_id = ?");
            args.add(f.professionalId());
        }
        if (f.specialtyId() != null) {
            sql.append(" AND a.specialty_id = ?");
            args.add(f.specialtyId());
        }
        JdbcAppointmentAdapter.appendDateRange(sql, args, "rr.requested_start_at", f.from(), f.to());
        sql.append(" ORDER BY rr.requested_start_at, rr.id");
        return jdbc.query(sql.toString(), (rs, n) -> new RescheduleInboxItem(rs.getLong("id"), rs.getLong("appointment_id"),
                rs.getString("patient_name"), rs.getLong("professional_id"), rs.getString("professional_name"),
                rs.getLong("specialty_id"), rs.getString("specialty_name"), rs.getString("location_code"),
                rs.getObject("scheduled_start_at", LocalDateTime.class), rs.getObject("scheduled_end_at", LocalDateTime.class),
                rs.getObject("requested_start_at", LocalDateTime.class), rs.getObject("requested_end_at", LocalDateTime.class),
                rs.getTimestamp("created_at").toInstant()), args.toArray());
    }
}
