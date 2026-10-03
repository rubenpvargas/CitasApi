package com.fcv.citas.it;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** HU-015 — disponibilidad real: consecutividad por duración, exclusiones y filtros contra MySQL. */
class AvailabilityQueryIT extends AbstractMySqlIT {

    @Test
    void ca02SixtyMinuteSpecialtyNeedsTwoConsecutiveFreeSlotsInTheSameBlock() throws Exception {
        ProfessionalFixture pro = activeProfessional(List.of("ORTOPEDIA_TRAUMATOLOGIA"), List.of("HIC"));
        LocalDate day = agendaToday().plusDays(2);
        long block = createBlock(pro.token(), day, "08:00", "10:00", "HIC");
        createBlock(pro.token(), day, "10:00", "10:30", "HIC");
        // Retención de reprogramación sobre el slot 08:30: rompe 08:00 y 08:30 como inicios de 60 min.
        jdbc.update("UPDATE professional_slots SET reschedule_request_id = 999998 WHERE availability_block_id = ? "
                + "AND start_at = ?", block, day.atTime(8, 30));

        JsonNode offers = search(Map.of("specialtyId", specialtyId("ORTOPEDIA_TRAUMATOLOGIA"), "from", day, "to", day,
                "professionalId", pro.id()));

        assertThat(starts(offers)).containsExactly(day + "T09:00:00");
        JsonNode offer = offers.get(0);
        assertThat(offer.get("endAt").asText()).isEqualTo(day + "T10:00:00");
        assertThat(offer.get("durationMinutes").asInt()).isEqualTo(60);
        assertThat(offer.get("locationCode").asText()).isEqualTo("HIC");
        assertThat(offer.get("professionalName").asText()).isEqualTo("Valeria Agenda");
        assertThat(offer.get("type").asText()).isEqualTo("SPECIALIZED");
        jdbc.update("UPDATE professional_slots SET reschedule_request_id = NULL WHERE availability_block_id = ?", block);
    }

    @Test
    void ca01ThirtyMinuteSearchHonoursLocationAndProfessionalFiltersAndExcludesBookedSlots() throws Exception {
        ProfessionalFixture pro = activeProfessional(List.of("MEDICINA_GENERAL"), List.of("HIC", "ICV"));
        LocalDate day = agendaToday().plusDays(3);
        createBlock(pro.token(), day, "08:00", "09:00", "HIC");
        createBlock(pro.token(), day, "14:00", "14:30", "ICV");
        mvc.perform(post("/api/v1/appointments/general").header("Authorization", userToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("professionalId", pro.id(), "locationCode", "HIC",
                                "startAt", day + "T08:00:00", "reason", "Control sintetico"))))
                .andExpect(status().isOk());
        long general = specialtyId("MEDICINA_GENERAL");

        assertThat(starts(search(Map.of("specialtyId", general, "from", day, "to", day, "professionalId", pro.id()))))
                .containsExactly(day + "T08:30:00", day + "T14:00:00");
        JsonNode icv = search(Map.of("specialtyId", general, "from", day, "to", day, "professionalId", pro.id(),
                "locationCode", "ICV"));
        assertThat(starts(icv)).containsExactly(day + "T14:00:00");
        assertThat(icv.get(0).get("type").asText()).isEqualTo("GENERAL");
    }

    @Test
    void ca03InactiveProfessionalUnassignedSpecialtyAndInactiveSpecialtyAreExcluded() throws Exception {
        ProfessionalFixture pro = activeProfessional(List.of("MEDICINA_GENERAL"), List.of("HIC"));
        LocalDate day = agendaToday().plusDays(4);
        createBlock(pro.token(), day, "08:00", "09:00", "HIC");
        long general = specialtyId("MEDICINA_GENERAL");
        assertThat(search(Map.of("specialtyId", general, "from", day, "to", day, "professionalId", pro.id()))).hasSize(2);

        assertThat(search(Map.of("specialtyId", specialtyId("CARDIOLOGIA_ADULTO"), "from", day, "to", day,
                "professionalId", pro.id()))).isEmpty();
        jdbc.update("UPDATE professionals SET active = FALSE WHERE id = ?", pro.id());
        assertThat(search(Map.of("specialtyId", general, "from", day, "to", day, "professionalId", pro.id()))).isEmpty();
        jdbc.update("UPDATE professionals SET active = TRUE WHERE id = ?", pro.id());
        jdbc.update("UPDATE professional_specialties SET active = FALSE WHERE professional_id = ?", pro.id());
        assertThat(search(Map.of("specialtyId", general, "from", day, "to", day, "professionalId", pro.id()))).isEmpty();
    }

    @Test
    void validationAndSecurity() throws Exception {
        String token = userToken();
        LocalDate day = agendaToday();
        mvc.perform(get("/api/v1/availability").header("Authorization", token)
                        .param("specialtyId", "1").param("from", day.toString()).param("to", day.plusDays(31).toString()))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        mvc.perform(get("/api/v1/availability").header("Authorization", token)
                        .param("from", day.toString()).param("to", day.toString()))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        mvc.perform(get("/api/v1/availability").header("Authorization", token)
                        .param("specialtyId", "987654").param("from", day.toString()).param("to", day.toString()))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/availability")
                        .param("specialtyId", "1").param("from", day.toString()).param("to", day.toString()))
                .andExpect(status().isUnauthorized());
    }

    private JsonNode search(Map<String, Object> params) throws Exception {
        var request = get("/api/v1/availability").header("Authorization", userToken());
        params.forEach((k, v) -> request.param(k, v.toString()));
        return body(mvc.perform(request).andExpect(status().isOk()).andReturn());
    }

    private static List<String> starts(JsonNode offers) {
        List<String> result = new ArrayList<>();
        offers.forEach(o -> result.add(o.get("startAt").asText()));
        return result;
    }

    private long specialtyId(String code) {
        return jdbc.queryForObject("SELECT id FROM specialties WHERE code = ?", Long.class, code);
    }

    private long createBlock(String token, LocalDate date, String start, String end, String location) throws Exception {
        return body(mvc.perform(post("/api/v1/professional/blocks").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("date", date.toString(), "startTime", start, "endTime", end, "locationCode", location))))
                .andExpect(status().isCreated()).andReturn()).get("id").asLong();
    }
}
