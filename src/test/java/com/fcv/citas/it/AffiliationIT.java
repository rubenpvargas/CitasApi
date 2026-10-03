package com.fcv.citas.it;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** HU-006 — afiliación vigente, reactivación, catálogo inactivo, integridad transaccional y lectura de EPS. */
class AffiliationIT extends AbstractMySqlIT {

    @Test
    void ca01SavingAndReselectingAPreviousCombinationReactivatesIt() throws Exception {
        String token = userToken();
        long planA = createPlan("REACT_A");
        long planB = createPlan("REACT_B");

        mvc.perform(get("/api/v1/me/affiliations").header("Authorization", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
        long firstId = body(save(token, planA, "M-1").andExpect(status().isOk())
                .andExpect(jsonPath("$.planId").value(planA))
                .andExpect(jsonPath("$.current").value(true))
                .andExpect(jsonPath("$.regimeCode").value("CONTRIBUTIVO"))
                .andReturn()).get("id").asLong();
        save(token, planB, "M-2").andExpect(status().isOk()).andExpect(jsonPath("$.planId").value(planB));

        save(token, planA, "M-1").andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(firstId))
                .andExpect(jsonPath("$.current").value(true));

        mvc.perform(get("/api/v1/me/affiliations").header("Authorization", token))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].planId").value(planA))
                .andExpect(jsonPath("$[0].epsCode").isString());
        long userId = jdbc.queryForObject("SELECT user_id FROM user_insurance_affiliations WHERE id = ?", Long.class, firstId);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM user_insurance_affiliations WHERE user_id = ?", Integer.class, userId))
                .isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM user_insurance_affiliations WHERE user_id = ? AND is_current = TRUE",
                Integer.class, userId)).isOne();
    }

    @Test
    void ca02InactivePlanIs409AndKeepsThePreviousAffiliation() throws Exception {
        String token = userToken();
        long active = createPlan("KEEP_A");
        long inactive = createPlan("KEEP_OFF");
        jdbc.update("UPDATE eps_plans SET active = FALSE WHERE id = ?", inactive);
        save(token, active, "K-1").andExpect(status().isOk());

        save(token, inactive, "K-2")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CATALOG_INACTIVE"));
        save(token, 987654L, "K-3").andExpect(status().isNotFound());

        mvc.perform(get("/api/v1/me/affiliations").header("Authorization", token))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].planId").value(active));
    }

    @Test
    void invalidBodyIs400AndOtherRolesAreForbidden() throws Exception {
        String token = userToken();
        mvc.perform(put("/api/v1/me/affiliations").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("membershipNumber", ""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        String professionalOnly = professionalOnlyToken();
        mvc.perform(get("/api/v1/me/affiliations").header("Authorization", professionalOnly))
                .andExpect(status().isForbidden());
    }

    @Test
    void activeInsuranceCatalogListsOnlyActiveEpsAndPlans() throws Exception {
        long active = createPlan("CAT_ON");
        long inactive = createPlan("CAT_OFF");
        jdbc.update("UPDATE eps_plans SET active = FALSE WHERE id = ?", inactive);

        mvc.perform(get("/api/v1/insurance/eps").header("Authorization", userToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].code", hasItem("EPS_DEMO_A")))
                .andExpect(jsonPath("$[*].plans[*].id", hasItem((int) active)))
                .andExpect(jsonPath("$[*].plans[*].id", not(hasItem((int) inactive))))
                .andExpect(jsonPath("$[0].plans[0].regime.code").isString())
                .andExpect(jsonPath("$[0].active").doesNotExist());
        mvc.perform(get("/api/v1/insurance/eps")).andExpect(status().isUnauthorized());
    }

    private org.springframework.test.web.servlet.ResultActions save(String token, long planId, String membership) throws Exception {
        return mvc.perform(put("/api/v1/me/affiliations").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                .content(toJson(Map.of("planId", planId, "membershipNumber", membership))));
    }

    private long createPlan(String prefix) {
        long epsId = jdbc.queryForObject("SELECT id FROM eps WHERE code = 'EPS_DEMO_B'", Long.class);
        String code = (prefix + "_" + unique()).toUpperCase();
        jdbc.update("INSERT INTO eps_plans(eps_id, regime_id, code, name, active) "
                + "SELECT ?, id, ?, ?, TRUE FROM insurance_regimes WHERE code = 'CONTRIBUTIVO'", epsId, code, "Plan " + code);
        return jdbc.queryForObject("SELECT id FROM eps_plans WHERE eps_id = ? AND code = ?", Long.class, epsId, code);
    }

    /** Usuario solo con rol PROFESSIONAL (sin USER). */
    private String professionalOnlyToken() throws Exception {
        String email = registerUser();
        jdbc.update("DELETE ur FROM user_roles ur JOIN users u ON u.id = ur.user_id WHERE u.email = ?", email);
        jdbc.update("INSERT INTO user_roles(user_id, role_id) SELECT u.id, r.id FROM users u, roles r "
                + "WHERE u.email = ? AND r.code = 'PROFESSIONAL'", email);
        return bearer(login(email, STRONG_PASSWORD).get("accessToken").asText());
    }
}
