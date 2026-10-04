package com.fcv.citas.adapter.out.persistence;

import com.fcv.citas.application.model.InboxFilter;
import com.fcv.citas.application.model.NewAppointment;
import com.fcv.citas.application.model.ReminderItem;
import com.fcv.citas.application.port.out.AppointmentRepositoryPort;
import com.fcv.citas.domain.model.Appointment;
import com.fcv.citas.domain.model.AppointmentStatus;
import com.fcv.citas.domain.model.PendingReschedule;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Component;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Persistencia de citas e historial. Las franjas son hora local de pared (DATETIME) enlazadas como
 * {@link LocalDateTime}; los sellos de auditoría (TIMESTAMP) como instantes.
 */
@Component
public class JdbcAppointmentAdapter implements AppointmentRepositoryPort {
    static final String VIEW = "SELECT a.id, a.patient_user_id, CONCAT(u.first_name, ' ', u.last_name) patient_name, "
            + "a.professional_id, CONCAT(pu.first_name, ' ', pu.last_name) professional_name, a.specialty_id, "
            + "s.name specialty_name, s.is_general, a.location_id, l.code location_code, l.name location_name, "
            + "st.code status, a.scheduled_start_at, a.scheduled_end_at, a.reason, a.created_at, "
            + "(SELECT h.reason FROM appointment_status_history h JOIN appointment_statuses hs ON hs.id = h.status_id "
            + " WHERE h.appointment_id = a.id AND hs.code = 'REJECTED' ORDER BY h.changed_at DESC, h.id DESC LIMIT 1) rejection_reason, "
            + "rr.id rr_id, rr.requested_start_at rr_start, rr.requested_end_at rr_end, rl.code rr_location_code "
            + "FROM appointments a "
            + "JOIN appointment_statuses st ON st.id = a.status_id "
            + "JOIN locations l ON l.id = a.location_id "
            + "JOIN specialties s ON s.id = a.specialty_id "
            + "JOIN professionals p ON p.id = a.professional_id "
            + "JOIN users pu ON pu.id = p.user_id "
            + "JOIN users u ON u.id = a.patient_user_id "
            + "LEFT JOIN reschedule_requests rr ON rr.appointment_id = a.id AND rr.status_id = "
            + " (SELECT id FROM reschedule_request_statuses WHERE code = 'PENDING') "
            + "LEFT JOIN locations rl ON rl.id = rr.requested_location_id ";
    static final RowMapper<Appointment> ROW = (rs, n) -> {
        long rrId = rs.getLong("rr_id");
        PendingReschedule pending = rs.wasNull() ? null : new PendingReschedule(rrId,
                rs.getObject("rr_start", LocalDateTime.class), rs.getObject("rr_end", LocalDateTime.class),
                rs.getString("rr_location_code"));
        Timestamp created = rs.getTimestamp("created_at");
        return new Appointment(rs.getLong("id"), rs.getLong("patient_user_id"), rs.getString("patient_name"),
                rs.getLong("professional_id"), rs.getString("professional_name"), rs.getLong("specialty_id"),
                rs.getString("specialty_name"), rs.getBoolean("is_general"), rs.getLong("location_id"),
                rs.getString("location_code"), rs.getString("location_name"), AppointmentStatus.valueOf(rs.getString("status")),
                rs.getObject("scheduled_start_at", LocalDateTime.class), rs.getObject("scheduled_end_at", LocalDateTime.class),
                rs.getString("reason"), rs.getString("rejection_reason"), pending,
                created == null ? null : created.toInstant());
    };

    private final JdbcTemplate jdbc;

