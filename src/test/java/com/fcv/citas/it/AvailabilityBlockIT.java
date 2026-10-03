package com.fcv.citas.it;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** HU-012 — publicación de bloques contra MySQL real, incluida la hora local de pared sin desplazamiento. */
class AvailabilityBlockIT extends AbstractMySqlIT {

    @Test
    void ca01PublishesBlockAndStoresSlotsAtTheSameWallClockTime() throws Exception {
        ProfessionalFixture pro = activeProfessional(List.of("MEDICINA_GENERAL"), List.of("HIC"));
        LocalDate day = agendaToday().plusDays(3);

        long blockId = body(create(pro.token(), day, "08:00", "09:00", "HIC")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.date").value(day.toString()))
                .andExpect(jsonPath("$.startTime").value("08:00"))
                .andExpect(jsonPath("$.endTime").value("09:00"))
                .andExpect(jsonPath("$.locationCode").value("HIC"))
                .andExpect(jsonPath("$.totalSlots").value(2))
                .andExpect(jsonPath("$.committedSlots").value(0))
                .andReturn()).get("id").asLong();

        // 08:00 Bogotá se lee como 08:00 directamente en MySQL (sin el desplazamiento de 5 h de Timestamp).
        assertThat(jdbc.queryForObject("SELECT TIME_FORMAT(start_time, '%H:%i') FROM availability_blocks WHERE id = ?",
                String.class, blockId)).isEqualTo("08:00");
        List<String> slotStarts = jdbc.queryForList("SELECT DATE_FORMAT(start_at, '%Y-%m-%dT%H:%i') FROM professional_slots "
                + "WHERE availability_block_id = ? ORDER BY start_at", String.class, blockId);
        assertThat(slotStarts).containsExactly(day + "T08:00", day + "T08:30");
        assertThat(jdbc.queryForObject("SELECT end_at FROM professional_slots WHERE availability_block_id = ? "
                + "ORDER BY start_at DESC LIMIT 1", LocalDateTime.class, blockId)).isEqualTo(LocalDateTime.of(day, LocalTime.of(9, 0)));
    }

    @Test
    void bookingOnAPublishedBlockKeepsTheSameWallClockTimeEndToEnd() throws Exception {
        ProfessionalFixture pro = activeProfessional(List.of("MEDICINA_GENERAL"), List.of("HIC"));
        LocalDate day = agendaToday().plusDays(6);
        long blockId = body(create(pro.token(), day, "08:00", "09:00", "HIC").andExpect(status().isCreated()).andReturn())
                .get("id").asLong();

        mvc.perform(post("/api/v1/appointments/general").header("Authorization", userToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("professionalId", pro.id(), "locationCode", "HIC",
                                "startAt", day + "T08:00:00", "reason", "Control sintetico"))))
                .andExpect(status().isCreated());

        assertThat(jdbc.queryForObject("SELECT DATE_FORMAT(a.scheduled_start_at, '%Y-%m-%dT%H:%i') FROM appointments a "
                + "WHERE a.professional_id = ?", String.class, pro.id())).isEqualTo(day + "T08:00");
        assertThat(jdbc.queryForList("SELECT DATE_FORMAT(start_at, '%H:%i') FROM professional_slots WHERE availability_block_id = ? "
                + "AND appointment_id IS NOT NULL", String.class, blockId)).containsExactly("08:00");
    }

    @Test
    void ca02PastOverlapAcrossLocationsAndUnassignedLocationAreRejectedWithoutChanges() throws Exception {
        ProfessionalFixture pro = activeProfessional(List.of("MEDICINA_GENERAL"), List.of("HIC", "ICV"));
        LocalDate day = agendaToday().plusDays(4);
        create(pro.token(), day, "08:00", "10:00", "HIC").andExpect(status().isCreated());

        create(pro.token(), day, "09:30", "11:00", "ICV")
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("BLOCK_OVERLAP"));
        create(pro.token(), agendaToday().minusDays(1), "08:00", "09:00", "HIC")
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("PAST_BLOCK"));
        create(pro.token(), day, "10:00", "11:00", "ICV").andExpect(status().isCreated());

        ProfessionalFixture onlyHic = activeProfessional(List.of("MEDICINA_GENERAL"), List.of("HIC"));
        create(onlyHic.token(), day, "08:00", "09:00", "ICV")
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("LOCATION_NOT_ASSIGNED"));
        create(onlyHic.token(), day, "08:00", "09:00", "NOPE").andExpect(status().isNotFound());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM availability_blocks WHERE professional_id = ?", Integer.class,
                pro.id())).isEqualTo(2);
    }

    @Test
    void alignmentAndRangeAre400() throws Exception {
        ProfessionalFixture pro = activeProfessional(List.of("MEDICINA_GENERAL"), List.of("HIC"));
        LocalDate day = agendaToday().plusDays(5);

        create(pro.token(), day, "08:15", "09:00", "HIC")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field").value("startTime"));
        create(pro.token(), day, "09:00", "08:00", "HIC")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors[0].field").value("endTime"));
        mvc.perform(post("/api/v1/professional/blocks").header("Authorization", pro.token())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"date\":\"2026-13-40\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void inactiveProfessionalCannotPublish() throws Exception {
        ProfessionalFixture pro = activeProfessional(List.of("MEDICINA_GENERAL"), List.of("HIC"));
        jdbc.update("UPDATE professionals SET active = FALSE WHERE id = ?", pro.id());

        create(pro.token(), agendaToday().plusDays(3), "08:00", "09:00", "HIC")
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("PROFESSIONAL_INACTIVE"));
    }

    @Test
    void ca03OnlyProfessionalsPublishAndTheActorComesFromTheToken() throws Exception {
        LocalDate day = agendaToday().plusDays(3);
        create(userToken(), day, "08:00", "09:00", "HIC").andExpect(status().isForbidden());
        create(adminToken(), day, "08:00", "09:00", "HIC").andExpect(status().isForbidden());
        ProfessionalFixture pro = activeProfessional(List.of("MEDICINA_GENERAL"), List.of("HIC"));
        ProfessionalFixture other = activeProfessional(List.of("MEDICINA_GENERAL"), List.of("HIC"));

        // Un professionalId ajeno en el cuerpo se ignora: el bloque queda a nombre del titular del JWT.
        long blockId = body(mvc.perform(post("/api/v1/professional/blocks").header("Authorization", pro.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("date", day.toString(), "startTime", "08:00", "endTime", "09:00",
                                "locationCode", "HIC", "professionalId", other.id()))))
                .andExpect(status().isCreated()).andReturn()).get("id").asLong();
        assertThat(jdbc.queryForObject("SELECT professional_id FROM availability_blocks WHERE id = ?", Long.class, blockId))
                .isEqualTo(pro.id());
        // Rol PROFESSIONAL sin perfil profesional -> 403.
        create(tokenWithRoles("PROFESSIONAL"), day, "08:00", "09:00", "HIC").andExpect(status().isForbidden());
    }

    protected ResultActions create(String token, LocalDate date, String start, String end, String location) throws Exception {
        return mvc.perform(post("/api/v1/professional/blocks").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(Map.of("date", date.toString(), "startTime", start, "endTime", end, "locationCode", location))));
    }
}
