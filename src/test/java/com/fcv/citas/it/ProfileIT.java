package com.fcv.citas.it;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** HU-005 — perfil propio con ownership por sub, validación y campos no editables. */
class ProfileIT extends AbstractMySqlIT {

    @Test
    void ca01GetReturnsOnlyTheOwnersAllowedFields() throws Exception {
        String email = registerUser();
        String token = bearer(login(email, STRONG_PASSWORD).get("accessToken").asText());

        JsonNode me = body(mvc.perform(get("/api/v1/me").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.roles", hasItem("USER")))
                .andExpect(jsonPath("$.documentType").value("CC"))
                .andReturn());

        assertThat(me.has("passwordHash")).isFalse();
        assertThat(me.has("password")).isFalse();
        assertThat(me.fieldNames()).toIterable().containsExactlyInAnyOrder(
                "id", "firstName", "lastName", "email", "documentType", "documentNumber", "phone", "roles");
    }

    @Test
    void ca02PatchPersistsContactFieldsAndIgnoresEmailAndDocument() throws Exception {
        String email = registerUser();
        String token = bearer(login(email, STRONG_PASSWORD).get("accessToken").asText());
        String document = jdbc.queryForObject("SELECT document_number FROM users WHERE email = ?", String.class, email);

        mvc.perform(patch("/api/v1/me").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("firstName", " Lucia ", "lastName", "Sintetica Ruiz",
                                "phone", "3115550000", "email", "hijack@example.test", "documentNumber", "X1", "id", 1))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Lucia"))
                .andExpect(jsonPath("$.lastName").value("Sintetica Ruiz"))
                .andExpect(jsonPath("$.phone").value("3115550000"))
                .andExpect(jsonPath("$.email").value(email));

        Map<String, Object> row = jdbc.queryForMap("SELECT first_name, email, document_number FROM users WHERE email = ?", email);
        assertThat(row.get("first_name")).isEqualTo("Lucia");
        assertThat(row.get("document_number")).isEqualTo(document);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE email = 'hijack@example.test'", Integer.class)).isZero();
        mvc.perform(get("/api/v1/me").header("Authorization", token)).andExpect(jsonPath("$.firstName").value("Lucia"));
    }

    @Test
    void ca03InvalidDataIs400WithoutModifyingTheProfile() throws Exception {
        String email = registerUser();
        String token = bearer(login(email, STRONG_PASSWORD).get("accessToken").asText());

        mvc.perform(patch("/api/v1/me").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("firstName", "", "lastName", "x".repeat(101), "phone", "3115550000"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        assertThat(jdbc.queryForObject("SELECT first_name FROM users WHERE email = ?", String.class, email)).isEqualTo("Ana");
    }

    @Test
    void ca03ProfileOfAnotherUserIsNeverReturned() throws Exception {
        String first = registerUser();
        String second = registerUser();
        String secondToken = bearer(login(second, STRONG_PASSWORD).get("accessToken").asText());

        mvc.perform(get("/api/v1/me").header("Authorization", secondToken))
                .andExpect(jsonPath("$.email").value(second));
        mvc.perform(get("/api/v1/me/" + first).header("Authorization", secondToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void withoutTokenIs401() throws Exception {
        mvc.perform(get("/api/v1/me")).andExpect(status().isUnauthorized());
        mvc.perform(patch("/api/v1/me").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }
}
