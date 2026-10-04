package com.fcv.citas.adapter.out.notification;

import com.fcv.citas.application.model.WebhookResult;
import com.fcv.citas.application.port.out.StatusWebhookPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;

/**
 * POST JSON al webhook de n8n (WF-002) con cabecera {@code X-Webhook-Secret} y timeout de 3 s. Nunca
 * registra el secreto ni el payload; solo el código de error estable.
 */
public class HttpStatusWebhookAdapter implements StatusWebhookPort {
    private static final Logger LOG = LoggerFactory.getLogger(HttpStatusWebhookAdapter.class);
    static final Duration TIMEOUT = Duration.ofSeconds(3);

    private final String url;
    private final String secret;
    private final HttpClient client;

    public HttpStatusWebhookAdapter(String url, String secret) {
        this.url = url == null ? "" : url.trim();
        this.secret = secret == null ? "" : secret;
        this.client = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
    }

    @Override
    public boolean enabled() {
        return !url.isEmpty();
    }

    @Override
    public WebhookResult post(String eventId, String payloadJson) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(TIMEOUT)
                    .header("Content-Type", "application/json")
                    .header("X-Webhook-Secret", secret)
                    .header("X-Event-Id", eventId)
                    .POST(HttpRequest.BodyPublishers.ofString(payloadJson))
                    .build();
            int status = client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
            return status >= 200 && status < 300 ? new WebhookResult(true, null)
                    : failure(eventId, "HTTP_" + status);
        } catch (HttpTimeoutException exception) {
            return failure(eventId, "TIMEOUT");
        } catch (IOException exception) {
            return failure(eventId, "IO_ERROR");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return failure(eventId, "INTERRUPTED");
        } catch (IllegalArgumentException exception) {
            return failure(eventId, "INVALID_URL");
        }
    }

    private static WebhookResult failure(String eventId, String code) {
        LOG.warn("Status webhook delivery failed for event {}: {}", eventId, code);
        return new WebhookResult(false, code);
    }
}
