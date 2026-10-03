package com.fcv.citas.config;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** HU-002 DoD — los secretos JWT solo llegan por entorno y la aplicación no arranca sin ellos. */
class SecurityPropertiesTest {
    private static final String A = "a".repeat(32);
    private static final String B = "b".repeat(32);

    @Test
    void rejectsMissingShortOrSharedSecrets() {
        assertThatThrownBy(() -> new SecurityProperties("citas-api", null, B, 15, 7))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SecurityProperties("citas-api", "short", B, 15, 7))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SecurityProperties("citas-api", A, A, 15, 7))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void applicationYamlResolvesJwtSecretsOnlyFromEnvironmentWithoutDefaults() throws Exception {
        String yaml = new ClassPathResource("application.yml").getContentAsString(StandardCharsets.UTF_8);

        assertThat(yaml).contains("access-secret: ${JWT_ACCESS_SECRET}");
        assertThat(yaml).contains("refresh-secret: ${JWT_REFRESH_SECRET}");
        assertThat(yaml).contains("password: ${DB_PASSWORD}");
    }
}