    public JdbcAppointmentAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public long insert(NewAppointment a, Instant at) {
        var keys = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement("INSERT INTO appointments(patient_user_id, professional_id, "
                    + "location_id, specialty_id, status_id, reason, scheduled_start_at, scheduled_end_at, created_by_user_id, "
                    + "approved_at, created_at, updated_at) VALUES (?, ?, ?, ?, (SELECT id FROM appointment_statuses WHERE code = ?), "
                    + "?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, a.patientUserId());
            ps.setLong(2, a.professionalId());
            ps.setLong(3, a.locationId());
            ps.setLong(4, a.specialtyId());
            ps.setString(5, a.status().name());
            ps.setString(6, a.reason());
            ps.setObject(7, a.startAt());
            ps.setObject(8, a.endAt());
            ps.setLong(9, a.patientUserId());
            if (a.approvedAt() == null) {
                ps.setNull(10, Types.TIMESTAMP);
            } else {
                ps.setObject(10, a.approvedAt());
            }
            ps.setTimestamp(11, Timestamp.from(at));
            ps.setTimestamp(12, Timestamp.from(at));
            return ps;
        }, keys);
        return keys.getKey().longValue();
    }

    @Override
    public Optional<Appointment> findById(long id) {
        return jdbc.query(VIEW + "WHERE a.id = ?", ROW, id).stream().findFirst();
    }

    @Override
    public Optional<Appointment> lockById(long id) {
        if (jdbc.queryForList("SELECT id FROM appointments WHERE id = ? FOR UPDATE", Long.class, id).isEmpty()) {
            return Optional.empty();
        }
        return findById(id);
    }

    @Override
    public List<Appointment> findByPatient(long patientUserId, AppointmentStatus status, LocalDate from, LocalDate to) {
        StringBuilder sql = new StringBuilder(VIEW).append("WHERE a.patient_user_id = ?");
        List<Object> args = new ArrayList<>(List.of(patientUserId));
        if (status != null) {
            sql.append(" AND st.code = ?");
            args.add(status.name());
        }
        appendDateRange(sql, args, "a.scheduled_start_at", from, to);
        sql.append(" ORDER BY a.scheduled_start_at, a.id");
        return jdbc.query(sql.toString(), ROW, args.toArray());
    }

    @Override
    public void updateStatus(long id, AppointmentStatus status, Long approvedByUserId, LocalDateTime approvedAt, Instant at) {
        if (approvedAt != null) {
            jdbc.update("UPDATE appointments SET status_id = (SELECT id FROM appointment_statuses WHERE code = ?), "
                    + "approved_by_user_id = ?, approved_at = ?, updated_at = ? WHERE id = ?",
                    status.name(), approvedByUserId, approvedAt, Timestamp.from(at), id);
        } else {
            jdbc.update("UPDATE appointments SET status_id = (SELECT id FROM appointment_statuses WHERE code = ?), "
                    + "updated_at = ? WHERE id = ?", status.name(), Timestamp.from(at), id);
        }
    }

    @Override
    public void moveTo(long id, long locationId, LocalDateTime startAt, LocalDateTime endAt, Instant at) {
        jdbc.update("UPDATE appointments SET location_id = ?, scheduled_start_at = ?, scheduled_end_at = ?, updated_at = ? "
                + "WHERE id = ?", locationId, startAt, endAt, Timestamp.from(at), id);
    }

    @Override
    public void addHistory(long id, AppointmentStatus status, Long actorUserId, String source, String reason, Instant at) {
        jdbc.update("INSERT INTO appointment_status_history(appointment_id, status_id, changed_by_user_id, change_source, "
                + "reason, changed_at) VALUES (?, (SELECT id FROM appointment_statuses WHERE code = ?), ?, ?, ?, ?)",
                id, status.name(), actorUserId, source, reason, Timestamp.from(at));
    }

    @Override
    public List<Appointment> findAgenda(long professionalId, LocalDate from, LocalDate to, String locationCode) {
        StringBuilder sql = new StringBuilder(VIEW).append("WHERE a.professional_id = ? AND st.code = 'APPROVED'");
        List<Object> args = new ArrayList<>(List.of(professionalId));
        appendDateRange(sql, args, "a.scheduled_start_at", from, to);
        if (locationCode != null) {
            sql.append(" AND l.code = ?");
            args.add(locationCode);
        }
        sql.append(" ORDER BY a.scheduled_start_at, a.id");
        return jdbc.query(sql.toString(), ROW, args.toArray());
    }

    @Override
    public List<Appointment> findRequested(InboxFilter f) {
        StringBuilder sql = new StringBuilder(VIEW).append("WHERE st.code = 'REQUESTED'");
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
        appendDateRange(sql, args, "a.scheduled_start_at", f.from(), f.to());
        sql.append(" ORDER BY a.scheduled_start_at, a.id");
        return jdbc.query(sql.toString(), ROW, args.toArray());
    }

    /** Intervalo semiabierto [from, to) sobre el inicio de citas APPROVED. */
    @Override
    public List<ReminderItem> findApprovedStartingBetween(LocalDateTime from, LocalDateTime to) {
        return jdbc.query("SELECT a.id, a.scheduled_start_at, a.scheduled_end_at, l.name location_name, s.name specialty_name, "
                        + "CONCAT(pu.first_name, ' ', pu.last_name) professional_name, u.first_name, u.email "
                        + "FROM appointments a JOIN appointment_statuses st ON st.id = a.status_id "
                        + "JOIN users u ON u.id = a.patient_user_id JOIN locations l ON l.id = a.location_id "
                        + "JOIN specialties s ON s.id = a.specialty_id JOIN professionals p ON p.id = a.professional_id "
                        + "JOIN users pu ON pu.id = p.user_id "
                        + "WHERE st.code = 'APPROVED' AND a.scheduled_start_at >= ? AND a.scheduled_start_at < ? "
                        + "ORDER BY a.scheduled_start_at, a.id",
                (rs, n) -> new ReminderItem(rs.getLong("id"), rs.getObject("scheduled_start_at", LocalDateTime.class),
                        rs.getObject("scheduled_end_at", LocalDateTime.class), rs.getString("location_name"),
                        rs.getString("specialty_name"), rs.getString("professional_name"), rs.getString("first_name"),
                        rs.getString("email")), from, to);
    }

    /** Rango inclusivo por fecha local sobre una columna DATETIME, apto para índice (sin DATE()). */
    static void appendDateRange(StringBuilder sql, List<Object> args, String column, LocalDate from, LocalDate to) {
        if (from != null) {
            sql.append(" AND ").append(column).append(" >= ?");
            args.add(from.atStartOfDay());
        }
        if (to != null) {
            sql.append(" AND ").append(column).append(" < ?");
            args.add(to.plusDays(1).atStartOfDay());
        }
    }
}
