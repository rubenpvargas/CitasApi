package com.fcv.citas.domain.model;

/** Estados de una solicitud de reprogramación (catálogo fijo reschedule_request_statuses). */
public enum RescheduleStatus {
    PENDING,
    APPROVED,
    REJECTED,
    /** Cerrada por el sistema al cancelarse la cita. */
    CANCELLED
}
