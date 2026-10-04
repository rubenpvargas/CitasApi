package com.fcv.citas.application.model;

/** Resultado de un envío al webhook: entregado (2xx) o código de error estable (HTTP_5xx, TIMEOUT, IO_ERROR). */
public record WebhookResult(boolean delivered, String errorCode) {
}
