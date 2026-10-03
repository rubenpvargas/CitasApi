package com.fcv.citas.application.service;

import com.fcv.citas.application.model.NewProfessionalCommand;
import com.fcv.citas.application.port.out.ProfessionalRepositoryPort;
import com.fcv.citas.domain.model.Location;
import com.fcv.citas.domain.model.ProfessionalAccount;
import com.fcv.citas.domain.model.ProfessionalCapabilitySpecialty;
import com.fcv.citas.domain.model.ProfessionalSummary;
import com.fcv.citas.domain.model.Specialty;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Repositorio en memoria de profesionales, sedes y especialidades para pruebas de casos de uso. */
final class InMemoryProfessionals implements ProfessionalRepositoryPort {
    final Map<Long, ProfessionalSummary> professionals = new LinkedHashMap<>();
    final Map<Long, String> documents = new LinkedHashMap<>();
    final Map<Long, String> passwordHashes = new LinkedHashMap<>();
    final Map<Long, Location> locations = new LinkedHashMap<>();
    final Map<Long, Specialty> specialties = new LinkedHashMap<>();
    final Map<Long, Long> userIdByProfessional = new LinkedHashMap<>();
    private long next = 100;

    @Override public boolean emailExists(String email) {
        return professionals.values().stream().anyMatch(p -> p.email().equals(email));
    }
    @Override public boolean documentExists(String documentType, String documentNumber) {
        return documents.containsValue(documentType + ":" + documentNumber);
    }
    @Override public boolean professionalCodeExists(String code) {
        return professionals.values().stream().anyMatch(p -> p.professionalCode().equals(code));
    }
    @Override public boolean licenseExists(String license) {
        return professionals.values().stream().anyMatch(p -> p.licenseNumber().equals(license));
    }
    @Override public long create(NewProfessionalCommand c, String passwordHash, Instant at) {
        long id = next++;
        long userId = next++;
        professionals.put(id, new ProfessionalSummary(id, userId, c.firstName(), c.lastName(), c.email(), c.phone(),
                c.professionalCode(), c.licenseNumber(), true, List.of(), List.of()));
        documents.put(id, c.documentType() + ":" + c.documentNumber());
        passwordHashes.put(id, passwordHash);
        userIdByProfessional.put(id, userId);
        return id;
    }
    @Override public List<ProfessionalSummary> findAll() { return new ArrayList<>(professionals.values()); }
    @Override public Optional<ProfessionalSummary> findById(long id) { return Optional.ofNullable(professionals.get(id)); }
    @Override public Optional<ProfessionalAccount> findAccountByUserId(long userId) {
        return professionals.values().stream().filter(p -> p.userId() == userId)
                .map(p -> new ProfessionalAccount(p.id(), p.userId(), p.active())).findFirst();
    }
    @Override public void lock(long professionalId) { }
    @Override public void replaceCapabilities(long id, Set<Long> specialtyIds, long primarySpecialtyId,
                                              Set<Long> locationIds, boolean active, Instant at) {
        ProfessionalSummary p = professionals.get(id);
        List<ProfessionalCapabilitySpecialty> specs = specialtyIds.stream().sorted().map(sid -> {
            Specialty s = specialties.get(sid);
            return new ProfessionalCapabilitySpecialty(sid, s.code(), s.name(), sid == primarySpecialtyId);
        }).toList();
        List<Location> locs = locationIds.stream().sorted().map(locations::get).toList();
        professionals.put(id, new ProfessionalSummary(id, p.userId(), p.firstName(), p.lastName(), p.email(), p.phone(),
                p.professionalCode(), p.licenseNumber(), active, specs, locs));
    }
    @Override public boolean isLocationAssigned(long professionalId, long locationId) {
        ProfessionalSummary p = professionals.get(professionalId);
        return p != null && p.locations().stream().anyMatch(l -> l.id() == locationId && l.active());
    }
    @Override public List<Location> findAllLocations() { return new ArrayList<>(locations.values()); }
    @Override public Optional<Location> findLocation(long id) { return Optional.ofNullable(locations.get(id)); }
    @Override public Optional<Location> findLocationByCode(String code) {
        return locations.values().stream().filter(l -> l.code().equals(code)).findFirst();
    }
}
