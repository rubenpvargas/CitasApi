package com.fcv.citas.adapter.out.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fcv.citas.application.model.OutboxMessage;
import com.fcv.citas.application.model.StatusEvent;
import com.fcv.citas.application.port.out.NotificationOutboxPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/** Outbox JDBC: el payload se serializa aquí con la forma exacta del contrato de WF-002. */
@Component
public class JdbcNotificationOutboxAdapter implements NotificationOutboxPort {
    private static final DateTimeFormatter WALL = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final RowMapper<OutboxMessage> ROW = (rs, n) -> new OutboxMessage(rs.getLong("id"),
            rs.getString("event_id"), rs.getString("payload"), rs.getInt("attempts"));

    private final JdbcTemplate jdbc;
    private final ObjectMapper json;

    public JdbcNotificationOutboxAdapter(JdbcTemplate jdbc, ObjectMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    @Override
    public void enqueue(StatusEvent event, Instant at) {
        jdbc.update("INSERT INTO notification_outbox(event_id, correlation_id, event_type, appointment_id, payload, "
                + "status, attempts, next_attempt_at, created_at) VALUES (?, ?, ?, ?, ?, 'PENDING', 0, ?, ?)",
                event.eventId(), event.correlationId(), event.type().name(), event.appointmentId(), payload(event),
                Timestamp.from(at), Timestamp.from(at));
    }

    @Override
    public List<OutboxMessage> findDue(Instant now, int limit) {
        return jdbc.query("SELECT id, event_id, payload, attempts FROM notification_outbox "
                + "WHERE status = 'PENDING' AND next_attempt_at <= ? ORDER BY next_attempt_at, id LIMIT ?",
                ROW, Timestamp.from(now), limit);
    }

    @Override
    public Optional<OutboxMessage> lockPending(long id) {
        return jdbc.query("SELECT id, event_id, payload, attempts FROM notification_outbox "
                + "WHERE id = ? AND status = 'PENDING' FOR UPDATE SKIP LOCKED", ROW, id).stream().findFirst();
    }

    @Override
    public void markSent(long id, int attempts, Instant at) {
        jdbc.update("UPDATE notification_outbox SET status = 'SENT', attempts = ?, sent_at = ?, last_error_code = NULL "
                + "WHERE id = ?", attempts, Timestamp.from(at), id);
    }

    @Override
    public void markRetry(long id, int attempts, Instant nextAttemptAt, String errorCode) {
        jdbc.update("UPDATE notification_outbox SET attempts = ?, next_attempt_at = ?, last_error_code = ? WHERE id = ?",
                attempts, Timestamp.from(nextAttemptAt), errorCode, id);
    }

    @Override
    public void markFailed(long id, int attempts, String errorCode) {
        jdbc.update("UPDATE notification_outbox SET status = 'FAILED', attempts = ?, last_error_code = ? WHERE id = ?",
                attempts, errorCode, id);
    }

    /** {eventId, correlationId, type, occurredAt, appointmentId, status, startAt, recipient:{firstName,email}, reason?} */
    private String payload(StatusEvent e) {
        ObjectNode root = json.createObjectNode();
        root.put("eventId", e.eventId());
        root.put("correlationId", e.correlationId());
        root.put("type", e.type().name());
        root.put("occurredAt", WALL.format(e.occurredAt()));
        root.put("appointmentId", e.appointmentId());
        root.put("status", e.status());
        root.put("startAt", WALL.format(e.startAt()));
        ObjectNode recipient = root.putObject("recipient");
        recipient.put("firstName", e.recipientFirstName());
        recipient.put("email", e.recipientEmail());
        if (e.reason() != null) {
            root.put("reason", e.reason());
        }
        try {
            return json.writeValueAsString(root);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot serialize status event", exception);
        }
    }
}
