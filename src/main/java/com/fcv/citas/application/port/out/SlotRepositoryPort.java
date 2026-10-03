package com.fcv.citas.application.port.out;

import com.fcv.citas.domain.model.LockedSlot;

import java.time.LocalDateTime;
import java.util.List;

/** Ocupación de slots: reservas (appointment_id) y retenciones de reprogramación (reschedule_request_id). */
public interface SlotRepositoryPort {
    /**
     * Lee con {@code SELECT … FOR UPDATE} los slots libres de bloques activos del profesional en la sede
     * cuyo intervalo cae en [from, to). Una transacción concurrente espera y luego relee el estado vigente.
     */
    List<LockedSlot> lockFreeSlots(long professionalId, long locationId, LocalDateTime from, LocalDateTime to);

    void assignToAppointment(List<Long> slotIds, long appointmentId);

    void holdForReschedule(List<Long> slotIds, long rescheduleRequestId);

    /** Libera los slots asignados a la cita (no toca retenciones). */
    void releaseAppointment(long appointmentId);

    void releaseHold(long rescheduleRequestId);

    /** Los slots retenidos por la solicitud pasan a la cita. */
    void transferHoldToAppointment(long rescheduleRequestId, long appointmentId);
}
