package com.fcv.citas.it;

import com.fasterxml.jackson.databind.JsonNode;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.web.servlet.MvcResult;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** HU-002 — login, rechazo seguro y aislamiento por rol con la cadena de seguridad real. */
class LoginAndRoleIsolationIT extends AbstractMySqlIT {

    @Test
    void ca01LoginIssuesSeparateAccessAndRefreshWithRolesClaim() throws Exception {
        String email = registerUser();

        JsonNode body = login(email.toUpperCase(), STRONG_PASSWORD);

        String access = body.get("accessToken").asText();
        String refresh = body.get("refreshToken").asText();
        assertThat(access).isNotBlank().isNotEqualTo(refresh);
        assertThat(body.get("tokenType").asText()).isEqualTo("Bearer");
        assertThat(body.get("user").get("email").asText()).isEqualTo(email);
        assertThat(body.get("user").has("passwordHash")).isFalse();
        JsonNode accessClaims = claims(access);
        assertThat(accessClaims.get("token_type").asText()).isEqualTo("access");
        assertThat(accessClaims.get("roles").get(0).asText()).isEqualTo("USER");
        assertThat(claims(refresh).get("token_type").asText()).isEqualTo("refresh");
        assertThat(claims(refresh).has("roles")).isFalse();

        mvc.perform(get("/api/v1/me").header("Authorization", bearer(access)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));
    }

    @Test
    void ca02WrongEmailAndWrongPasswordProduceTheSameSafeError() throws Exception {
        String email = registerUser();

        String wrongPassword = failedLogin(email, "Incorrecta2026");
        String unknownEmail = failedLogin("nobody." + unique() + "@example.test", STRONG_PASSWORD);

        JsonNode a = json.readTree(wrongPassword);
        JsonNode b = json.readTree(unknownEmail);
        assertThat(a.get("code").asText()).isEqualTo("INVALID_CREDENTIALS");
        for (String field : List.of("status", "code", "title", "detail")) {
            assertThat(a.get(field)).isEqualTo(b.get(field));
        }
        assertThat(wrongPassword).doesNotContain("Incorrecta2026").doesNotContain(email);
    }

    @Test
    void ca03UserTokenIsForbiddenOnAdminAndProfessionalCapabilities() throws Exception {
        String access = login(registerUser(), STRONG_PASSWORD).get("accessToken").asText();

        mvc.perform(get("/api/v1/admin/eps").header("Authorization", bearer(access)))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        mvc.perform(get("/api/v1/admin/inbox").header("Authorization", bearer(access)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/professional/calendar").param("from", "2026-10-01").param("to", "2026-10-02")
                        .header("Authorization", bearer(access)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void ca03AdminRoleIsAuthorizedOnAdminCapability() throws Exception {
        String email = registerUser();
        jdbc.update("INSERT INTO user_roles(user_id, role_id) SELECT u.id, r.id FROM users u, roles r "
                + "WHERE u.email = ? AND r.code = 'ADMIN'", email);
        String access = login(email, STRONG_PASSWORD).get("accessToken").asText();

        mvc.perform(get("/api/v1/admin/eps").header("Authorization", bearer(access)))
                .andExpect(status().isOk());
    }

    @Test
    void missingTokenIs401() throws Exception {
        mvc.perform(get("/api/v1/admin/eps"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        mvc.perform(get("/api/v1/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tamperedTokenIs401() throws Exception {
        String access = login(registerUser(), STRONG_PASSWORD).get("accessToken").asText();
        String[] parts = access.split("\\.");
        JsonNode payload = claims(access);
        String forgedPayload = base64Url(payload.toString().replace("\"USER\"", "\"ADMIN\""));
        String tampered = parts[0] + "." + forgedPayload + "." + parts[2];

        mvc.perform(get("/api/v1/admin/eps").header("Authorization", bearer(tampered)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void expiredTokenSignedWithTheRealKeyIs401() throws Exception {
        Instant past = Instant.now().minusSeconds(3600);
        String expired = sign(ACCESS_SECRET, JwtClaimsSet.builder().issuer("citas-api").subject("1")
                .issuedAt(past.minusSeconds(900)).expiresAt(past)
                .claim("token_type", "access").claim("roles", List.of("ADMIN")).build());

        mvc.perform(get("/api/v1/admin/eps").header("Authorization", bearer(expired)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshTokenIsNotAcceptedAsAccessToken() throws Exception {
        String refresh = login(registerUser(), STRONG_PASSWORD).get("refreshToken").asText();

        mvc.perform(get("/api/v1/me").header("Authorization", bearer(refresh)))
                .andExpect(status().isUnauthorized());
    }

    private String failedLogin(String email, String password) throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", email, "password", password))))
                .andExpect(status().isUnauthorized())
                .andReturn();
        return result.getResponse().getContentAsString();
    }

    private JsonNode claims(String jwt) throws Exception {
        return json.readTree(Base64.getUrlDecoder().decode(jwt.split("\\.")[1]));
    }

    private static String base64Url(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String sign(String secret, JwtClaimsSet claims) {
        var key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        return new NimbusJwtEncoder(new ImmutableSecret<>(key))
                .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }
}
