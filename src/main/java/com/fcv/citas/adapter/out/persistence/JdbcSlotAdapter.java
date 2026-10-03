package com.fcv.citas.adapter.out.persistence;

import com.fcv.citas.application.port.out.SlotRepositoryPort;
import com.fcv.citas.domain.model.LockedSlot;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class JdbcSlotAdapter implements SlotRepositoryPort {
    private final JdbcTemplate jdbc;

    public JdbcSlotAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<LockedSlot> lockFreeSlots(long professionalId, long locationId, LocalDateTime from, LocalDateTime to) {
        // ORDER BY start_at fija un orden de bloqueo estable entre transacciones (evita interbloqueos cruzados).
        return jdbc.query("SELECT ps.id, ps.availability_block_id, ps.start_at FROM professional_slots ps "
                        + "JOIN availability_blocks b ON b.id = ps.availability_block_id "
                        + "WHERE b.professional_id = ? AND b.location_id = ? AND b.active = TRUE "
                        + "AND ps.start_at >= ? AND ps.end_at <= ? "
                        + "AND ps.appointment_id IS NULL AND ps.reschedule_request_id IS NULL "
                        + "ORDER BY ps.start_at FOR UPDATE",
                (rs, n) -> new LockedSlot(rs.getLong("id"), rs.getLong("availability_block_id"),
                        rs.getObject("start_at", LocalDateTime.class)),
                professionalId, locationId, from, to);
    }

    @Override
    public void assignToAppointment(List<Long> slotIds, long appointmentId) {
        List<Object[]> rows = new ArrayList<>();
        slotIds.forEach(id -> rows.add(new Object[]{appointmentId, id}));
        jdbc.batchUpdate("UPDATE professional_slots SET appointment_id = ? WHERE id = ? AND appointment_id IS NULL "
                + "AND reschedule_request_id IS NULL", rows);
    }

    @Override
    public void holdForReschedule(List<Long> slotIds, long rescheduleRequestId) {
        List<Object[]> rows = new ArrayList<>();
        slotIds.forEach(id -> rows.add(new Object[]{rescheduleRequestId, id}));
        jdbc.batchUpdate("UPDATE professional_slots SET reschedule_request_id = ? WHERE id = ? AND appointment_id IS NULL "
                + "AND reschedule_request_id IS NULL", rows);
    }

    @Override
    public void releaseAppointment(long appointmentId) {
        jdbc.update("UPDATE professional_slots SET appointment_id = NULL WHERE appointment_id = ?", appointmentId);
    }

    @Override
    public void releaseHold(long rescheduleRequestId) {
        jdbc.update("UPDATE professional_slots SET reschedule_request_id = NULL WHERE reschedule_request_id = ?",
                rescheduleRequestId);
    }

    @Override
    public void transferHoldToAppointment(long rescheduleRequestId, long appointmentId) {
        jdbc.update("UPDATE professional_slots SET appointment_id = ?, reschedule_request_id = NULL "
                + "WHERE reschedule_request_id = ?", appointmentId, rescheduleRequestId);
    }
}
