package com.fcv.citas.it;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** HU-008 — EPS/planes ADMIN: alta 201, baja lógica, duplicados 409, 404 y 403 para otros roles. */
class InsuranceAdminIT extends AbstractMySqlIT {

    @Test
    void ca01AdminCreatesListsAndEditsEpsAndPlans() throws Exception {
        String admin = adminToken();
        String code = ("EPS_IT_" + unique()).toUpperCase();

        long epsId = body(mvc.perform(post("/api/v1/admin/eps").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content(toJson(Map.of("code", code, "name", "EPS IT"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.active").value(true))
                .andReturn()).get("id").asLong();
        long regimeId = jdbc.queryForObject("SELECT id FROM insurance_regimes WHERE code = 'SUBSIDIADO'", Long.class);

        long planId = body(mvc.perform(post("/api/v1/admin/eps/" + epsId + "/plans").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("regimeId", regimeId, "code", "PLAN_A", "name", "Plan A"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.epsId").value(epsId))
                .andExpect(jsonPath("$.regimeCode").value("SUBSIDIADO"))
                .andReturn()).get("id").asLong();

        mvc.perform(get("/api/v1/admin/plans").param("epsId", Long.toString(epsId)).header("Authorization", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(planId))
                .andExpect(jsonPath("$[0].epsCode").value(code));
        mvc.perform(patch("/api/v1/admin/plans/" + planId).header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content(toJson(Map.of("name", "Plan A2", "active", true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Plan A2"));
        mvc.perform(get("/api/v1/admin/eps").header("Authorization", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].code", hasItem(code)));
    }

    @Test
    void ca02RetiringReferencedEpsAndPlanIsLogical() throws Exception {
        String admin = adminToken();
        long epsId = jdbc.queryForObject("SELECT id FROM eps WHERE code = 'EPS_DEMO_A'", Long.class);
        long planId = jdbc.queryForObject("SELECT id FROM eps_plans WHERE code = 'DEMO-CONTRIB'", Long.class);
        String epsName = jdbc.queryForObject("SELECT name FROM eps WHERE id = ?", String.class, epsId);
        try {
            mvc.perform(patch("/api/v1/admin/plans/" + planId).header("Authorization", admin)
                            .contentType(MediaType.APPLICATION_JSON).content(toJson(Map.of("name", "Plan Contributivo Demo", "active", false))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.active").value(false));
            mvc.perform(patch("/api/v1/admin/eps/" + epsId).header("Authorization", admin)
                            .contentType(MediaType.APPLICATION_JSON).content(toJson(Map.of("name", epsName, "active", false))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.active").value(false));

            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM eps WHERE id = ?", Integer.class, epsId)).isOne();
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM eps_plans WHERE id = ?", Integer.class, planId)).isOne();
        } finally {
            jdbc.update("UPDATE eps SET active = TRUE WHERE id = ?", epsId);
            jdbc.update("UPDATE eps_plans SET active = TRUE WHERE id = ?", planId);
        }
    }

    @Test
    void duplicateCodesAre409AndUnknownIdsAre404() throws Exception {
        String admin = adminToken();
        long regimeId = jdbc.queryForObject("SELECT id FROM insurance_regimes WHERE code = 'CONTRIBUTIVO'", Long.class);
        long demoEps = jdbc.queryForObject("SELECT id FROM eps WHERE code = 'EPS_DEMO_A'", Long.class);

        mvc.perform(post("/api/v1/admin/eps").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("code", "eps_demo_a", "name", "Duplicada"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_CODE"));
        mvc.perform(post("/api/v1/admin/eps/" + demoEps + "/plans").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("regimeId", regimeId, "code", "DEMO-CONTRIB", "name", "Dup"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_CODE"));
        mvc.perform(patch("/api/v1/admin/eps/987654").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content(toJson(Map.of("name", "x", "active", true))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        mvc.perform(post("/api/v1/admin/eps/987654/plans").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("regimeId", regimeId, "code", "X", "name", "X"))))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/admin/eps").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("code", "", "name", ""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void ca03OtherRolesAreForbidden() throws Exception {
        for (String token : new String[]{userToken(), tokenWithRoles("PROFESSIONAL")}) {
            mvc.perform(get("/api/v1/admin/eps").header("Authorization", token)).andExpect(status().isForbidden());
            mvc.perform(get("/api/v1/admin/plans").header("Authorization", token)).andExpect(status().isForbidden());
            mvc.perform(post("/api/v1/admin/eps").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(Map.of("code", "NOPE", "name", "Nope"))))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM eps WHERE code = 'NOPE'", Integer.class)).isZero();
    }
}
