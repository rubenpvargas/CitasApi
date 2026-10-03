package com.fcv.citas.adapter.out.persistence;

import com.fcv.citas.application.port.out.AvailabilityBlockRepositoryPort;
import com.fcv.citas.domain.model.AvailabilityBlock;
import com.fcv.citas.domain.model.BlockSchedule;
import com.fcv.citas.domain.model.ExistingBlock;
import com.fcv.citas.domain.model.SlotTime;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Component;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Persistencia de bloques y slots. Fechas y horas de agenda son hora local de pared: se enlazan como
 * {@link LocalDate}/{@link LocalTime}/{@link java.time.LocalDateTime} (sin conversión de zona del
 * driver) y nunca como {@link Timestamp}, que el driver desplazaría según la zona de la JVM.
 */
@Component
public class JdbcAvailabilityBlockAdapter implements AvailabilityBlockRepositoryPort {
    private static final String BLOCK_SELECT = "SELECT b.id, b.professional_id, l.code location_code, "
            + "l.name location_name, b.available_date, b.start_time, b.end_time, COUNT(ps.id) total_slots, "
            + "COALESCE(SUM(CASE WHEN ps.appointment_id IS NOT NULL OR ps.reschedule_request_id IS NOT NULL "
            + "THEN 1 ELSE 0 END), 0) committed_slots FROM availability_blocks b "
            + "JOIN locations l ON l.id = b.location_id "
            + "LEFT JOIN professional_slots ps ON ps.availability_block_id = b.id ";
    private static final String BLOCK_GROUP = " GROUP BY b.id, b.professional_id, l.code, l.name, b.available_date, "
            + "b.start_time, b.end_time";
    private static final RowMapper<AvailabilityBlock> BLOCK = (rs, n) -> new AvailabilityBlock(rs.getLong("id"),
            rs.getLong("professional_id"), rs.getString("location_code"), rs.getString("location_name"),
            schedule(rs), rs.getInt("total_slots"), rs.getInt("committed_slots"));

    private final JdbcTemplate jdbc;

    public JdbcAvailabilityBlockAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<ExistingBlock> findActiveSchedules(long professionalId, LocalDate date) {
        return jdbc.query("SELECT b.id, b.available_date, b.start_time, b.end_time FROM availability_blocks b "
                        + "WHERE b.professional_id = ? AND b.available_date = ? AND b.active = TRUE",
                (rs, n) -> new ExistingBlock(rs.getLong("id"), schedule(rs)), professionalId, date);
    }

    @Override
    public long insert(long professionalId, long locationId, BlockSchedule schedule, Instant at) {
        var keys = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement("INSERT INTO availability_blocks(professional_id, "
                    + "location_id, available_date, start_time, end_time, active, created_at, updated_at) "
                    + "VALUES (?, ?, ?, ?, ?, TRUE, ?, ?)", Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, professionalId);
            ps.setLong(2, locationId);
            ps.setObject(3, schedule.date());
            ps.setObject(4, schedule.startTime());
            ps.setObject(5, schedule.endTime());
            ps.setTimestamp(6, Timestamp.from(at));
            ps.setTimestamp(7, Timestamp.from(at));
            return ps;
        }, keys);
        return keys.getKey().longValue();
    }

    @Override
    public void insertSlots(long blockId, List<SlotTime> slots) {
        List<Object[]> rows = new ArrayList<>();
        for (SlotTime slot : slots) {
            rows.add(new Object[]{blockId, slot.startAt(), slot.endAt()});
        }
        jdbc.batchUpdate("INSERT INTO professional_slots(availability_block_id, start_at, end_at) VALUES (?, ?, ?)", rows);
    }

    @Override
    public void lockBlock(long blockId) {
        jdbc.queryForList("SELECT id FROM availability_blocks WHERE id = ? FOR UPDATE", Long.class, blockId);
        jdbc.queryForList("SELECT id FROM professional_slots WHERE availability_block_id = ? FOR UPDATE", Long.class, blockId);
    }

    @Override
    public Optional<AvailabilityBlock> findActiveOwned(long blockId, long professionalId) {
        return jdbc.query(BLOCK_SELECT + "WHERE b.id = ? AND b.professional_id = ? AND b.active = TRUE" + BLOCK_GROUP,
                BLOCK, blockId, professionalId).stream().findFirst();
    }

    @Override
    public void update(long blockId, long locationId, BlockSchedule schedule, Instant at) {
        jdbc.update("UPDATE availability_blocks SET location_id = ?, available_date = ?, start_time = ?, end_time = ?, "
                + "updated_at = ? WHERE id = ?", locationId, schedule.date(), schedule.startTime(), schedule.endTime(),
                Timestamp.from(at), blockId);
    }

    @Override
    public void deleteFreeSlots(long blockId) {
        jdbc.update("DELETE FROM professional_slots WHERE availability_block_id = ? AND appointment_id IS NULL "
                + "AND reschedule_request_id IS NULL", blockId);
    }

    @Override
    public void deactivate(long blockId, Instant at) {
        jdbc.update("UPDATE availability_blocks SET active = FALSE, updated_at = ? WHERE id = ?", Timestamp.from(at), blockId);
    }

    @Override
    public List<AvailabilityBlock> findCalendar(long professionalId, LocalDate from, LocalDate to, String locationCode) {
        String where = "WHERE b.professional_id = ? AND b.active = TRUE AND b.available_date BETWEEN ? AND ?";
        String order = " ORDER BY b.available_date, b.start_time, b.id";
        return locationCode == null
                ? jdbc.query(BLOCK_SELECT + where + BLOCK_GROUP + order, BLOCK, professionalId, from, to)
                : jdbc.query(BLOCK_SELECT + where + " AND l.code = ?" + BLOCK_GROUP + order, BLOCK, professionalId,
                from, to, locationCode);
    }

    private static BlockSchedule schedule(ResultSet rs) throws SQLException {
        return new BlockSchedule(rs.getObject("available_date", LocalDate.class),
                rs.getObject("start_time", LocalTime.class), rs.getObject("end_time", LocalTime.class));
    }
}
