package com.fcv.citas.application.port.out;

import com.fcv.citas.domain.model.AvailabilityBlock;
import com.fcv.citas.domain.model.BlockSchedule;
import com.fcv.citas.domain.model.ExistingBlock;
import com.fcv.citas.domain.model.SlotTime;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AvailabilityBlockRepositoryPort {
    /** Bloques activos del profesional en la fecha, en cualquier sede. */
    List<ExistingBlock> findActiveSchedules(long professionalId, LocalDate date);

    long insert(long professionalId, long locationId, BlockSchedule schedule, Instant at);

    void insertSlots(long blockId, List<SlotTime> slots);

    /** Bloquea la fila del bloque y sus slots (frente a reservas concurrentes) hasta el fin de la transacción. */
    void lockBlock(long blockId);

    Optional<AvailabilityBlock> findActiveOwned(long blockId, long professionalId);

    void update(long blockId, long locationId, BlockSchedule schedule, Instant at);

    /** Elimina los slots no reservados ni retenidos del bloque. */
    void deleteFreeSlots(long blockId);

    void deactivate(long blockId, Instant at);

    List<AvailabilityBlock> findCalendar(long professionalId, LocalDate from, LocalDate to, String locationCode);
}
