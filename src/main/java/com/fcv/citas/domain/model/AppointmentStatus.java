package com.fcv.citas.domain.model;

/** Estados del ciclo de vida de una cita (catálogo fijo appointment_statuses). */
public enum AppointmentStatus {
    REQUESTED(false),
    APPROVED(false),
    REJECTED(true),
    CANCELLED(true),
    COMPLETED(true),
    NO_SHOW(true);

    private final boolean terminal;

    AppointmentStatus(boolean terminal) {
        this.terminal = terminal;
    }

    public boolean terminal() {
        return terminal;
    }
}
