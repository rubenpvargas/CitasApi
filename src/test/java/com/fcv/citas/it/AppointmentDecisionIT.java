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

/** HU-018 — decisión ADMIN: aprobar conserva franja, rechazar exige motivo y libera, transición inválida. */
class AppointmentDecisionIT extends AbstractMySqlIT {

    @Test
    void ca01ApproveKeepsSlotsAndAuditsAsAdmin() throws Exception {
        long id = requestSpecialized(agendaToday().plusDays(5));
        String admin = tokenOnlyRoles("ADMIN");

        decide(admin, id, true, null).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.rejectionReason").isEmpty());

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM professional_slots WHERE appointment_id = ?", Integer.class, id))
                .isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT h.change_source FROM appointment_status_history h JOIN appointment_statuses s "
                + "ON s.id = h.status_id WHERE h.appointment_id = ? AND s.code = 'APPROVED'", String.class, id)).isEqualTo("ADMIN");
        assertThat(jdbc.queryForObject("SELECT approved_by_user_id IS NOT NULL FROM appointments WHERE id = ?", Boolean.class, id))
                .isTrue();
        decide(admin, id, false, "tarde").andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_TRANSITION"));
    }

    @Test
    void ca02RejectRequiresReasonThenReleasesSlotsAndExposesTheReason() throws Exception {
        long id = requestSpecialized(agendaToday().plusDays(6));
        String admin = tokenOnlyRoles("ADMIN");

        decide(admin, id, false, " ").andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("REJECTION_REASON_REQUIRED"));
        assertThat(statusOf(id)).isEqualTo("REQUESTED");

        JsonNode dto = body(decide(admin, id, false, "Agenda sintetica sin cupo").andExpect(status().isOk()).andReturn());
        assertThat(dto.get("status").asText()).isEqualTo("REJECTED");
        assertThat(dto.get("rejectionReason").asText()).isEqualTo("Agenda sintetica sin cupo");
        assertThat(dto.get("cancellable").asBoolean()).isFalse();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM professional_slots WHERE appointment_id = ?", Integer.class, id))
                .isZero();
        decide(admin, id, true, null).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_TRANSITION"));
    }

    @Test
    void approvedGeneralCannotBeDecidedUnknownIs404AndOtherRolesAre403() throws Exception {
        ProfessionalFixture pro = activeProfessional(List.of("MEDICINA_GENERAL"), List.of("HIC"));
        LocalDate day = agendaToday().plusDays(7);
        publishBlock(pro.token(), day, "08:00", "08:30", "HIC");
        long general = bookGeneral(userToken(), pro.id(), "HIC", day.atTime(8, 0)).get("id").asLong();
        String admin = tokenOnlyRoles("ADMIN");

        decide(admin, general, true, null).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_TRANSITION"));
        decide(admin, 987654L, true, null).andExpect(status().isNotFound());
        decide(userToken(), general, true, null).andExpect(status().isForbidden());
        decide(pro.token(), general, true, null).andExpect(status().isForbidden());
    }

    long requestSpecialized(LocalDate day) throws Exception {
        ProfessionalFixture pro = activeProfessional(List.of("ORTOPEDIA_TRAUMATOLOGIA"), List.of("HIC"));
        publishBlock(pro.token(), day, "08:00", "09:00", "HIC");
        long ortho = jdbc.queryForObject("SELECT id FROM specialties WHERE code = 'ORTOPEDIA_TRAUMATOLOGIA'", Long.class);
        return body(mvc.perform(post("/api/v1/appointments/specialized").header("Authorization", userToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("specialtyId", ortho, "professionalId", pro.id(), "locationCode", "HIC",
                                "startAt", day + "T08:00:00"))))
                .andExpect(status().isCreated()).andReturn()).get("id").asLong();
    }

    ResultActions decide(String token, long id, boolean approve, String reason) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("approve", approve);
        body.put("reason", reason);
        return mvc.perform(post("/api/v1/admin/appointments/" + id + "/decision").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content(toJson(body)));
    }

    String statusOf(long id) {
        return jdbc.queryForObject("SELECT s.code FROM appointments a JOIN appointment_statuses s ON s.id = a.status_id "
                + "WHERE a.id = ?", String.class, id);
    }
}
