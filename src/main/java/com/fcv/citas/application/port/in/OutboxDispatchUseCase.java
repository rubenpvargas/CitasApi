package com.fcv.citas.application.port.in;

/** WF-002 — despacho de los mensajes del outbox vencidos. */
public interface OutboxDispatchUseCase {
    /** @return número de mensajes intentados en esta pasada */
    int dispatchDue();
}
