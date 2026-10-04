package com.fcv.citas.application.service;

import com.fcv.citas.application.model.OutboxMessage;
import com.fcv.citas.application.model.WebhookResult;
import com.fcv.citas.application.port.in.OutboxDispatchUseCase;
import com.fcv.citas.application.port.out.NotificationOutboxPort;
import com.fcv.citas.application.port.out.StatusWebhookPort;
import com.fcv.citas.application.port.out.TransactionPort;
import com.fcv.citas.domain.model.OutboxRetryPolicy;

import java.time.Clock;
import java.time.Instant;

/**
 * WF-002 — cada mensaje vencido se procesa en su propia transacción corta: se bloquea si sigue PENDING,
 * se envía y se registra el resultado según {@link OutboxRetryPolicy}. Un fallo del webhook solo afecta al
 * mensaje; la transición de la cita ya está confirmada.
 */
public final class OutboxDispatchService implements OutboxDispatchUseCase {
    private final NotificationOutboxPort outbox;
    private final StatusWebhookPort webhook;
    private final TransactionPort transactions;
    private final Clock clock;
    private final OutboxRetryPolicy policy;
    private final int batchSize;

    public OutboxDispatchService(NotificationOutboxPort outbox, StatusWebhookPort webhook, TransactionPort transactions,
                                 Clock clock, OutboxRetryPolicy policy, int batchSize) {
        this.outbox = outbox;
        this.webhook = webhook;
        this.transactions = transactions;
        this.clock = clock;
        this.policy = policy;
        this.batchSize = batchSize;
    }

    @Override
    public int dispatchDue() {
        if (!webhook.enabled()) {
            return 0;
        }
        int attempted = 0;
        for (OutboxMessage due : outbox.findDue(clock.instant(), batchSize)) {
            Boolean processed = transactions.required(() -> outbox.lockPending(due.id()).map(this::deliver).orElse(false));
            if (Boolean.TRUE.equals(processed)) {
                attempted++;
            }
        }
        return attempted;
    }

    private boolean deliver(OutboxMessage message) {
        WebhookResult result = webhook.post(message.eventId(), message.payloadJson());
        int attempt = message.attempts() + 1;
        Instant now = clock.instant();
        OutboxRetryPolicy.Outcome outcome = policy.afterAttempt(attempt, result.delivered(), now);
        switch (outcome.status()) {
            case SENT -> outbox.markSent(message.id(), attempt, now);
            case PENDING -> outbox.markRetry(message.id(), attempt, outcome.nextAttemptAt(), result.errorCode());
            case FAILED -> outbox.markFailed(message.id(), attempt, result.errorCode());
        }
        return true;
    }
}
