package com.fcv.citas.it;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** HU-020 — cancelación: libera slots, cierra reprogramación pendiente, sin reactivación. */
class CancelAppointmentIT extends AbstractMySqlIT {

    @Test
    void ca01CancelReleasesSlotsAuditsAndClosesPendingRescheduleReleasingItsHold() throws Exception {
        ProfessionalFixture pro = activeProfessional(List.of("MEDICINA_GENERAL"), List.of("HIC"));
        LocalDate day = agendaToday().plusDays(5);
        long block = publishBlock(pro.token(), day, "08:00", "09:00", "HIC");
        String user = userToken();
        JsonNode booked = bookGeneral(user, pro.id(), "HIC", day.atTime(8, 0));
        long id = booked.get("id").asLong();
        long requestId = pendingRescheduleFor(id, day.atTime(8, 30));
        jdbc.update("UPDATE professional_slots SET reschedule_request_id = ? WHERE availability_block_id = ? AND start_at = ?",
                requestId, block, day.atTime(8, 30));

        mvc.perform(post("/api/v1/appointments/" + id + "/cancel").header("Authorization", user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancellable").value(false))
                .andExpect(jsonPath("$.pendingReschedule").isEmpty());

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM professional_slots WHERE availability_block_id = ? "
                + "AND appointment_id IS NULL AND reschedule_request_id IS NULL", Integer.class, block)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT s.code FROM reschedule_requests r JOIN reschedule_request_statuses s "
                + "ON s.id = r.status_id WHERE r.id = ?", String.class, requestId)).isEqualTo("CANCELLED");
        assertThat(jdbc.queryForObject("SELECT change_source FROM reschedule_request_history WHERE reschedule_request_id = ?",
                String.class, requestId)).isEqualTo("SYSTEM");
        assertThat(jdbc.queryForObject("SELECT h.change_source FROM appointment_status_history h JOIN appointment_statuses s "
                + "ON s.id = h.status_id WHERE h.appointment_id = ? AND s.code = 'CANCELLED'", String.class, id)).isEqualTo("USER");
    }

    @Test
    void ca02Ca03ForeignPastOrTerminalCannotBeCancelledAndThereIsNoReactivation() throws Exception {
        ProfessionalFixture pro = activeProfessional(List.of("MEDICINA_GENERAL"), List.of("HIC"));
        LocalDate day = agendaToday().plusDays(6);
        publishBlock(pro.token(), day, "08:00", "09:00", "HIC");
        String user = userToken();
        long first = bookGeneral(user, pro.id(), "HIC", day.atTime(8, 0)).get("id").asLong();
        long second = bookGeneral(user, pro.id(), "HIC", day.atTime(8, 30)).get("id").asLong();

        mvc.perform(post("/api/v1/appointments/" + first + "/cancel").header("Authorization", userToken()))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/appointments/" + first + "/cancel").header("Authorization", user))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/appointments/" + first + "/cancel").header("Authorization", user))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("INVALID_TRANSITION"));
        jdbc.update("UPDATE appointments SET scheduled_start_at = ?, scheduled_end_at = ? WHERE id = ?",
                agendaToday().minusDays(1).atTime(8, 0), agendaToday().minusDays(1).atTime(8, 30), second);
        mvc.perform(post("/api/v1/appointments/" + second + "/cancel").header("Authorization", user))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("INVALID_TRANSITION"));
        assertThat(jdbc.queryForObject("SELECT s.code FROM appointments a JOIN appointment_statuses s ON s.id = a.status_id "
                + "WHERE a.id = ?", String.class, second)).isEqualTo("APPROVED");
        mvc.perform(post("/api/v1/appointments/" + first + "/reactivate").header("Authorization", user))
                .andExpect(status().isNotFound());
    }

    private long pendingRescheduleFor(long appointmentId, java.time.LocalDateTime requestedStart) {
        jdbc.update("INSERT INTO reschedule_requests(appointment_id, requested_by_user_id, requested_location_id, status_id, "
                + "previous_start_at, previous_end_at, requested_start_at, requested_end_at, created_at) "
                + "SELECT a.id, a.patient_user_id, a.location_id, rs.id, a.scheduled_start_at, a.scheduled_end_at, ?, ?, NOW(6) "
                + "FROM appointments a, reschedule_request_statuses rs WHERE a.id = ? AND rs.code = 'PENDING'",
                requestedStart, requestedStart.plusMinutes(30), appointmentId);
        return jdbc.queryForObject("SELECT MAX(id) FROM reschedule_requests WHERE appointment_id = ?", Long.class, appointmentId);
    }
}
