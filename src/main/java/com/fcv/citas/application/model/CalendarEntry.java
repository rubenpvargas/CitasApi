package com.fcv.citas.application.model;

import com.fcv.citas.domain.model.AvailabilityBlock;
import com.fcv.citas.domain.model.BlockLockReason;

import java.util.Optional;

/** Bloque del calendario propio con su editabilidad evaluada respecto al reloj de la aplicación. */
public record CalendarEntry(AvailabilityBlock block, Optional<BlockLockReason> notEditableReason) {
    public boolean editable() {
        return notEditableReason.isEmpty();
    }
}
