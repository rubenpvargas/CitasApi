package com.fcv.citas.application.service;

import com.fcv.citas.application.port.out.AvailabilityBlockRepositoryPort;
import com.fcv.citas.domain.model.AvailabilityBlock;
import com.fcv.citas.domain.model.BlockSchedule;
import com.fcv.citas.domain.model.ExistingBlock;
import com.fcv.citas.domain.model.Location;
import com.fcv.citas.domain.model.SlotTime;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Bloques en memoria: professionalId, sede, horario, activo, slots y compromisos. */
final class InMemoryBlocks implements AvailabilityBlockRepositoryPort {
    final Map<Long, Row> rows = new LinkedHashMap<>();
    private final InMemoryProfessionals professionals;
    private long next = 1;

    InMemoryBlocks(InMemoryProfessionals professionals) {
        this.professionals = professionals;
    }

    static final class Row {
        long id;
        long professionalId;
        long locationId;
        BlockSchedule schedule;
        boolean active = true;
        List<SlotTime> slots = new ArrayList<>();
        int committed;
    }

    @Override public List<ExistingBlock> findActiveSchedules(long professionalId, LocalDate date) {
        return rows.values().stream().filter(r -> r.active && r.professionalId == professionalId && r.schedule.date().equals(date))
                .map(r -> new ExistingBlock(r.id, r.schedule)).toList();
    }
    @Override public long insert(long professionalId, long locationId, BlockSchedule schedule, Instant at) {
        Row r = new Row();
        r.id = next++; r.professionalId = professionalId; r.locationId = locationId; r.schedule = schedule;
        rows.put(r.id, r);
        return r.id;
    }
    @Override public void lockBlock(long blockId) { }
    @Override public void insertSlots(long blockId, List<SlotTime> slots) { rows.get(blockId).slots.addAll(slots); }
    @Override public Optional<AvailabilityBlock> findActiveOwned(long blockId, long professionalId) {
        Row r = rows.get(blockId);
        return r == null || !r.active || r.professionalId != professionalId ? Optional.empty() : Optional.of(toBlock(r));
    }
    @Override public void update(long blockId, long locationId, BlockSchedule schedule, Instant at) {
        Row r = rows.get(blockId); r.locationId = locationId; r.schedule = schedule;
    }
    @Override public void deleteFreeSlots(long blockId) { rows.get(blockId).slots.clear(); }
    @Override public void deactivate(long blockId, Instant at) { rows.get(blockId).active = false; }
    @Override public List<AvailabilityBlock> findCalendar(long professionalId, LocalDate from, LocalDate to, String locationCode) {
        return rows.values().stream().filter(r -> r.active && r.professionalId == professionalId
                        && !r.schedule.date().isBefore(from) && !r.schedule.date().isAfter(to))
                .map(this::toBlock)
                .filter(b -> locationCode == null || b.locationCode().equals(locationCode))
                .sorted(Comparator.comparing(b -> b.schedule().startAt())).toList();
    }

    private AvailabilityBlock toBlock(Row r) {
        Location l = professionals.locations.get(r.locationId);
        return new AvailabilityBlock(r.id, r.professionalId, l.code(), l.name(), r.schedule, r.slots.size(), r.committed);
    }
}
