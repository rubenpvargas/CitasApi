-- Ola G (WF-002): outbox transaccional de eventos de estado de citas.
--
-- Cada fila es un mensaje a entregar al webhook de n8n, insertado en la MISMA transacción que la
-- transición (APPOINTMENT_APPROVED|REJECTED|CANCELLED, RESCHEDULE_APPROVED|REJECTED): si la transición
-- hace rollback, no hay mensaje.
--
-- 3FN: todos los atributos dependen solo de id. event_id es clave candidata (UNIQUE, idempotencia del
-- receptor). appointment_id es FK; payload es el contenido inmutable del mensaje tal como ocurrió (un
-- hecho histórico, no una copia del estado vigente de la cita), por eso no se recalcula desde las tablas
-- de dominio ni introduce dependencias transitivas sobre datos mutables. status/attempts/next_attempt_at/
-- last_error_code/sent_at describen solo el estado de entrega de ese mensaje.
--
-- Índices: (status, next_attempt_at) sirve la consulta del despachador "PENDING vencidos" sin recorrer
-- mensajes entregados; (appointment_id) cubre la FK y la trazabilidad por cita.
CREATE TABLE notification_outbox (
    id BIGINT NOT NULL AUTO_INCREMENT,
    event_id CHAR(36) NOT NULL,
    correlation_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(40) NOT NULL,
    appointment_id BIGINT NOT NULL,
    payload JSON NOT NULL,
    status VARCHAR(10) NOT NULL DEFAULT 'PENDING',
    attempts SMALLINT UNSIGNED NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMP(6) NOT NULL,
    last_error_code VARCHAR(60) NULL,
    created_at TIMESTAMP(6) NOT NULL,
    sent_at TIMESTAMP(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_notification_outbox_event UNIQUE (event_id),
    CONSTRAINT ck_notification_outbox_status CHECK (status IN ('PENDING', 'SENT', 'FAILED')),
    CONSTRAINT ck_notification_outbox_type CHECK (event_type IN ('APPOINTMENT_APPROVED', 'APPOINTMENT_REJECTED',
        'APPOINTMENT_CANCELLED', 'RESCHEDULE_APPROVED', 'RESCHEDULE_REJECTED')),
    CONSTRAINT fk_notification_outbox_appointment FOREIGN KEY (appointment_id) REFERENCES appointments (id),
    INDEX ix_notification_outbox_dispatch (status, next_attempt_at),
    INDEX ix_notification_outbox_appointment (appointment_id)
);
