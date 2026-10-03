package com.fcv.citas.domain.model;

/** Motivos por los que un bloque de disponibilidad no es publicable. */
public enum BlockViolation {
    START_NOT_ALIGNED,
    END_NOT_ALIGNED,
    END_NOT_AFTER_START,
    PAST_BLOCK,
    BLOCK_OVERLAP
}
