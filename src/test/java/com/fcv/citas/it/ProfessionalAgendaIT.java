package com.fcv.citas.it;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** HU-023 — agenda propia: solo APPROVED propias, filtros, datos mínimos y privacidad. */
class ProfessionalAgendaIT extends AbstractMySqlIT {

    @Test
    void ca01ListsOnlyOwnApprovedAppointmentsWithMinimalFields() throws Exception {
        ProfessionalFixture pro = activeProfessional(List.of("MEDICINA_GENERAL", "CARDIOLOGIA_ADULTO"), List.of("HIC", "ICV"));
        ProfessionalFixture other = activeProfessional(List.of("MEDICINA_GENERAL"), List.of("HIC"));
        LocalDate day = agendaToday().plusDays(4);
        publishBlock(pro.token(), day, "08:00", "10:00", "HIC");
        publishBlock(pro.token(), day.plusDays(1), "08:00", "08:30", "ICV");
        publishBlock(other.token(), day, "08:00", "08:30", "HIC");
        long hic = bookGeneral(userToken(), pro.id(), "HIC", day.atTime(8, 0)).get("id").asLong();
        long icv = bookGeneral(userToken(), pro.id(), "ICV", day.plusDays(1).atTime(8, 0)).get("id").asLong();
        String canceller = userToken();
        long cancelled = bookGeneral(canceller, pro.id(), "HIC", day.atTime(8, 30)).get("id").asLong();
        mvc.perform(post("/api/v1/appointments/" + cancelled + "/cancel").header("Authorization", canceller))
                .andExpect(status().isOk());
        long cardio = jdbc.queryForObject("SELECT id FROM specialties WHERE code = 'CARDIOLOGIA_ADULTO'", Long.class);
        mvc.perform(post("/api/v1/appointments/specialized").header("Authorization", userToken())
                        .contentType(MediaType.APPLICATION_JSON).content(toJson(Map.of("specialtyId", cardio,
                                "professionalId", pro.id(), "locationCode", "HIC", "startAt", day + "T09:00:00"))))
                .andExpect(status().isCreated());
        bookGeneral(userToken(), other.id(), "HIC", day.atTime(8, 0));

        JsonNode agenda = body(mvc.perform(get("/api/v1/professional/agenda").header("Authorization", pro.token())
                        .param("from", day.toString()).param("to", day.plusDays(6).toString()))
                .andExpect(status().isOk()).andReturn());

        assertThat(agenda).hasSize(2);
        assertThat(agenda.get(0).get("id").asLong()).isEqualTo(hic);
        assertThat(agenda.get(1).get("id").asLong()).isEqualTo(icv);
        assertThat(agenda.get(0).fieldNames()).toIterable().containsExactlyInAnyOrder("id", "startAt", "endAt",
                "locationCode", "specialtyName", "patientName", "closable");
        assertThat(agenda.get(0).get("patientName").asText()).isEqualTo("Ana Sintetica");
        assertThat(agenda.get(0).get("closable").asBoolean()).isFalse();
        assertThat(agenda.toString()).doesNotContain("@example.test").doesNotContain("3000000000");

        mvc.perform(get("/api/v1/professional/agenda").header("Authorization", pro.token())
                        .param("from", day.toString()).param("to", day.plusDays(6).toString()).param("locationCode", "ICV"))
                .andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].id").value(icv));
        jdbc.update("UPDATE appointments SET scheduled_start_at = ?, scheduled_end_at = ? WHERE id = ?",
                agendaToday().minusDays(1).atTime(8, 0), agendaToday().minusDays(1).atTime(8, 30), hic);
        mvc.perform(get("/api/v1/professional/agenda").header("Authorization", pro.token())
                        .param("from", agendaToday().minusDays(1).toString()).param("to", agendaToday().minusDays(1).toString()))
                .andExpect(jsonPath("$[0].id").value(hic)).andExpect(jsonPath("$[0].closable").value(true));
    }

    @Test
    void ca02PrivacyRangeAndRoles() throws Exception {
        ProfessionalFixture pro = activeProfessional(List.of("MEDICINA_GENERAL"), List.of("HIC"));
        LocalDate day = agendaToday();
        mvc.perform(get("/api/v1/professional/agenda").header("Authorization", pro.token())
                        .param("from", day.toString()).param("to", day.plusDays(31).toString()))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        mvc.perform(get("/api/v1/professional/agenda").header("Authorization", userToken())
                        .param("from", day.toString()).param("to", day.toString()))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/professional/agenda").header("Authorization", tokenOnlyRoles("PROFESSIONAL"))
                        .param("from", day.toString()).param("to", day.toString()))
                .andExpect(status().isForbidden());
    }
}
