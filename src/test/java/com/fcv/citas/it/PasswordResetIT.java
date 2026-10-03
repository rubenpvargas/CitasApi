package com.fcv.citas.it;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HU-004 con la configuración por defecto ({@code expose-development-token=false}): no enumeración,
 * nunca se devuelve el token y solo se persiste su hash.
 */
class PasswordResetIT extends AbstractMySqlIT {

    @Test
    void requestIsIdentical202ForExistingAndUnknownEmailAndNeverReturnsToken() throws Exception {
        String email = registerUser();

        String existing = request(email).andExpect(status().isAccepted())
                .andExpect(jsonPath("$.developmentToken").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        String unknown = request("ghost." + unique() + "@example.test").andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString();

        assertThat(existing).isEqualTo(unknown);
        assertThat(json.readTree(existing).get("message").asText()).isNotBlank();
    }

    @Test
    void requestPersistsOnlyAHashedTokenWithExpiryForExistingAccount() throws Exception {
        String email = registerUser();

        request(email).andExpect(status().isAccepted());

        Map<String, Object> row = jdbc.queryForMap("SELECT t.token_hash, t.used_at, "
                + "TIMESTAMPDIFF(MINUTE, t.created_at, t.expires_at) AS ttl FROM password_reset_tokens t "
                + "JOIN users u ON u.id = t.user_id WHERE u.email = ?", email);
        assertThat((String) row.get("token_hash")).matches("[0-9a-f]{64}");
        assertThat(row.get("used_at")).isNull();
        assertThat(((Number) row.get("ttl")).intValue()).isEqualTo(30);
    }

    @Test
    void unknownTokenIs409InvalidResetToken() throws Exception {
        confirm("never-issued-" + unique(), "NuevaClave2026")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_RESET_TOKEN"));
    }

    @Test
    void weakNewPasswordIs400AndInvalidEmailIs400() throws Exception {
        confirm("whatever", "debil")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        request("not-an-email")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    private ResultActions request(String email) throws Exception {
        return mvc.perform(post("/api/v1/auth/password-reset/request").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email", email))));
    }

    private ResultActions confirm(String token, String newPassword) throws Exception {
        return mvc.perform(post("/api/v1/auth/password-reset/confirm").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("token", token, "newPassword", newPassword))));
    }
}
