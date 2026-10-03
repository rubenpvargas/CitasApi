package com.fcv.citas.it;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** HU-003 — rotación de refresh, rechazo de reuso y revocación por logout contra MySQL real. */
class RefreshAndLogoutIT extends AbstractMySqlIT {

    @Test
    void ca01RefreshRotatesTokensAndNewAccessIsUsable() throws Exception {
        JsonNode session = login(registerUser(), STRONG_PASSWORD);
        String originalRefresh = session.get("refreshToken").asText();

        JsonNode rotated = json.readTree(refresh(originalRefresh)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());

        String newAccess = rotated.get("accessToken").asText();
        String newRefresh = rotated.get("refreshToken").asText();
        assertThat(newRefresh).isNotEqualTo(originalRefresh);
        assertThat(newAccess).isNotBlank();
        mvc.perform(get("/api/v1/me").header("Authorization", bearer(newAccess))).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM refresh_sessions WHERE token_hash = ?",
                Integer.class, originalRefresh)).as("only the hash is persisted").isZero();
    }

    @Test
    void ca02ReuseOfARotatedRefreshTokenIsRejected() throws Exception {
        String originalRefresh = login(registerUser(), STRONG_PASSWORD).get("refreshToken").asText();
        refresh(originalRefresh).andExpect(status().isOk());

        refresh(originalRefresh)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    void ca02ExpiredRefreshSessionIsRejected() throws Exception {
        String email = registerUser();
        String refreshToken = login(email, STRONG_PASSWORD).get("refreshToken").asText();
        jdbc.update("UPDATE refresh_sessions rs JOIN users u ON u.id = rs.user_id "
                + "SET rs.expires_at = TIMESTAMPADD(MINUTE, -1, UTC_TIMESTAMP(6)) WHERE u.email = ?", email);

        refresh(refreshToken)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    void ca03LogoutRevokesTheRefreshTokenForFurtherRenewal() throws Exception {
        String refreshToken = login(registerUser(), STRONG_PASSWORD).get("refreshToken").asText();

        mvc.perform(post("/api/v1/auth/logout").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("refreshToken", refreshToken))))
                .andExpect(status().isNoContent());

        refresh(refreshToken)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
        mvc.perform(post("/api/v1/auth/logout").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("refreshToken", refreshToken))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    void accessTokenOrGarbageIsNotAcceptedAsRefreshToken() throws Exception {
        String access = login(registerUser(), STRONG_PASSWORD).get("accessToken").asText();

        refresh(access).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
        refresh("not-a-jwt").andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    void blankRefreshTokenIs400() throws Exception {
        refresh("").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    private ResultActions refresh(String refreshToken) throws Exception {
        return mvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("refreshToken", refreshToken))));
    }
}
