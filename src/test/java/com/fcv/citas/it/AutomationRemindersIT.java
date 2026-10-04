package com.fcv.citas.it;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Ola G (WF-001) — credencial X-Automation-Key de mínimo privilegio y ventana sin duplicados. */
class AutomationRemindersIT extends AbstractMySqlIT {
    private static final String ROUTE = "/api/v1/automation/appointments/reminders";

    @Test
    void absentOrWrongKeyIs401() throws Exception {
        mvc.perform(get(ROUTE)).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        mvc.perform(get(ROUTE).header("X-Automation-Key", "clave-equivocada"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void keyIsOnlyValidOnAutomationRoutesAndJwtIsNotValidThere() throws Exception {
        mvc.perform(get("/api/v1/me").header("X-Automation-Key", AUTOMATION_KEY)).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/admin/inbox").header("X-Automation-Key", AUTOMATION_KEY)).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/catalogs").header("X-Automation-Key", AUTOMATION_KEY)).andExpect(status().isForbidden());
        mvc.perform(get(ROUTE).header("Authorization", adminToken())).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/admin/automation/appointments/reminders").header("Authorization", adminToken()))
                .andExpect(status().isNotFound());
    }

    @Test
    void returnsOnlyApprovedAppointmentsInsideTheHalfOpenWindowWithMinimalFields() throws Exception {
        LocalDateTime end = LocalDateTime.now(ZoneId.of("America/Bogota")).withSecond(0).withNano(0).plusHours(24);
        long insideEarly = appointment(end.minusMinutes(55), "APPROVED");
        long insideLate = appointment(end.minusMinutes(5), "APPROVED");
        long afterEnd = appointment(end.plusMinutes(5), "APPROVED");
        long beforeStart = appointment(end.minusMinutes(65), "APPROVED");
        long requested = appointment(end.minusMinutes(30), "REQUESTED");

        JsonNode body = body(mvc.perform(get(ROUTE).header("X-Automation-Key", AUTOMATION_KEY)
                        .param("hours", "24").param("windowMinutes", "60"))
                .andExpect(status().isOk()).andReturn());

        List<Long> ids = new ArrayList<>();
        body.forEach(item -> ids.add(item.get("appointmentId").asLong()));
        assertThat(ids).contains(insideEarly, insideLate).doesNotContain(afterEnd, beforeStart, requested);
        JsonNode item = body.get(ids.indexOf(insideEarly));
        assertThat(item.fieldNames()).toIterable().containsExactlyInAnyOrder("appointmentId", "startAt", "endAt",
                "locationName", "specialtyName", "professionalName", "recipient");
        assertThat(item.get("recipient").fieldNames()).toIterable().containsExactlyInAnyOrder("firstName", "email");
        assertThat(item.get("recipient").get("firstName").asText()).isEqualTo("Ana");
        assertThat(item.toString()).doesNotContain("documentNumber").doesNotContain("phone").doesNotContain("userId");
    }

    @Test
    void parametersOutOfRangeAre400() throws Exception {
        mvc.perform(get(ROUTE).header("X-Automation-Key", AUTOMATION_KEY).param("hours", "73"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        mvc.perform(get(ROUTE).header("X-Automation-Key", AUTOMATION_KEY).param("windowMinutes", "10"))
                .andExpect(status().isBadRequest());
    }

    private long appointment(LocalDateTime start, String status) throws Exception {
        String email = registerUser();
        long userId = jdbc.queryForObject("SELECT id FROM users WHERE email = ?", Long.class, email);
        jdbc.update("INSERT INTO appointments(patient_user_id, professional_id, location_id, specialty_id, status_id, "
                + "scheduled_start_at, scheduled_end_at, created_by_user_id, created_at, updated_at) "
                + "SELECT ?, 9001, l.id, s.id, st.id, ?, ?, ?, NOW(6), NOW(6) FROM locations l, specialties s, "
                + "appointment_statuses st WHERE l.code = 'HIC' AND s.code = 'MEDICINA_GENERAL' AND st.code = ?",
                userId, start, start.plusMinutes(30), userId, status);
        return jdbc.queryForObject("SELECT MAX(id) FROM appointments WHERE patient_user_id = ?", Long.class, userId);
    }
}
