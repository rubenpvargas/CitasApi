package com.fcv.citas.it;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** HU-024 — cierre de atención: solo cita propia aplicable, historial PROFESSIONAL. */
class CloseAppointmentIT extends AbstractMySqlIT {

    @Test
    void ca01ClosesStartedOwnAppointmentAndAuditsAsProfessional() throws Exception {
        ProfessionalFixture pro = activeProfessional(List.of("MEDICINA_GENERAL"), List.of("HIC"));
        LocalDate day = agendaToday().plusDays(3);
        publishBlock(pro.token(), day, "08:00", "09:00", "HIC");
        long completed = bookGeneral(userToken(), pro.id(), "HIC", day.atTime(8, 0)).get("id").asLong();
        long noShow = bookGeneral(userToken(), pro.id(), "HIC", day.atTime(8, 30)).get("id").asLong();
        moveToPast(completed);
        moveToPast(noShow);

        close(pro.token(), completed, "COMPLETED").andExpect(status().isOk()).andExpect(jsonPath("$.status").value("COMPLETED"));
        close(pro.token(), noShow, "NO_SHOW").andExpect(status().isOk()).andExpect(jsonPath("$.status").value("NO_SHOW"));

        assertThat(jdbc.queryForObject("SELECT h.change_source FROM appointment_status_history h JOIN appointment_statuses s "
                + "ON s.id = h.status_id WHERE h.appointment_id = ? AND s.code = 'COMPLETED'", String.class, completed))
                .isEqualTo("PROFESSIONAL");
        close(pro.token(), completed, "NO_SHOW").andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_TRANSITION"));
    }

    @Test
    void ca02FutureForeignOrInvalidOutcomeAreRejectedWithoutChange() throws Exception {
        ProfessionalFixture pro = activeProfessional(List.of("MEDICINA_GENERAL"), List.of("HIC"));
        ProfessionalFixture other = activeProfessional(List.of("MEDICINA_GENERAL"), List.of("HIC"));
        LocalDate day = agendaToday().plusDays(3);
        publishBlock(pro.token(), day, "10:00", "11:00", "HIC");
        long future = bookGeneral(userToken(), pro.id(), "HIC", day.atTime(10, 0)).get("id").asLong();
        long past = bookGeneral(userToken(), pro.id(), "HIC", day.atTime(10, 30)).get("id").asLong();
        moveToPast(past);

        close(pro.token(), future, "COMPLETED").andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_TRANSITION"));
        close(other.token(), past, "COMPLETED").andExpect(status().isNotFound());
        close(pro.token(), past, "CANCELLED").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        close(userToken(), past, "COMPLETED").andExpect(status().isForbidden());
        assertThat(jdbc.queryForObject("SELECT s.code FROM appointments a JOIN appointment_statuses s ON s.id = a.status_id "
                + "WHERE a.id = ?", String.class, past)).isEqualTo("APPROVED");
    }

    private void moveToPast(long id) {
        jdbc.update("UPDATE appointments SET scheduled_start_at = ?, scheduled_end_at = ? WHERE id = ?",
                agendaToday().minusDays(1).atTime(8, 0), agendaToday().minusDays(1).atTime(8, 30), id);
    }

    private ResultActions close(String token, long id, String outcome) throws Exception {
        return mvc.perform(post("/api/v1/professional/appointments/" + id + "/close").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content(toJson(Map.of("outcome", outcome))));
    }
}
