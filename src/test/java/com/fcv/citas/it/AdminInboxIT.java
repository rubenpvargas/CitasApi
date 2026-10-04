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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** HU-025 — bandeja ADMIN: solo pendientes, forma {appointments, reschedules}, filtros y rol. */
class AdminInboxIT extends AbstractMySqlIT {

    @Test
    void ca01Ca02ShowsOnlyPendingItemsWithComparisonFieldsAndFilters() throws Exception {
        ProfessionalFixture pro = activeProfessional(List.of("MEDICINA_GENERAL", "CARDIOLOGIA_ADULTO"), List.of("HIC", "ICV"));
        LocalDate day = agendaToday().plusDays(10);
        publishBlock(pro.token(), day, "08:00", "10:00", "HIC");
        publishBlock(pro.token(), day.plusDays(1), "10:00", "10:30", "ICV");
        long cardio = jdbc.queryForObject("SELECT id FROM specialties WHERE code = 'CARDIOLOGIA_ADULTO'", Long.class);
        long requested = specialized(cardio, pro.id(), day + "T09:00:00");
        long decided = specialized(cardio, pro.id(), day + "T09:30:00");
        mvc.perform(post("/api/v1/admin/appointments/" + decided + "/decision").header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON).content(toJson(Map.of("approve", true))))
                .andExpect(status().isOk());
        String user = userToken();
        long approved = bookGeneral(user, pro.id(), "HIC", day.atTime(8, 0)).get("id").asLong();
        long requestId = body(mvc.perform(post("/api/v1/appointments/" + approved + "/reschedule").header("Authorization", user)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("startAt", day.plusDays(1) + "T10:00:00", "locationCode", "ICV"))))
                .andExpect(status().isCreated()).andReturn()).get("id").asLong();
        String admin = tokenOnlyRoles("ADMIN");

        JsonNode inbox = body(mvc.perform(get("/api/v1/admin/inbox").header("Authorization", admin)
                        .param("professionalId", Long.toString(pro.id())))
                .andExpect(status().isOk()).andReturn());

        assertThat(ids(inbox.get("appointments"))).containsExactly(requested);
        JsonNode appointment = inbox.get("appointments").get(0);
        assertThat(appointment.get("patientName").asText()).isEqualTo("Ana Sintetica");
        assertThat(appointment.get("specialtyName").asText()).isEqualTo("Cardiologia Adulto");
        assertThat(appointment.get("startAt").asText()).isEqualTo(day + "T09:00:00");
        assertThat(appointment.get("createdAt").asText()).isNotBlank();
        assertThat(ids(inbox.get("reschedules"))).containsExactly(requestId);
        JsonNode reschedule = inbox.get("reschedules").get(0);
        assertThat(reschedule.get("appointmentId").asLong()).isEqualTo(approved);
        assertThat(reschedule.get("currentStartAt").asText()).isEqualTo(day + "T08:00:00");
        assertThat(reschedule.get("currentEndAt").asText()).isEqualTo(day + "T08:30:00");
        assertThat(reschedule.get("requestedStartAt").asText()).isEqualTo(day.plusDays(1) + "T10:00:00");
        assertThat(reschedule.get("requestedEndAt").asText()).isEqualTo(day.plusDays(1) + "T10:30:00");
        assertThat(reschedule.get("locationCode").asText()).isEqualTo("ICV");
        assertThat(reschedule.get("professionalName").asText()).isEqualTo("Valeria Agenda");

        JsonNode byLocation = body(mvc.perform(get("/api/v1/admin/inbox").header("Authorization", admin)
                        .param("professionalId", Long.toString(pro.id())).param("locationCode", "icv"))
                .andReturn());
        assertThat(ids(byLocation.get("appointments"))).isEmpty();
        assertThat(ids(byLocation.get("reschedules"))).containsExactly(requestId);
        JsonNode bySpecialty = body(mvc.perform(get("/api/v1/admin/inbox").header("Authorization", admin)
                        .param("professionalId", Long.toString(pro.id())).param("specialtyId", Long.toString(cardio)))
                .andReturn());
        assertThat(ids(bySpecialty.get("reschedules"))).isEmpty();
        JsonNode byDate = body(mvc.perform(get("/api/v1/admin/inbox").header("Authorization", admin)
                        .param("professionalId", Long.toString(pro.id()))
                        .param("from", day.plusDays(2).toString()).param("to", day.plusDays(3).toString()))
                .andReturn());
        assertThat(ids(byDate.get("appointments"))).isEmpty();
        assertThat(ids(byDate.get("reschedules"))).isEmpty();
    }

    @Test
    void ca03OnlyAdmin() throws Exception {
        mvc.perform(get("/api/v1/admin/inbox").header("Authorization", userToken())).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/admin/inbox").header("Authorization", tokenOnlyRoles("PROFESSIONAL")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/admin/inbox")).andExpect(status().isUnauthorized());
    }

    private long specialized(long specialtyId, long professionalId, String startAt) throws Exception {
        return body(mvc.perform(post("/api/v1/appointments/specialized").header("Authorization", userToken())
                        .contentType(MediaType.APPLICATION_JSON).content(toJson(Map.of("specialtyId", specialtyId,
                                "professionalId", professionalId, "locationCode", "HIC", "startAt", startAt))))
                .andExpect(status().isCreated()).andReturn()).get("id").asLong();
    }

    private static List<Long> ids(JsonNode array) {
        List<Long> ids = new ArrayList<>();
        array.forEach(item -> ids.add(item.get("id").asLong()));
        return ids;
    }
}
