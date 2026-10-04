package com.fcv.citas.domain.model;

/** Estado de entrega de un mensaje del outbox transaccional. */
public enum OutboxStatus {
    PENDING,
    SENT,
    FAILED
}
