package com.fcv.citas.it;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** HU-009 — especialidades: ADMIN exclusivo, duración 30/60, una general activa, baja lógica, lectura USER. */
class SpecialtyIT extends AbstractMySqlIT {

    @Test
    void ca01AdminCreatesAndEditsWithOnlyThirtyOrSixtyMinutes() throws Exception {
        String admin = adminToken();
        String code = ("ESP_" + unique()).toUpperCase();

        long id = body(mvc.perform(post("/api/v1/admin/specialties").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("code", code, "name", "Especialidad " + code, "durationMinutes", 60, "general", false))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.durationMinutes").value(60))
                .andExpect(jsonPath("$.requiresAdminApproval").value(true))
                .andExpect(jsonPath("$.active").value(true))
                .andReturn()).get("id").asLong();

        mvc.perform(post("/api/v1/admin/specialties").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("code", code + "X", "name", "Mala " + code, "durationMinutes", 45, "general", false))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field").value("durationMinutes"));
        mvc.perform(patch("/api/v1/admin/specialties/" + id).header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("name", "Especialidad " + code, "durationMinutes", 90, "active", true))))
                .andExpect(status().isBadRequest());
        mvc.perform(patch("/api/v1/admin/specialties/" + id).header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("name", "Especialidad " + code, "durationMinutes", 30, "active", true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.durationMinutes").value(30));
        mvc.perform(patch("/api/v1/admin/specialties/987654").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("name", "x", "durationMinutes", 30, "active", true))))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/admin/specialties").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("code", code, "name", "Otro nombre " + code, "durationMinutes", 30, "general", false))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_CODE"));
    }

    @Test
    void ca02DeactivationIsLogicalAndPublicListingOnlyShowsActive() throws Exception {
        String admin = adminToken();
        String code = ("BAJA_" + unique()).toUpperCase();
        long id = body(mvc.perform(post("/api/v1/admin/specialties").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("code", code, "name", "Baja " + code, "durationMinutes", 30, "general", false))))
                .andReturn()).get("id").asLong();

        mvc.perform(patch("/api/v1/admin/specialties/" + id).header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("name", "Baja " + code, "durationMinutes", 30, "active", false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM specialties WHERE id = ?", Integer.class, id)).isOne();
        mvc.perform(get("/api/v1/admin/specialties").header("Authorization", admin))
                .andExpect(jsonPath("$[*].code", hasItem(code)));
        mvc.perform(get("/api/v1/specialties").header("Authorization", userToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].code", not(hasItem(code))))
                .andExpect(jsonPath("$[*].code", hasItem("MEDICINA_GENERAL")))
                .andExpect(jsonPath("$[0].active").doesNotExist())
                .andExpect(jsonPath("$[*].durationMinutes", everyItem(org.hamcrest.Matchers.isOneOf(30, 60))));
    }

    @Test
    void onlyOneActiveGeneralSpecialty() throws Exception {
        String admin = adminToken();
        String code = ("GEN_" + unique()).toUpperCase();

        mvc.perform(post("/api/v1/admin/specialties").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("code", code, "name", "General " + code, "durationMinutes", 30, "general", true))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("GENERAL_SPECIALTY_CONFLICT"));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM specialties WHERE is_general = TRUE AND active = TRUE",
                Integer.class)).isOne();
    }

    @Test
    void adminEndpointsAreForbiddenForOtherRolesIncludingGetList() throws Exception {
        for (String token : new String[]{userToken(), tokenWithRoles("PROFESSIONAL")}) {
            mvc.perform(get("/api/v1/admin/specialties").header("Authorization", token))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("FORBIDDEN"));
            mvc.perform(post("/api/v1/admin/specialties").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(Map.of("code", "NOPE", "name", "Nope", "durationMinutes", 30, "general", false))))
                    .andExpect(status().isForbidden());
        }
        mvc.perform(get("/api/v1/specialties")).andExpect(status().isUnauthorized());
    }
}
