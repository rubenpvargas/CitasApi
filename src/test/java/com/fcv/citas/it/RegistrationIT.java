package com.fcv.citas.it;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** HU-001 — contrato de registro contra MySQL real. */
class RegistrationIT extends AbstractMySqlIT {

    @Test
    void ca01ValidRegistrationReturns201AsUserWithoutPasswordInResponse() throws Exception {
        String suffix = unique();
        String email = "Reg." + suffix + "@Example.TEST";

        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(registrationBody(email, "R" + suffix))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.email").value(email.toLowerCase()))
                .andExpect(jsonPath("$.roles[0]").value("USER"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void ca03PasswordIsStoredAsAdaptiveHashNeverPlaintext() throws Exception {
        String email = registerUser();

        String stored = jdbc.queryForObject("SELECT password_hash FROM users WHERE email = ?", String.class, email);

        assertThat(stored).isNotEqualTo(STRONG_PASSWORD).doesNotContain(STRONG_PASSWORD);
        assertThat(stored).startsWith("$2");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM users u JOIN user_roles ur ON ur.user_id = u.id "
                + "JOIN roles r ON r.id = ur.role_id WHERE u.email = ? AND r.code = 'USER'", Integer.class, email))
                .isOne();
    }

    @Test
    void invalidFieldsReturn400ValidationErrorWithoutCreatingUser() throws Exception {
        String suffix = unique();
        Map<String, Object> body = new HashMap<>(registrationBody("not-an-email", "B" + suffix));
        body.put("firstName", "");

        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[*].field", hasItem("email")))
                .andExpect(jsonPath("$.errors[*].field", hasItem("firstName")));

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE document_number = ?",
                Integer.class, "B" + suffix)).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"Short1", "sinmayuscula2026", "SINNUMEROSaqui", "12345678"})
    void weakPasswordReturns400ValidationError(String weak) throws Exception {
        String suffix = unique();
        Map<String, Object> body = new HashMap<>(registrationBody("weak." + suffix + "@example.test", "W" + suffix));
        body.put("password", weak);

        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[*].field", hasItem("password")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(weak))));
    }

    @Test
    void malformedJsonReturns400InvalidRequest() throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content("{\"email\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void ca02DuplicateEmailIgnoringCaseReturns409WithoutCreatingDuplicate() throws Exception {
        String email = registerUser();
        String suffix = unique();

        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(registrationBody(email.toUpperCase(), "D" + suffix))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("IDENTIFIER_ALREADY_REGISTERED"));

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE LOWER(email) = ?", Integer.class, email))
                .isOne();
    }

    @Test
    void ca02DuplicateDocumentReturns409WithoutCreatingDuplicate() throws Exception {
        String suffix = unique();
        String document = "DOC" + suffix;
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(registrationBody("first." + suffix + "@example.test", document))))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(registrationBody("second." + suffix + "@example.test",
                                document.toLowerCase()))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("IDENTIFIER_ALREADY_REGISTERED"));

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE document_type = 'CC' AND document_number = ?",
                Integer.class, document.toUpperCase())).isOne();
    }
}
