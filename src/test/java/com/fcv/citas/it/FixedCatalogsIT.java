package com.fcv.citas.it;

import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.empty;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** HU-007 — revalidación de catálogos fijos tras V1..V8 sobre base migrada desde vacío. */
class FixedCatalogsIT extends AbstractMySqlIT {

    @Test
    void withoutTokenIs401() throws Exception {
        mvc.perform(get("/api/v1/catalogs"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void ca01Ca02AuthenticatedConsumerGetsTheFiveSeededCatalogs() throws Exception {
        String access = login(registerUser(), STRONG_PASSWORD).get("accessToken").asText();

        mvc.perform(get("/api/v1/catalogs").header("Authorization", bearer(access)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles[*].code", containsInAnyOrder("USER", "PROFESSIONAL", "ADMIN")))
                .andExpect(jsonPath("$.appointmentStatuses", not(empty())))
                .andExpect(jsonPath("$.appointmentStatuses[0].terminal").isBoolean())
                .andExpect(jsonPath("$.rescheduleRequestStatuses", not(empty())))
                .andExpect(jsonPath("$.insuranceRegimes", not(empty())))
                .andExpect(jsonPath("$.insuranceRegimes[0].id").isNumber())
                .andExpect(jsonPath("$.locations[*].code", hasItems("HIC", "ICV")));
    }

    @Test
    void ca02WriteOperationIs405() throws Exception {
        String access = login(registerUser(), STRONG_PASSWORD).get("accessToken").asText();

        mvc.perform(post("/api/v1/catalogs").header("Authorization", bearer(access)))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
    }
}
