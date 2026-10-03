-- Ola E/F (HU-020..HU-024): auditoría de reprogramaciones, fuente PROFESSIONAL y FK de retención.
--
-- 1) appointment_status_history.change_source admite PROFESSIONAL: el cierre de atención (HU-024) lo
--    registra el profesional. La fuente es un atributo de cada transición (depende solo de la PK del
--    historial), por lo que basta ampliar el dominio del CHECK; no cambia la forma normal.
ALTER TABLE appointment_status_history DROP CHECK ck_history_source;
ALTER TABLE appointment_status_history
    ADD CONSTRAINT ck_history_source CHECK (change_source IN ('SYSTEM', 'USER', 'ADMIN', 'PROFESSIONAL'));

-- 2) Historial append-only de cada transición de una solicitud de reprogramación (creación PENDING,
--    decisión APPROVED/REJECTED, cierre CANCELLED por cancelación de la cita). 3FN: cada fila depende
--    solo de su id; estado, actor y solicitud son FK a sus tablas (sin atributos derivados repetidos).
--    El índice (reschedule_request_id, changed_at) sirve la consulta cronológica por solicitud y cubre
--    la FK.
CREATE TABLE reschedule_request_history (
    id BIGINT NOT NULL AUTO_INCREMENT,
    reschedule_request_id BIGINT NOT NULL,
    status_id SMALLINT UNSIGNED NOT NULL,
    changed_by_user_id BIGINT NULL,
    change_source VARCHAR(20) NOT NULL,
    reason VARCHAR(500) NULL,
    changed_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    INDEX ix_reschedule_history_request (reschedule_request_id, changed_at),
    CONSTRAINT ck_reschedule_history_source CHECK (change_source IN ('SYSTEM', 'USER', 'ADMIN', 'PROFESSIONAL')),
    CONSTRAINT fk_reschedule_history_request FOREIGN KEY (reschedule_request_id) REFERENCES reschedule_requests (id),
    CONSTRAINT fk_reschedule_history_status FOREIGN KEY (status_id) REFERENCES reschedule_request_statuses (id),
    CONSTRAINT fk_reschedule_history_user FOREIGN KEY (changed_by_user_id) REFERENCES users (id)
);

-- 3) La retención de un slot por una reprogramación debe apuntar a una solicitud existente. V6 añadió
--    la columna y el índice ix_slot_reschedule_request (que respalda esta FK) sin la restricción.
ALTER TABLE professional_slots
    ADD CONSTRAINT fk_slot_reschedule_request FOREIGN KEY (reschedule_request_id) REFERENCES reschedule_requests (id);
