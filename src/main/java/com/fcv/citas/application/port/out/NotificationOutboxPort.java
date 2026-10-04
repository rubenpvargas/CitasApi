package com.fcv.citas.application.port.out;

import com.fcv.citas.application.model.OutboxMessage;
import com.fcv.citas.application.model.StatusEvent;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Outbox transaccional de notificaciones de estado (WF-002). */
public interface NotificationOutboxPort {
    /** Inserta el evento en la transacción vigente: si ésta hace rollback, el evento no existe. */
    void enqueue(StatusEvent event, Instant at);

    List<OutboxMessage> findDue(Instant now, int limit);

    /** Bloquea la fila si sigue PENDING (SKIP LOCKED frente a despachadores concurrentes). */
    Optional<OutboxMessage> lockPending(long id);

    void markSent(long id, int attempts, Instant at);

    void markRetry(long id, int attempts, Instant nextAttemptAt, String errorCode);

    void markFailed(long id, int attempts, String errorCode);
}
