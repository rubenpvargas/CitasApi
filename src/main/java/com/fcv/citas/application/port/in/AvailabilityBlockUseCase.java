package com.fcv.citas.application.port.in;

import com.fcv.citas.application.model.BlockCommand;
import com.fcv.citas.domain.model.AvailabilityBlock;

/** HU-012..HU-014 — agenda propia del PROFESSIONAL autenticado. */
public interface AvailabilityBlockUseCase {
    AvailabilityBlock create(long userId, BlockCommand command);

    AvailabilityBlock update(long userId, long blockId, BlockCommand command);

    void delete(long userId, long blockId);

    /** HU-014 — bloques propios en [from, to] (máx. 31 días), opcionalmente por sede. */
    java.util.List<com.fcv.citas.application.model.CalendarEntry> calendar(long userId, java.time.LocalDate from,
                                                                        java.time.LocalDate to, String locationCode);
}
