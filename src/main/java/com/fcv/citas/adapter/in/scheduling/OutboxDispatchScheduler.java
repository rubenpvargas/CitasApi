package com.fcv.citas.adapter.in.scheduling;

import com.fcv.citas.application.port.in.OutboxDispatchUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Disparador programado del despachador del outbox (WF-002); sin URL de webhook no envía nada. */
@Component
@ConditionalOnProperty(prefix = "app.notifications", name = "scheduler-enabled", havingValue = "true", matchIfMissing = true)
public class OutboxDispatchScheduler {
    private static final Logger LOG = LoggerFactory.getLogger(OutboxDispatchScheduler.class);
    private final OutboxDispatchUseCase dispatcher;

    public OutboxDispatchScheduler(OutboxDispatchUseCase dispatcher) {
        this.dispatcher = dispatcher;
    }

    @Scheduled(fixedDelayString = "${app.notifications.dispatch-delay-ms:10000}",
            initialDelayString = "${app.notifications.dispatch-delay-ms:10000}")
    void dispatch() {
        try {
            dispatcher.dispatchDue();
        } catch (RuntimeException exception) {
            LOG.error("Outbox dispatch pass failed: {}", exception.getClass().getName());
        }
    }
}
