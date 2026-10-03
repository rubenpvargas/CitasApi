package com.fcv.citas.application.service;

import com.fcv.citas.application.exception.NotFoundException;
import com.fcv.citas.application.model.AvailabilityOffer;
import com.fcv.citas.application.model.AvailabilityQuery;
import com.fcv.citas.application.model.CandidateSlot;
import com.fcv.citas.application.port.in.AvailabilityQueryUseCase;
import com.fcv.citas.application.port.out.AvailabilityQueryPort;
import com.fcv.citas.application.port.out.SpecialtyRepositoryPort;
import com.fcv.citas.domain.model.AvailabilityCalculator;
import com.fcv.citas.domain.model.FreeSlot;
import com.fcv.citas.domain.model.Specialty;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * HU-015 — la duración la fija la especialidad (catálogo), el cálculo de inicios consecutivos es del
 * dominio ({@link AvailabilityCalculator}) y las exclusiones de estado se aplican en la consulta.
 */
public final class AvailabilityQueryService implements AvailabilityQueryUseCase {
    private final AvailabilityQueryPort query;
    private final SpecialtyRepositoryPort specialties;
    private final Clock clock;

    public AvailabilityQueryService(AvailabilityQueryPort query, SpecialtyRepositoryPort specialties, Clock clock) {
        this.query = query;
        this.specialties = specialties;
        this.clock = clock;
    }

    @Override
    public List<AvailabilityOffer> search(AvailabilityQuery q) {
        DateRanges.require(q.from(), q.to());
        Specialty specialty = specialties.findById(q.specialtyId())
                .orElseThrow(() -> new NotFoundException("Specialty not found"));
        if (!specialty.active()) {
            return List.of();
        }
        String location = q.locationCode() == null || q.locationCode().isBlank() ? null
                : q.locationCode().trim().toUpperCase(Locale.ROOT);
        List<CandidateSlot> candidates = query.findFreeSlots(specialty.id(), q.from(), q.to(), location, q.professionalId());
        Map<Long, CandidateSlot> blockInfo = new HashMap<>();
        candidates.forEach(c -> blockInfo.putIfAbsent(c.blockId(), c));
        List<FreeSlot> free = candidates.stream().map(c -> new FreeSlot(c.blockId(), c.startAt())).toList();
        return AvailabilityCalculator.calculate(free, specialty.durationMinutes(), LocalDateTime.now(clock)).stream()
                .map(start -> {
                    CandidateSlot info = blockInfo.get(start.blockId());
                    return new AvailabilityOffer(info.professionalId(), info.professionalName(), specialty.id(),
                            specialty.name(), specialty.general(), info.locationCode(), info.locationName(),
                            start.startAt(), start.endAt(), specialty.durationMinutes());
                })
                .toList();
    }
}
