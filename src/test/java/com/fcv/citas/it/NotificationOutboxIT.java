package com.fcv.citas.it;

import com.fasterxml.jackson.databind.JsonNode;
import com.fcv.citas.application.port.in.AppointmentDecisionUseCase;
import com.fcv.citas.application.port.in.OutboxDispatchUseCase;
import com.fcv.citas.application.port.out.TransactionPort;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Ola G (WF-002) — outbox transaccional contra MySQL real y un webhook HTTP local en la JVM: cabecera,
 * payload, SENT; 500 → reintentos → FAILED; rollback → sin evento; webhook caído no afecta la transición.
 */
class NotificationOutboxIT extends AbstractMySqlIT {
    private static final String SECRET = "it-webhook-" + UUID.randomUUID();
    private static final AtomicInteger RESPONSE_STATUS = new AtomicInteger(200);
    private static final List<Received> RECEIVED = new CopyOnWriteArrayList<>();
    private static final HttpServer SERVER = start();

    record Received(String secret, String body) {
    }

    @Autowired
    private OutboxDispatchUseCase dispatcher;
    @Autowired
    private AppointmentDecisionUseCase decisions;
    @Autowired
    private TransactionPort transactions;

    private static HttpServer start() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/webhook/status", exchange -> {
                byte[] body = exchange.getRequestBody().readAllBytes();
                RECEIVED.add(new Received(exchange.getRequestHeaders().getFirst("X-Webhook-Secret"),
                        new String(body, StandardCharsets.UTF_8)));
                exchange.sendResponseHeaders(RESPONSE_STATUS.get(), -1);
                exchange.close();
            });
            server.start();
            return server;
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    @DynamicPropertySource
    static void webhookProperties(DynamicPropertyRegistry registry) {
        registry.add("app.notifications.webhook-url",
                () -> "http://127.0.0.1:" + SERVER.getAddress().getPort() + "/webhook/status");
        registry.add("app.notifications.webhook-secret", () -> SECRET);
        registry.add("app.notifications.scheduler-enabled", () -> "false");
        registry.add("app.notifications.retry-base-seconds", () -> "2");
    }

    @AfterAll
    static void stop() {
        SERVER.stop(0);
    }

    @BeforeEach
    void reset() {
        RESPONSE_STATUS.set(200);
        RECEIVED.clear();
        // Aísla cada prueba: lo pendiente de pruebas anteriores no se despacha aquí.
        jdbc.update("UPDATE notification_outbox SET status = 'FAILED' WHERE status = 'PENDING'");
    }

    @Test
    void rejectionEnqueuesInTheSameTransactionAndIsDeliveredWithSecretAndContractPayload() throws Exception {
        long id = requestSpecialized(agendaToday().plusDays(6));

        decide(id, false, "Agenda sintetica completa");

        Map<String, Object> row = jdbc.queryForMap("SELECT event_type, status, attempts FROM notification_outbox "
                + "WHERE appointment_id = ?", id);
        assertThat(row.get("event_type")).isEqualTo("APPOINTMENT_REJECTED");
        assertThat(row.get("status")).isEqualTo("PENDING");

        assertThat(dispatcher.dispatchDue()).isEqualTo(1);

        assertThat(RECEIVED).hasSize(1);
        assertThat(RECEIVED.getFirst().secret()).isEqualTo(SECRET);
        JsonNode payload = json.readTree(RECEIVED.getFirst().body());
        assertThat(payload.fieldNames()).toIterable().containsExactlyInAnyOrder("eventId", "correlationId", "type",
                "occurredAt", "appointmentId", "status", "startAt", "recipient", "reason");
        assertThat(payload.get("type").asText()).isEqualTo("APPOINTMENT_REJECTED");
        assertThat(payload.get("status").asText()).isEqualTo("REJECTED");
        assertThat(payload.get("appointmentId").asLong()).isEqualTo(id);
        assertThat(payload.get("reason").asText()).isEqualTo("Agenda sintetica completa");
        assertThat(payload.get("recipient").get("firstName").asText()).isEqualTo("Ana");
        assertThat(payload.get("recipient").get("email").asText()).endsWith("@example.test");
        assertThat(payload.get("startAt").asText()).matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}");
        assertThat(RECEIVED.getFirst().body()).doesNotContain("password").doesNotContain("document").doesNotContain("eyJ");
        assertThat(jdbc.queryForObject("SELECT status FROM notification_outbox WHERE appointment_id = ?", String.class, id))
                .isEqualTo("SENT");
        assertThat(jdbc.queryForObject("SELECT sent_at IS NOT NULL FROM notification_outbox WHERE appointment_id = ?",
                Boolean.class, id)).isTrue();
    }

    @Test
    void serverErrorIsRetriedWithBackoffAndEndsFailedAfterThreeAttempts() throws Exception {
        long id = requestSpecialized(agendaToday().plusDays(7));
        decide(id, true, null);
        RESPONSE_STATUS.set(500);

        Instant before = Instant.now();
        dispatcher.dispatchDue();
        Map<String, Object> first = jdbc.queryForMap("SELECT status, attempts, last_error_code, next_attempt_at "
                + "FROM notification_outbox WHERE appointment_id = ?", id);
        assertThat(first.get("status")).isEqualTo("PENDING");
        assertThat(((Number) first.get("attempts")).intValue()).isEqualTo(1);
        assertThat(first.get("last_error_code")).isEqualTo("HTTP_500");
        assertThat(((Timestamp) first.get("next_attempt_at")).toInstant()).isAfter(before.plusMillis(1500));

        makeDue(id);
        dispatcher.dispatchDue();
        makeDue(id);
        dispatcher.dispatchDue();

        Map<String, Object> last = jdbc.queryForMap("SELECT status, attempts FROM notification_outbox WHERE appointment_id = ?", id);
        assertThat(last.get("status")).isEqualTo("FAILED");
        assertThat(((Number) last.get("attempts")).intValue()).isEqualTo(3);
        assertThat(RECEIVED).hasSize(3);
        makeDue(id);
        dispatcher.dispatchDue();
        assertThat(RECEIVED).hasSize(3);
        assertThat(appointmentStatus(id)).isEqualTo("APPROVED");
    }

    @Test
    void rolledBackTransitionLeavesNoEventAndNoStateChange() throws Exception {
        long id = requestSpecialized(agendaToday().plusDays(8));
        long admin = jdbc.queryForObject("SELECT MIN(id) FROM users", Long.class);

        assertThatThrownBy(() -> transactions.required(() -> {
            decisions.decide(admin, id, true, null);
            throw new IllegalStateException("rollback sintetico");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM notification_outbox WHERE appointment_id = ?", Integer.class, id))
                .isZero();
        assertThat(appointmentStatus(id)).isEqualTo("REQUESTED");
    }

    @Test
    void transitionSucceedsWhenTheWebhookIsDown() throws Exception {
        ProfessionalFixture pro = activeProfessional(List.of("MEDICINA_GENERAL"), List.of("HIC"));
        LocalDate day = agendaToday().plusDays(9);
        publishBlock(pro.token(), day, "08:00", "08:30", "HIC");
        String user = userToken();
        long id = bookGeneral(user, pro.id(), "HIC", day.atTime(8, 0)).get("id").asLong();
        RESPONSE_STATUS.set(503);

        mvc.perform(post("/api/v1/appointments/" + id + "/cancel").header("Authorization", user)).andExpect(status().isOk());
        dispatcher.dispatchDue();

        assertThat(appointmentStatus(id)).isEqualTo("CANCELLED");
        Map<String, Object> row = jdbc.queryForMap("SELECT event_type, status, last_error_code FROM notification_outbox "
                + "WHERE appointment_id = ?", id);
        assertThat(row.get("event_type")).isEqualTo("APPOINTMENT_CANCELLED");
        assertThat(row.get("status")).isEqualTo("PENDING");
        assertThat(row.get("last_error_code")).isEqualTo("HTTP_503");
    }

    private void makeDue(long appointmentId) {
        jdbc.update("UPDATE notification_outbox SET next_attempt_at = TIMESTAMPADD(SECOND, -1, NOW(6)) WHERE appointment_id = ?",
                appointmentId);
    }

    private String appointmentStatus(long id) {
        return jdbc.queryForObject("SELECT s.code FROM appointments a JOIN appointment_statuses s ON s.id = a.status_id "
                + "WHERE a.id = ?", String.class, id);
    }

    private long requestSpecialized(LocalDate day) throws Exception {
        ProfessionalFixture pro = activeProfessional(List.of("CARDIOLOGIA_ADULTO"), List.of("HIC"));
        publishBlock(pro.token(), day, "08:00", "08:30", "HIC");
        long cardio = jdbc.queryForObject("SELECT id FROM specialties WHERE code = 'CARDIOLOGIA_ADULTO'", Long.class);
        return body(mvc.perform(post("/api/v1/appointments/specialized").header("Authorization", userToken())
                        .contentType(MediaType.APPLICATION_JSON).content(toJson(Map.of("specialtyId", cardio,
                                "professionalId", pro.id(), "locationCode", "HIC", "startAt", day + "T08:00:00"))))
                .andExpect(status().isCreated()).andReturn()).get("id").asLong();
    }

    private void decide(long id, boolean approve, String reason) throws Exception {
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("approve", approve);
        body.put("reason", reason);
        mvc.perform(post("/api/v1/admin/appointments/" + id + "/decision").header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON).content(toJson(body)))
                .andExpect(status().isOk());
    }
}
