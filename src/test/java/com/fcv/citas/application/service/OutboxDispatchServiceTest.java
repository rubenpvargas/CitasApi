package com.fcv.citas.application.service;

import com.fcv.citas.application.model.OutboxMessage;
import com.fcv.citas.application.model.StatusEvent;
import com.fcv.citas.application.model.WebhookResult;
import com.fcv.citas.application.port.out.NotificationOutboxPort;
import com.fcv.citas.application.port.out.StatusWebhookPort;
import com.fcv.citas.domain.model.OutboxRetryPolicy;
import com.fcv.citas.domain.model.OutboxStatus;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/** WF-002 — despachador: entrega, reintentos con backoff, FAILED y desactivación sin URL. */
class OutboxDispatchServiceTest {
    static final Instant NOW = Instant.parse("2026-10-04T15:00:00Z");
    final FakeOutbox outbox = new FakeOutbox();
    final FakeWebhook webhook = new FakeWebhook();
    final OutboxDispatchService service = new OutboxDispatchService(outbox, webhook, new DirectTransactions(),
            Clock.fixed(NOW, ZoneOffset.UTC), new OutboxRetryPolicy(3, Duration.ofSeconds(30)), 20);

    @Test
    void deliveredMessageIsMarkedSentWithItsEventIdAndPayload() {
        outbox.add(1L, 0);
        webhook.responses.add(new WebhookResult(true, null));

        assertThat(service.dispatchDue()).isEqualTo(1);

        assertThat(outbox.rows.get(1L).status).isEqualTo(OutboxStatus.SENT);
        assertThat(outbox.rows.get(1L).attempts).isEqualTo(1);
        assertThat(webhook.sentEventIds).containsExactly("event-1");
    }

    @Test
    void failuresRetryWithBackoffAndTheThirdOneFails() {
        outbox.add(1L, 0);
        webhook.responses.addAll(List.of(new WebhookResult(false, "HTTP_500"), new WebhookResult(false, "HTTP_500"),
                new WebhookResult(false, "TIMEOUT")));

        service.dispatchDue();
        assertThat(outbox.rows.get(1L).status).isEqualTo(OutboxStatus.PENDING);
        assertThat(outbox.rows.get(1L).nextAttemptAt).isEqualTo(NOW.plusSeconds(30));
        assertThat(outbox.rows.get(1L).lastError).isEqualTo("HTTP_500");
        service.dispatchDue();
        assertThat(outbox.rows.get(1L).nextAttemptAt).isEqualTo(NOW.plusSeconds(60));
        service.dispatchDue();
        assertThat(outbox.rows.get(1L).status).isEqualTo(OutboxStatus.FAILED);
        assertThat(outbox.rows.get(1L).attempts).isEqualTo(3);
        assertThat(outbox.rows.get(1L).lastError).isEqualTo("TIMEOUT");
    }

    @Test
    void disabledWebhookLeavesMessagesPending() {
        outbox.add(1L, 0);
        webhook.enabled = false;

        assertThat(service.dispatchDue()).isZero();
        assertThat(outbox.rows.get(1L).status).isEqualTo(OutboxStatus.PENDING);
        assertThat(webhook.sentEventIds).isEmpty();
    }

    static final class Row {
        long id;
        int attempts;
        OutboxStatus status = OutboxStatus.PENDING;
        Instant nextAttemptAt = NOW;
        String lastError;
    }

    static final class FakeOutbox implements NotificationOutboxPort {
        final Map<Long, Row> rows = new LinkedHashMap<>();
        final List<StatusEvent> enqueued = new ArrayList<>();

        void add(long id, int attempts) {
            Row r = new Row();
            r.id = id;
            r.attempts = attempts;
            rows.put(id, r);
        }

        @Override public void enqueue(StatusEvent event, Instant at) { enqueued.add(event); }
        @Override public List<OutboxMessage> findDue(Instant now, int limit) {
            return rows.values().stream().filter(r -> r.status == OutboxStatus.PENDING)
                    .map(r -> new OutboxMessage(r.id, "event-" + r.id, "{\"type\":\"X\"}", r.attempts)).toList();
        }
        @Override public Optional<OutboxMessage> lockPending(long id) {
            Row r = rows.get(id);
            return r == null || r.status != OutboxStatus.PENDING ? Optional.empty()
                    : Optional.of(new OutboxMessage(r.id, "event-" + r.id, "{\"type\":\"X\"}", r.attempts));
        }
        @Override public void markSent(long id, int attempts, Instant at) {
            rows.get(id).status = OutboxStatus.SENT;
            rows.get(id).attempts = attempts;
        }
        @Override public void markRetry(long id, int attempts, Instant nextAttemptAt, String errorCode) {
            Row r = rows.get(id);
            r.attempts = attempts;
            r.nextAttemptAt = nextAttemptAt;
            r.lastError = errorCode;
        }
        @Override public void markFailed(long id, int attempts, String errorCode) {
            Row r = rows.get(id);
            r.status = OutboxStatus.FAILED;
            r.attempts = attempts;
            r.lastError = errorCode;
        }
    }

    static final class FakeWebhook implements StatusWebhookPort {
        boolean enabled = true;
        final List<WebhookResult> responses = new ArrayList<>();
        final List<String> sentEventIds = new ArrayList<>();
        @Override public boolean enabled() { return enabled; }
        @Override public WebhookResult post(String eventId, String payloadJson) {
            sentEventIds.add(eventId);
            return responses.remove(0);
        }
    }
}
