package com.fcv.citas.it;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** HU-013 — edición revalidada y baja lógica; pasado, comprometido o ajeno no se alteran. */
class AvailabilityBlockEditIT extends AbstractMySqlIT {

    @Test
    void ca01EditRevalidatesAndRegeneratesSlots() throws Exception {
        ProfessionalFixture pro = activeProfessional(List.of("MEDICINA_GENERAL"), List.of("HIC", "ICV"));
        LocalDate day = agendaToday().plusDays(7);
        long id = createBlock(pro.token(), day, "08:00", "09:00", "HIC");
        createBlock(pro.token(), day, "12:00", "13:00", "HIC");

        edit(pro.token(), id, day.plusDays(1), "14:00", "16:00", "ICV")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value(day.plusDays(1).toString()))
                .andExpect(jsonPath("$.locationCode").value("ICV"))
                .andExpect(jsonPath("$.totalSlots").value(4));
        assertThat(jdbc.queryForList("SELECT DATE_FORMAT(start_at, '%H:%i') FROM professional_slots "
                + "WHERE availability_block_id = ? ORDER BY start_at", String.class, id))
                .containsExactly("14:00", "14:30", "15:00", "15:30");

        edit(pro.token(), id, day, "11:30", "12:30", "ICV")
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("BLOCK_OVERLAP"));
        edit(pro.token(), id, agendaToday().minusDays(1), "08:00", "09:00", "HIC")
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("PAST_BLOCK"));
        edit(pro.token(), id, day, "08:45", "09:00", "HIC").andExpect(status().isBadRequest());
    }

    @Test
    void ca03CommittedByAppointmentOrRescheduleHoldCannotChange() throws Exception {
        ProfessionalFixture pro = activeProfessional(List.of("MEDICINA_GENERAL"), List.of("HIC"));
        LocalDate day = agendaToday().plusDays(8);
        long booked = createBlock(pro.token(), day, "08:00", "09:00", "HIC");
        long held = createBlock(pro.token(), day, "10:00", "11:00", "HIC");
        mvc.perform(post("/api/v1/appointments/general").header("Authorization", userToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("professionalId", pro.id(), "locationCode", "HIC",
                                "startAt", day + "T08:30:00", "reason", "Control sintetico"))))
                .andExpect(status().isCreated());
        jdbc.update("UPDATE professional_slots SET reschedule_request_id = ? WHERE availability_block_id = ? "
                + "ORDER BY start_at LIMIT 1", syntheticPendingRescheduleId(), held);

        for (long id : new long[]{booked, held}) {
            edit(pro.token(), id, day.plusDays(1), "08:00", "09:00", "HIC")
                    .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("BLOCK_COMMITTED"));
            mvc.perform(delete("/api/v1/professional/blocks/" + id).header("Authorization", pro.token()))
                    .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("BLOCK_COMMITTED"));
            assertThat(jdbc.queryForObject("SELECT active FROM availability_blocks WHERE id = ?", Boolean.class, id)).isTrue();
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM professional_slots WHERE availability_block_id = ?",
                    Integer.class, id)).isEqualTo(2);
        }
        jdbc.update("UPDATE professional_slots SET reschedule_request_id = NULL WHERE availability_block_id = ?", held);
    }

    @Test
    void ca03PastBlockIsImmutable() throws Exception {
        ProfessionalFixture pro = activeProfessional(List.of("MEDICINA_GENERAL"), List.of("HIC"));
        long id = createBlock(pro.token(), agendaToday().plusDays(9), "08:00", "09:00", "HIC");
        jdbc.update("UPDATE availability_blocks SET available_date = ? WHERE id = ?", agendaToday().minusDays(2), id);

        edit(pro.token(), id, agendaToday().plusDays(10), "08:00", "09:00", "HIC")
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("PAST_BLOCK"));
        mvc.perform(delete("/api/v1/professional/blocks/" + id).header("Authorization", pro.token()))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("PAST_BLOCK"));
    }

    @Test
    void ca02DeleteIsLogicalAndForeignBlocksAre404() throws Exception {
        ProfessionalFixture pro = activeProfessional(List.of("MEDICINA_GENERAL"), List.of("HIC"));
        ProfessionalFixture other = activeProfessional(List.of("MEDICINA_GENERAL"), List.of("HIC"));
        long id = createBlock(pro.token(), agendaToday().plusDays(11), "08:00", "09:00", "HIC");

        edit(other.token(), id, agendaToday().plusDays(12), "08:00", "09:00", "HIC")
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("NOT_FOUND"));
        mvc.perform(delete("/api/v1/professional/blocks/" + id).header("Authorization", other.token()))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/v1/professional/blocks/" + id).header("Authorization", userToken()))
                .andExpect(status().isForbidden());

        mvc.perform(delete("/api/v1/professional/blocks/" + id).header("Authorization", pro.token()))
                .andExpect(status().isNoContent());
        assertThat(jdbc.queryForObject("SELECT active FROM availability_blocks WHERE id = ?", Boolean.class, id)).isFalse();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM professional_slots WHERE availability_block_id = ?",
                Integer.class, id)).isZero();
        mvc.perform(delete("/api/v1/professional/blocks/" + id).header("Authorization", pro.token()))
                .andExpect(status().isNotFound());
    }

    private long createBlock(String token, LocalDate date, String start, String end, String location) throws Exception {
        return body(mvc.perform(post("/api/v1/professional/blocks").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("date", date.toString(), "startTime", start, "endTime", end, "locationCode", location))))
                .andExpect(status().isCreated()).andReturn()).get("id").asLong();
    }

    private ResultActions edit(String token, long id, LocalDate date, String start, String end, String location) throws Exception {
        return mvc.perform(patch("/api/v1/professional/blocks/" + id).header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(Map.of("date", date.toString(), "startTime", start, "endTime", end, "locationCode", location))));
    }
}
