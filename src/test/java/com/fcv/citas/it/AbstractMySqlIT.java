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

import java.util.List;
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
    protected static final String AUTOMATION_KEY = "it-automation-" + UUID.randomUUID();
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
        registry.add("app.automation.api-key", () -> AUTOMATION_KEY);
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

    /** Registra un USER sintético, le añade los roles indicados en la base de pruebas y devuelve su access token. */
    protected String tokenWithRoles(String... extraRoles) throws Exception {
        String email = registerUser();
        for (String role : extraRoles) {
            jdbc.update("INSERT INTO user_roles(user_id, role_id) SELECT u.id, r.id FROM users u, roles r "
                    + "WHERE u.email = ? AND r.code = ?", email, role);
        }
        return bearer(login(email, STRONG_PASSWORD).get("accessToken").asText());
    }

    /** Usuario con exactamente los roles indicados (se retira el rol USER del registro). */
    protected String tokenOnlyRoles(String... roles) throws Exception {
        String email = registerUser();
        jdbc.update("DELETE ur FROM user_roles ur JOIN users u ON u.id = ur.user_id WHERE u.email = ?", email);
        for (String role : roles) {
            jdbc.update("INSERT INTO user_roles(user_id, role_id) SELECT u.id, r.id FROM users u, roles r "
                    + "WHERE u.email = ? AND r.code = ?", email, role);
        }
        return bearer(login(email, STRONG_PASSWORD).get("accessToken").asText());
    }

    /** Reserva general vía API como un USER nuevo; devuelve el cuerpo AppointmentDto. */
    protected JsonNode bookGeneral(String userToken, long professionalId, String locationCode, java.time.LocalDateTime startAt)
            throws Exception {
        return body(mvc.perform(post("/api/v1/appointments/general").header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("professionalId", professionalId, "locationCode", locationCode,
                                "startAt", startAt.toString().length() == 16 ? startAt + ":00" : startAt.toString(),
                                "reason", "Control sintetico"))))
                .andExpect(status().isCreated()).andReturn());
    }

    /** Publica un bloque como el profesional indicado y devuelve su id. */
    protected long publishBlock(String professionalToken, java.time.LocalDate date, String start, String end,
                                String locationCode) throws Exception {
        return body(mvc.perform(post("/api/v1/professional/blocks").header("Authorization", professionalToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("date", date.toString(), "startTime", start, "endTime", end,
                                "locationCode", locationCode))))
                .andExpect(status().isCreated()).andReturn()).get("id").asLong();
    }

    /**
     * Fixture SQL sintético: una cita APPROVED del profesional demo 9001 y una reprogramación PENDING
     * real (respeta fk_slot_reschedule_request) para simular retenciones en pruebas de agenda.
     */
    protected long syntheticPendingRescheduleId() throws Exception {
        String email = registerUser();
        long userId = jdbc.queryForObject("SELECT id FROM users WHERE email = ?", Long.class, email);
        java.time.LocalDateTime start = agendaToday().plusDays(60).atTime(7, 0);
        jdbc.update("INSERT INTO appointments(patient_user_id, professional_id, location_id, specialty_id, status_id, "
                + "scheduled_start_at, scheduled_end_at, created_by_user_id, created_at, updated_at) "
                + "SELECT ?, 9001, l.id, s.id, st.id, ?, ?, ?, NOW(6), NOW(6) FROM locations l, specialties s, "
                + "appointment_statuses st WHERE l.code = 'HIC' AND s.code = 'MEDICINA_GENERAL' AND st.code = 'APPROVED'",
                userId, start, start.plusMinutes(30), userId);
        long appointmentId = jdbc.queryForObject("SELECT MAX(id) FROM appointments WHERE patient_user_id = ?", Long.class, userId);
        jdbc.update("INSERT INTO reschedule_requests(appointment_id, requested_by_user_id, requested_location_id, status_id, "
                + "previous_start_at, previous_end_at, requested_start_at, requested_end_at, created_at) "
                + "SELECT ?, ?, l.id, rs.id, ?, ?, ?, ?, NOW(6) FROM locations l, reschedule_request_statuses rs "
                + "WHERE l.code = 'HIC' AND rs.code = 'PENDING'",
                appointmentId, userId, start, start.plusMinutes(30), start.plusDays(1), start.plusDays(1).plusMinutes(30));
        return jdbc.queryForObject("SELECT MAX(id) FROM reschedule_requests WHERE appointment_id = ?", Long.class, appointmentId);
    }

    protected String userToken() throws Exception {
        return tokenWithRoles();
    }

    protected String adminToken() throws Exception {
        return tokenWithRoles("ADMIN");
    }

    /** Profesional sintético creado por la API de ADMIN, con capacidades asignadas y su access token. */
    protected record ProfessionalFixture(long id, String token, String email) {
    }

    protected ProfessionalFixture activeProfessional(List<String> specialtyCodes, List<String> locationCodes) throws Exception {
        String admin = adminToken();
        String suffix = unique();
        String email = "agenda." + suffix + "@example.test";
        Map<String, Object> create = new java.util.HashMap<>();
        create.put("firstName", "Valeria");
        create.put("lastName", "Agenda");
        create.put("documentType", "CC");
        create.put("documentNumber", "AG" + suffix);
        create.put("email", email);
        create.put("phone", "3000000000");
        create.put("password", STRONG_PASSWORD);
        create.put("professionalCode", "AG-" + suffix);
        create.put("licenseNumber", "RM-AG-" + suffix);
        long id = body(mvc.perform(post("/api/v1/admin/professionals").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content(toJson(create)))
                .andExpect(status().isCreated()).andReturn()).get("id").asLong();
        List<Long> specialtyIds = specialtyCodes.stream()
                .map(code -> jdbc.queryForObject("SELECT id FROM specialties WHERE code = ?", Long.class, code)).toList();
        List<Long> locationIds = locationCodes.stream()
                .map(code -> jdbc.queryForObject("SELECT id FROM locations WHERE code = ?", Long.class, code)).toList();
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/api/v1/admin/professionals/" + id + "/capabilities").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("specialtyIds", specialtyIds, "primarySpecialtyId", specialtyIds.getFirst(),
                                "locationIds", locationIds, "active", true))))
                .andExpect(status().isOk());
        return new ProfessionalFixture(id, bearer(login(email, STRONG_PASSWORD).get("accessToken").asText()), email);
    }

    /** Hoy en la zona de la agenda (America/Bogota), la misma que usa el Clock de la aplicación. */
    protected static java.time.LocalDate agendaToday() {
        return java.time.LocalDate.now(java.time.ZoneId.of("America/Bogota"));
    }

    protected JsonNode body(MvcResult result) throws Exception {
        return json.readTree(result.getResponse().getContentAsString());
    }

    protected String toJson(Object value) throws Exception {
        return json.writeValueAsString(value);
    }
}
