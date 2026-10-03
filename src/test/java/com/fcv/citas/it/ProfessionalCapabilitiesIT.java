package com.fcv.citas.it;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** HU-011 — capacidades del profesional y exclusión del profesional inactivo en la reserva. */
class ProfessionalCapabilitiesIT extends AbstractMySqlIT {

    @Test
    void ca01AdminAssignsCapabilitiesAndListingReturnsArrays() throws Exception {
        String admin = adminToken();
        long id = createProfessional(admin);
        long general = specialtyId("MEDICINA_GENERAL");
        long cardio = specialtyId("CARDIOLOGIA_ADULTO");
        long hic = locationId("HIC");
        long icv = locationId("ICV");

        capabilities(admin, id, List.of(general, cardio), cardio, List.of(hic, icv), true)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.primarySpecialtyId").value(cardio))
                .andExpect(jsonPath("$.specialtyIds", containsInAnyOrder((int) general, (int) cardio)))
                .andExpect(jsonPath("$.locationCodes", containsInAnyOrder("HIC", "ICV")));

        mvc.perform(get("/api/v1/admin/professionals").header("Authorization", admin))
                .andExpect(jsonPath("$[?(@.id == " + id + ")].primarySpecialtyId", hasItem((int) cardio)));
        mvc.perform(get("/api/v1/admin/locations").header("Authorization", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].code", hasItem("HIC")))
                .andExpect(jsonPath("$[0].city").isString());
    }

    @Test
    void ca02PrimaryNotAssignedInactiveCatalogUnknownAndEmptyLists() throws Exception {
        String admin = adminToken();
        long id = createProfessional(admin);
        long general = specialtyId("MEDICINA_GENERAL");
        long cardio = specialtyId("CARDIOLOGIA_ADULTO");
        long hic = locationId("HIC");
        long inactiveSpecialty = inactiveSpecialty();

        capabilities(admin, id, List.of(general), cardio, List.of(hic), true)
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("PRIMARY_NOT_ASSIGNED"));
        capabilities(admin, id, List.of(inactiveSpecialty), inactiveSpecialty, List.of(hic), true)
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("CATALOG_INACTIVE"));
        capabilities(admin, id, List.of(987654L), 987654L, List.of(hic), true).andExpect(status().isNotFound());
        capabilities(admin, 987654L, List.of(general), general, List.of(hic), true).andExpect(status().isNotFound());
        capabilities(admin, id, List.of(), general, List.of(hic), true)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        capabilities(admin, id, List.of(general), general, List.of(), true).andExpect(status().isBadRequest());
    }

    @Test
    void otherRolesAreForbidden() throws Exception {
        String admin = adminToken();
        long id = createProfessional(admin);
        long general = specialtyId("MEDICINA_GENERAL");
        for (String token : new String[]{userToken(), tokenWithRoles("PROFESSIONAL")}) {
            capabilities(token, id, List.of(general), general, List.of(locationId("HIC")), true)
                    .andExpect(status().isForbidden());
            mvc.perform(get("/api/v1/admin/locations").header("Authorization", token)).andExpect(status().isForbidden());
        }
    }

    @Test
    void ca03InactiveProfessionalCannotBeBooked() throws Exception {
        jdbc.update("UPDATE professionals SET active = FALSE WHERE id = 9001");
        try {
            mvc.perform(post("/api/v1/appointments/general").header("Authorization", userToken())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(Map.of("professionalId", 9001, "locationCode", "HIC",
                                    "startAt", LocalDate.now().plusDays(1) + "T08:00:00", "reason", "Control sintetico"))))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("PROFESSIONAL_INACTIVE"));
        } finally {
            jdbc.update("UPDATE professionals SET active = TRUE WHERE id = 9001");
        }
    }

    protected long createProfessional(String admin) throws Exception {
        return body(mvc.perform(post("/api/v1/admin/professionals").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content(toJson(ProfessionalAdminIT.professionalBody(unique()))))
                .andExpect(status().isCreated()).andReturn()).get("id").asLong();
    }

    private org.springframework.test.web.servlet.ResultActions capabilities(String token, long id, List<Long> specialtyIds,
                                                                            long primary, List<Long> locationIds,
                                                                            boolean active) throws Exception {
        return mvc.perform(put("/api/v1/admin/professionals/" + id + "/capabilities").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(Map.of("specialtyIds", specialtyIds, "primarySpecialtyId", primary,
                        "locationIds", locationIds, "active", active))));
    }

    private long specialtyId(String code) {
        return jdbc.queryForObject("SELECT id FROM specialties WHERE code = ?", Long.class, code);
    }

    private long locationId(String code) {
        return jdbc.queryForObject("SELECT id FROM locations WHERE code = ?", Long.class, code);
    }

    private long inactiveSpecialty() {
        String code = ("INACT_" + unique()).toUpperCase();
        jdbc.update("INSERT INTO specialties(code, name, appointment_duration_minutes, is_general, requires_admin_approval, active) "
                + "VALUES (?, ?, 30, FALSE, TRUE, FALSE)", code, "Inactiva " + code);
        return specialtyId(code);
    }
}
