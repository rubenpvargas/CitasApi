package com.fcv.citas.it;

import com.fcv.citas.application.exception.BusinessRuleException;
import com.fcv.citas.application.port.in.PasswordRecoveryUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.ResultActions;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HU-004 flujo completo con {@code expose-development-token=true} (solo laboratorio): consumo, un solo uso,
 * invalidación por solicitud nueva, expiración y consumo atómico concurrente.
 */
@TestPropertySource(properties = "app.password-reset.expose-development-token=true")
class PasswordResetDevelopmentFlowIT extends AbstractMySqlIT {
    private static final String NEW_PASSWORD = "NuevaClave2026";

    @Autowired
    private PasswordRecoveryUseCase recovery;

    @Test
    void ca01Ca02FullFlowChangesPasswordConsumesTokenAndRevokesSessions() throws Exception {
        String email = registerUser();
        String oldRefresh = login(email, STRONG_PASSWORD).get("refreshToken").asText();

        String token = requestToken(email);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM password_reset_tokens WHERE token_hash = ?",
                Integer.class, token)).as("raw token is never stored").isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM password_reset_tokens WHERE token_hash = ?",
                Integer.class, sha256(token))).isOne();

        confirm(token, NEW_PASSWORD).andExpect(status().isNoContent());

        String stored = jdbc.queryForObject("SELECT password_hash FROM users WHERE email = ?", String.class, email);
        assertThat(stored).startsWith("$2").doesNotContain(NEW_PASSWORD);
        login(email, NEW_PASSWORD);
        loginFails(email, STRONG_PASSWORD);
        mvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("refreshToken", oldRefresh))))
                .andExpect(status().isUnauthorized());
        assertThat(jdbc.queryForObject("SELECT used_at IS NOT NULL FROM password_reset_tokens WHERE token_hash = ?",
                Boolean.class, sha256(token))).isTrue();
    }

    @Test
    void ca03UsedTokenIsRejectedWithoutChangingPassword() throws Exception {
        String email = registerUser();
        String token = requestToken(email);
        confirm(token, NEW_PASSWORD).andExpect(status().isNoContent());
        String hashAfterFirstUse = passwordHash(email);

        confirm(token, "OtraClave2026")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_RESET_TOKEN"));

        assertThat(passwordHash(email)).isEqualTo(hashAfterFirstUse);
    }

    @Test
    void ca03ExpiredTokenIsRejectedWithoutChangingPassword() throws Exception {
        String email = registerUser();
        String token = requestToken(email);
        String original = passwordHash(email);
        jdbc.update("UPDATE password_reset_tokens SET expires_at = TIMESTAMPADD(SECOND, -1, UTC_TIMESTAMP(6)) "
                + "WHERE token_hash = ?", sha256(token));

        confirm(token, NEW_PASSWORD)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_RESET_TOKEN"));

        assertThat(passwordHash(email)).isEqualTo(original);
        login(email, STRONG_PASSWORD);
    }

    @Test
    void newRequestInvalidatesPreviousUnusedToken() throws Exception {
        String email = registerUser();
        String first = requestToken(email);
        String second = requestToken(email);

        confirm(first, NEW_PASSWORD)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_RESET_TOKEN"));
        confirm(second, NEW_PASSWORD).andExpect(status().isNoContent());
        assertThat(jdbc.queryForObject("SELECT invalidated_at IS NOT NULL FROM password_reset_tokens WHERE token_hash = ?",
                Boolean.class, sha256(first))).isTrue();
    }

    @Test
    void unknownEmailNeverReceivesADevelopmentToken() throws Exception {
        request("ghost." + unique() + "@example.test")
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.developmentToken").doesNotExist());
    }

    @Test
    void concurrentConfirmationsConsumeTheTokenExactlyOnce() throws Exception {
        String email = registerUser();
        String token = requestToken(email);
        CountDownLatch start = new CountDownLatch(1);
        Callable<Boolean> attempt = () -> {
            start.await();
            try {
                recovery.confirmReset(token, NEW_PASSWORD);
                return true;
            } catch (BusinessRuleException rejected) {
                return false;
            }
        };
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<Future<Boolean>> results = List.of(pool.submit(attempt), pool.submit(attempt));
            start.countDown();
            long successes = 0;
            for (Future<Boolean> result : results) {
                if (result.get()) successes++;
            }
            assertThat(successes).isOne();
        } finally {
            pool.shutdownNow();
        }
    }

    private String requestToken(String email) throws Exception {
        String body = request(email).andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString();
        String token = json.readTree(body).get("developmentToken").asText();
        assertThat(token).isNotBlank();
        return token;
    }

    private ResultActions request(String email) throws Exception {
        return mvc.perform(post("/api/v1/auth/password-reset/request").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email", email))));
    }

    private ResultActions confirm(String token, String newPassword) throws Exception {
        return mvc.perform(post("/api/v1/auth/password-reset/confirm").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("token", token, "newPassword", newPassword))));
    }

    private void loginFails(String email, String password) throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", email, "password", password))))
                .andExpect(status().isUnauthorized());
    }

    private String passwordHash(String email) {
        return jdbc.queryForObject("SELECT password_hash FROM users WHERE email = ?", String.class, email);
    }

    private static String sha256(String value) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8)));
    }
}
