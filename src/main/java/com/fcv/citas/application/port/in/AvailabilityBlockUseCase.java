package com.fcv.citas.application.port.in;

import com.fcv.citas.application.model.BlockCommand;
import com.fcv.citas.domain.model.AvailabilityBlock;

/** HU-012..HU-014 — agenda propia del PROFESSIONAL autenticado. */
public interface AvailabilityBlockUseCase {
    AvailabilityBlock create(long userId, BlockCommand command);
}
