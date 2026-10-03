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

/** HU-019 — mis citas: campos mínimos, motivo de rechazo, filtros y ownership. */
class MyAppointmentsIT extends AbstractMySqlIT {

    @Test
    void ca01Ca02ListsOnlyOwnAppointmentsWithFiltersAndRejectionReason() throws Exception {
        ProfessionalFixture pro = activeProfessional(List.of("MEDICINA_GENERAL", "CARDIOLOGIA_ADULTO"), List.of("HIC"));
        LocalDate day = agendaToday().plusDays(4);
        publishBlock(pro.token(), day, "08:00", "10:00", "HIC");
        String user = userToken();
        long approved = bookGeneral(user, pro.id(), "HIC", day.atTime(8, 0)).get("id").asLong();
        long cardio = jdbc.queryForObject("SELECT id FROM specialties WHERE code = 'CARDIOLOGIA_ADULTO'", Long.class);
        long rejected = body(mvc.perform(post("/api/v1/appointments/specialized").header("Authorization", user)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("specialtyId", cardio, "professionalId", pro.id(), "locationCode", "HIC",
                                "startAt", day + "T09:00:00"))))
                .andExpect(status().isCreated()).andReturn()).get("id").asLong();
        mvc.perform(post("/api/v1/admin/appointments/" + rejected + "/decision").header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON).content(toJson(Map.of("approve", false, "reason", "Motivo sintetico"))))
                .andExpect(status().isOk());
        bookGeneral(userToken(), pro.id(), "HIC", day.atTime(8, 30));

        JsonNode all = body(mvc.perform(get("/api/v1/appointments").header("Authorization", user))
                .andExpect(status().isOk()).andReturn());
        assertThat(all).hasSize(2);
        assertThat(all.get(0).get("id").asLong()).isEqualTo(approved);
        assertThat(all.get(0).fieldNames()).toIterable().containsExactlyInAnyOrder("id", "status", "locationCode",
                "locationName", "professionalId", "professionalName", "specialtyId", "specialtyName", "startAt", "endAt",
                "durationMinutes", "reason", "rejectionReason", "pendingReschedule", "cancellable", "reschedulable");
        assertThat(all.get(1).get("rejectionReason").asText()).isEqualTo("Motivo sintetico");

        mvc.perform(get("/api/v1/appointments").header("Authorization", user).param("status", "REJECTED"))
                .andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].id").value(rejected));
        mvc.perform(get("/api/v1/appointments").header("Authorization", user)
                        .param("from", day.plusDays(1).toString()).param("to", day.plusDays(3).toString()))
                .andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/v1/appointments").header("Authorization", user).param("status", "NOPE"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void ca03DetailOfAnotherAccountIsNotFoundAndOtherRolesAreForbidden() throws Exception {
        ProfessionalFixture pro = activeProfessional(List.of("MEDICINA_GENERAL"), List.of("HIC"));
        LocalDate day = agendaToday().plusDays(5);
        publishBlock(pro.token(), day, "08:00", "08:30", "HIC");
        String owner = userToken();
        long id = bookGeneral(owner, pro.id(), "HIC", day.atTime(8, 0)).get("id").asLong();

        mvc.perform(get("/api/v1/appointments/" + id).header("Authorization", owner))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id));
        mvc.perform(get("/api/v1/appointments/" + id).header("Authorization", userToken()))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("NOT_FOUND"));
        mvc.perform(get("/api/v1/appointments").header("Authorization", tokenOnlyRoles("ADMIN")))
                .andExpect(status().isForbidden());
    }
}
