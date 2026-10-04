package com.fcv.citas.application.port.out;

import com.fcv.citas.application.model.WebhookResult;

/** Webhook de n8n para WF-002. */
public interface StatusWebhookPort {
    /** Falso si no hay URL configurada: los mensajes permanecen PENDING. */
    boolean enabled();

    WebhookResult post(String eventId, String payloadJson);
}
