package com.fcv.citas.it;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** HU-022 — decisión de reprogramación: estado final de slots (libres, retenidos, asignados) y auditoría. */
class RescheduleDecisionIT extends AbstractMySqlIT {

    record Scenario(long appointmentId, long requestId, long originalBlock, long targetBlock, String user, LocalDate day) {
    }

    Scenario scenario(int offsetDays) throws Exception {
        ProfessionalFixture pro = activeProfessional(List.of("MEDICINA_GENERAL"), List.of("HIC", "ICV"));
        LocalDate day = agendaToday().plusDays(offsetDays);
        long original = publishBlock(pro.token(), day, "08:00", "08:30", "HIC");
        long target = publishBlock(pro.token(), day.plusDays(1), "10:00", "10:30", "ICV");
        String user = userToken();
        long id = bookGeneral(user, pro.id(), "HIC", day.atTime(8, 0)).get("id").asLong();
        long requestId = body(mvc.perform(post("/api/v1/appointments/" + id + "/reschedule").header("Authorization", user)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("startAt", day.plusDays(1) + "T10:00:00", "locationCode", "ICV"))))
                .andExpect(status().isCreated()).andReturn()).get("id").asLong();
        return new Scenario(id, requestId, original, target, user, day);
    }

    @Test
    void ca01ApproveMovesAppointmentFreesOldSlotAndAssignsHeldSlot() throws Exception {
        Scenario s = scenario(5);
        String admin = tokenOnlyRoles("ADMIN");

        JsonNode dto = body(decide(admin, s.requestId(), true, null).andExpect(status().isOk()).andReturn());

        assertThat(dto.get("startAt").asText()).isEqualTo(s.day().plusDays(1) + "T10:00:00");
        assertThat(dto.get("locationCode").asText()).isEqualTo("ICV");
        assertThat(dto.get("status").asText()).isEqualTo("APPROVED");
        assertThat(dto.get("pendingReschedule").isNull()).isTrue();
        assertThat(slot(s.originalBlock())).isEqualTo("FREE");
        assertThat(slot(s.targetBlock())).isEqualTo("ASSIGNED:" + s.appointmentId());
        assertThat(requestStatus(s.requestId())).isEqualTo("APPROVED");
        assertThat(jdbc.queryForList("SELECT change_source FROM reschedule_request_history WHERE reschedule_request_id = ? "
                + "ORDER BY id", String.class, s.requestId())).containsExactly("USER", "ADMIN");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM appointment_status_history WHERE appointment_id = ? "
                + "AND change_source = 'ADMIN'", Integer.class, s.appointmentId())).isOne();
        decide(admin, s.requestId(), false, "otra vez").andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_TRANSITION"));
    }

    @Test
    void ca02RejectRequiresReasonFreesTheHoldAndKeepsTheOriginal() throws Exception {
        Scenario s = scenario(7);
        String admin = tokenOnlyRoles("ADMIN");

        decide(admin, s.requestId(), false, "").andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("REJECTION_REASON_REQUIRED"));
        assertThat(slot(s.targetBlock())).isEqualTo("HELD:" + s.requestId());

        JsonNode dto = body(decide(admin, s.requestId(), false, "Franja sintetica reservada").andExpect(status().isOk()).andReturn());

        assertThat(dto.get("startAt").asText()).isEqualTo(s.day() + "T08:00:00");
        assertThat(dto.get("locationCode").asText()).isEqualTo("HIC");
        assertThat(dto.get("reschedulable").asBoolean()).isTrue();
        assertThat(slot(s.originalBlock())).isEqualTo("ASSIGNED:" + s.appointmentId());
        assertThat(slot(s.targetBlock())).isEqualTo("FREE");
        assertThat(requestStatus(s.requestId())).isEqualTo("REJECTED");
        assertThat(jdbc.queryForObject("SELECT decision_reason FROM reschedule_requests WHERE id = ?", String.class,
                s.requestId())).isEqualTo("Franja sintetica reservada");
    }

    @Test
    void securityAndUnknownRequest() throws Exception {
        Scenario s = scenario(9);
        decide(s.user(), s.requestId(), true, null).andExpect(status().isForbidden());
        decide(tokenOnlyRoles("ADMIN"), 987654L, true, null).andExpect(status().isNotFound());
        assertThat(requestStatus(s.requestId())).isEqualTo("PENDING");
    }

    String slot(long blockId) {
        Map<String, Object> row = jdbc.queryForMap("SELECT appointment_id, reschedule_request_id FROM professional_slots "
                + "WHERE availability_block_id = ?", blockId);
        if (row.get("appointment_id") != null) {
            return "ASSIGNED:" + row.get("appointment_id");
        }
        return row.get("reschedule_request_id") != null ? "HELD:" + row.get("reschedule_request_id") : "FREE";
    }

    String requestStatus(long id) {
        return jdbc.queryForObject("SELECT s.code FROM reschedule_requests r JOIN reschedule_request_statuses s "
                + "ON s.id = r.status_id WHERE r.id = ?", String.class, id);
    }

    ResultActions decide(String token, long id, boolean approve, String reason) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("approve", approve);
        body.put("reason", reason);
        return mvc.perform(post("/api/v1/admin/reschedules/" + id + "/decision").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content(toJson(body)));
    }
}
