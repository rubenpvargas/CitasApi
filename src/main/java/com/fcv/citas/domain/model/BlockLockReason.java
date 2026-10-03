package com.fcv.citas.domain.model;

/** Motivo por el que un bloque publicado ya no admite edición ni baja. */
public enum BlockLockReason {
    /** El bloque ya comenzó o está en el pasado: es inmutable. */
    PAST_BLOCK,
    /** Tiene slots reservados por una cita o retenidos por una reprogramación pendiente. */
    BLOCK_COMMITTED
}
