package com.fcv.citas.it;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fcv.citas.CitasApiApplication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base de pruebas de integración REST + persistencia contra MySQL 8.4 real.
 *
 * <p>El datasource proviene exclusivamente de variables de entorno ({@code IT_DB_URL}, {@code IT_DB_USER},
 * {@code IT_DB_PASSWORD}). Los secretos JWT son fixtures aleatorios generados por JVM, no secretos reales.
 * Al arrancar, Flyway limpia la base y la migra desde cero (ver {@link MySqlItConfiguration}).
 */
@SpringBootTest(classes = {CitasApiApplication.class, MySqlItConfiguration.class})
@AutoConfigureMockMvc
public abstract class AbstractMySqlIT {
    protected static final String ACCESS_SECRET = "it-access-" + UUID.randomUUID() + UUID.randomUUID();
    protected static final String REFRESH_SECRET = "it-refresh-" + UUID.randomUUID() + UUID.randomUUID();
    protected static final String STRONG_PASSWORD = "Sintetica2026";
    private static final AtomicInteger SEQUENCE = new AtomicInteger();
    private static final String RUN = Long.toString(System.nanoTime(), 36);

    @Autowired
    protected MockMvc mvc;
    @Autowired
    protected ObjectMapper json;
    @Autowired
    protected JdbcTemplate jdbc;

    @DynamicPropertySource
    static void mysqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> requiredEnv("IT_DB_URL"));
        registry.add("spring.datasource.username", () -> requiredEnv("IT_DB_USER"));
        registry.add("spring.datasource.password", () -> requiredEnv("IT_DB_PASSWORD"));
        registry.add("spring.flyway.clean-disabled", () -> "false");
        registry.add("app.security.access-secret", () -> ACCESS_SECRET);
        registry.add("app.security.refresh-secret", () -> REFRESH_SECRET);
    }

    private static String requiredEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Integration tests require environment variable " + name);
        }
        return value;
    }

    /** Sufijo único por prueba para no depender del orden de ejecución ni de datos previos. */
    protected static String unique() {
        return RUN + SEQUENCE.incrementAndGet();
    }

    protected Map<String, Object> registrationBody(String email, String documentNumber) {
        return Map.of("firstName", "Ana", "lastName", "Sintetica", "documentType", "CC",
                "documentNumber", documentNumber, "email", email, "phone", "3000000000",
                "password", STRONG_PASSWORD);
    }

    /** Registra un USER sintético y devuelve su email normalizado. */
    protected String registerUser() throws Exception {
        String suffix = unique();
        String email = "it." + suffix + "@example.test";
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(registrationBody(email, "IT" + suffix))))
                .andExpect(status().isCreated());
        return email;
    }

    protected JsonNode login(String email, String password) throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", email, "password", password))))
                .andExpect(status().isOk())
                .andReturn();
        return json.readTree(result.getResponse().getContentAsString());
    }

    protected String bearer(String accessToken) {
        return "Bearer " + accessToken;
    }
}
