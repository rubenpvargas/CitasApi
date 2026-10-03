package com.fcv.citas.application.port.out;

import com.fcv.citas.application.model.CandidateSlot;

import java.time.LocalDate;
import java.util.List;

public interface AvailabilityQueryPort {
    /**
     * Slots libres (sin cita ni retención) en [from, to] de bloques activos de profesionales activos con la
     * especialidad asignada y activa en el profesional y la sede del bloque asignada y activa.
     */
    List<CandidateSlot> findFreeSlots(long specialtyId, LocalDate from, LocalDate to, String locationCode,
                                      Long professionalId);
}
