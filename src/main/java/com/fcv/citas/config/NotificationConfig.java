package com.fcv.citas.config;

import com.fcv.citas.adapter.out.notification.HttpStatusWebhookAdapter;
import com.fcv.citas.application.port.in.OutboxDispatchUseCase;
import com.fcv.citas.application.port.out.NotificationOutboxPort;
import com.fcv.citas.application.port.out.ProfileRepositoryPort;
import com.fcv.citas.application.port.out.StatusWebhookPort;
import com.fcv.citas.application.port.out.TransactionPort;
import com.fcv.citas.application.service.OutboxDispatchService;
import com.fcv.citas.application.service.OutboxStatusEventRecorder;
import com.fcv.citas.application.service.StatusEventRecorder;
import com.fcv.citas.domain.model.OutboxRetryPolicy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;
import java.time.Duration;

/** Ola G (WF-002) — outbox transaccional, webhook de n8n y despachador programado. */
@Configuration
@EnableScheduling
public class NotificationConfig {
    @Bean
    StatusEventRecorder statusEventRecorder(NotificationOutboxPort outbox, ProfileRepositoryPort profiles, Clock clock) {
        return new OutboxStatusEventRecorder(outbox, profiles, clock);
    }

    @Bean
    StatusWebhookPort statusWebhookPort(@Value("${app.notifications.webhook-url:}") String url,
                                        @Value("${app.notifications.webhook-secret:}") String secret) {
        return new HttpStatusWebhookAdapter(url, secret);
    }

    @Bean
    OutboxDispatchUseCase outboxDispatchUseCase(NotificationOutboxPort outbox, StatusWebhookPort webhook,
                                                TransactionPort transactions, Clock clock,
                                                @Value("${app.notifications.max-attempts:3}") int maxAttempts,
                                                @Value("${app.notifications.retry-base-seconds:30}") long baseSeconds,
                                                @Value("${app.notifications.batch-size:20}") int batchSize) {
        return new OutboxDispatchService(outbox, webhook, transactions, clock,
                new OutboxRetryPolicy(maxAttempts, Duration.ofSeconds(baseSeconds)), batchSize);
    }
}
