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

/** HU-014 — calendario propio: rango, sede, ocupación, editabilidad y aislamiento sin datos de pacientes. */
class ProfessionalCalendarIT extends AbstractMySqlIT {

    @Test
    void ca01ListsOwnBlocksByDateAndLocationWithCommittedCountsAndEditability() throws Exception {
        ProfessionalFixture pro = activeProfessional(List.of("MEDICINA_GENERAL"), List.of("HIC", "ICV"));
        LocalDate day = agendaToday().plusDays(2);
        long free = createBlock(pro.token(), day, "08:00", "09:00", "HIC");
        long booked = createBlock(pro.token(), day.plusDays(1), "10:00", "11:00", "ICV");
        mvc.perform(post("/api/v1/appointments/general").header("Authorization", userToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("professionalId", pro.id(), "locationCode", "ICV",
                                "startAt", day.plusDays(1) + "T10:00:00", "reason", "Control sintetico"))))
                .andExpect(status().isOk());

        JsonNode calendar = body(mvc.perform(get("/api/v1/professional/calendar").header("Authorization", pro.token())
                        .param("from", day.toString()).param("to", day.plusDays(5).toString()))
                .andExpect(status().isOk()).andReturn());

        assertThat(calendar).hasSize(2);
        JsonNode first = calendar.get(0);
        assertThat(first.get("id").asLong()).isEqualTo(free);
        assertThat(first.get("date").asText()).isEqualTo(day.toString());
        assertThat(first.get("startTime").asText()).isEqualTo("08:00");
        assertThat(first.get("locationName").asText()).isNotBlank();
        assertThat(first.get("totalSlots").asInt()).isEqualTo(2);
        assertThat(first.get("editable").asBoolean()).isTrue();
        assertThat(first.get("notEditableReason").isNull()).isTrue();
        JsonNode second = calendar.get(1);
        assertThat(second.get("id").asLong()).isEqualTo(booked);
        assertThat(second.get("committedSlots").asInt()).isEqualTo(1);
        assertThat(second.get("editable").asBoolean()).isFalse();
        assertThat(second.get("notEditableReason").asText()).isEqualTo("BLOCK_COMMITTED");
        assertThat(second.toString()).doesNotContain("patient").doesNotContain("reason\":\"Control");

        mvc.perform(get("/api/v1/professional/calendar").header("Authorization", pro.token())
                        .param("from", day.toString()).param("to", day.plusDays(5).toString()).param("locationCode", "ICV"))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].locationCode").value("ICV"));
    }

    @Test
    void ca02IsolationAndRangeValidation() throws Exception {
        ProfessionalFixture pro = activeProfessional(List.of("MEDICINA_GENERAL"), List.of("HIC"));
        ProfessionalFixture other = activeProfessional(List.of("MEDICINA_GENERAL"), List.of("HIC"));
        LocalDate day = agendaToday().plusDays(3);
        createBlock(pro.token(), day, "08:00", "09:00", "HIC");

        mvc.perform(get("/api/v1/professional/calendar").header("Authorization", other.token())
                        .param("from", day.toString()).param("to", day.toString()).param("professionalId", Long.toString(pro.id())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/v1/professional/calendar").header("Authorization", pro.token())
                        .param("from", day.toString()).param("to", day.plusDays(31).toString()))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        mvc.perform(get("/api/v1/professional/calendar").header("Authorization", pro.token())
                        .param("from", day.toString()).param("to", day.minusDays(1).toString()))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/professional/calendar").header("Authorization", pro.token()).param("from", day.toString()))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        mvc.perform(get("/api/v1/professional/calendar").header("Authorization", pro.token())
                        .param("from", "no-date").param("to", day.toString()))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/professional/calendar").header("Authorization", userToken())
                        .param("from", day.toString()).param("to", day.toString()))
                .andExpect(status().isForbidden());
    }

    private long createBlock(String token, LocalDate date, String start, String end, String location) throws Exception {
        return body(mvc.perform(post("/api/v1/professional/blocks").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("date", date.toString(), "startTime", start, "endTime", end, "locationCode", location))))
                .andExpect(status().isCreated()).andReturn()).get("id").asLong();
    }
}
