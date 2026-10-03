package com.fcv.citas.it;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** HU-010 — alta de profesional por ADMIN: 201, normalización, duplicados específicos, política, 403. */
class ProfessionalAdminIT extends AbstractMySqlIT {

    static Map<String, Object> professionalBody(String suffix) {
        Map<String, Object> body = new HashMap<>();
        body.put("firstName", "Valeria");
        body.put("lastName", "Sintetica");
        body.put("documentType", "CC");
        body.put("documentNumber", "PR" + suffix);
        body.put("email", "Prof." + suffix + "@Example.TEST");
        body.put("phone", "3000000000");
        body.put("password", "ClaveProf2026");
        body.put("professionalCode", "PC-" + suffix);
        body.put("licenseNumber", "RM-" + suffix);
        return body;
    }

    @Test
    void ca01AdminCreatesProfessionalWithRoleAndNormalizedEmail() throws Exception {
        String admin = adminToken();
        String suffix = unique();

        JsonNode created = body(mvc.perform(post("/api/v1/admin/professionals").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content(toJson(professionalBody(suffix))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(("prof." + suffix + "@example.test")))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.userId").isNumber())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andReturn());

        long userId = created.get("userId").asLong();
        assertThat(jdbc.queryForObject("SELECT r.code FROM user_roles ur JOIN roles r ON r.id = ur.role_id WHERE ur.user_id = ?",
                String.class, userId)).isEqualTo("PROFESSIONAL");
        assertThat(jdbc.queryForObject("SELECT password_hash FROM users WHERE id = ?", String.class, userId)).startsWith("$2");
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("email", "prof." + suffix + "@example.test", "password", "ClaveProf2026"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.roles", hasItem("PROFESSIONAL")));
        mvc.perform(get("/api/v1/admin/professionals").header("Authorization", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].professionalCode", hasItem(("PC-" + suffix).toUpperCase())))
                .andExpect(jsonPath("$[0].specialtyIds").isArray())
                .andExpect(jsonPath("$[0].locationIds").isArray())
                .andExpect(jsonPath("$[0].specialtyCodes").isArray())
                .andExpect(jsonPath("$[0].locationCodes").isArray());
    }

    @Test
    void duplicatesReturnSpecificCodes() throws Exception {
        String admin = adminToken();
        String suffix = unique();
        mvc.perform(post("/api/v1/admin/professionals").header("Authorization", admin)
                .contentType(MediaType.APPLICATION_JSON).content(toJson(professionalBody(suffix))))
                .andExpect(status().isCreated());

        expectDuplicate(admin, withChanges(unique(), Map.of("email", "PROF." + suffix + "@example.test")), "DUPLICATE_EMAIL");
        expectDuplicate(admin, withChanges(unique(), Map.of("documentNumber", ("pr" + suffix))), "DUPLICATE_DOCUMENT");
        expectDuplicate(admin, withChanges(unique(), Map.of("professionalCode", "pc-" + suffix)), "DUPLICATE_PROFESSIONAL_CODE");
        expectDuplicate(admin, withChanges(unique(), Map.of("licenseNumber", "RM-" + suffix)), "DUPLICATE_LICENSE");
    }

    @Test
    void weakPasswordIs400() throws Exception {
        Map<String, Object> body = professionalBody(unique());
        body.put("password", "debil");
        mvc.perform(post("/api/v1/admin/professionals").header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON).content(toJson(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field").value("password"));
    }

    @Test
    void ca02UserAndProfessionalCannotCreateProfessionals() throws Exception {
        for (String token : new String[]{userToken(), tokenWithRoles("PROFESSIONAL")}) {
            mvc.perform(post("/api/v1/admin/professionals").header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON).content(toJson(professionalBody(unique()))))
                    .andExpect(status().isForbidden());
            mvc.perform(get("/api/v1/admin/professionals").header("Authorization", token))
                    .andExpect(status().isForbidden());
        }
    }

    private Map<String, Object> withChanges(String suffix, Map<String, Object> changes) {
        Map<String, Object> body = professionalBody(suffix);
        body.putAll(changes);
        return body;
    }

    private void expectDuplicate(String admin, Map<String, Object> body, String code) throws Exception {
        mvc.perform(post("/api/v1/admin/professionals").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content(toJson(body)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(code));
    }
}
