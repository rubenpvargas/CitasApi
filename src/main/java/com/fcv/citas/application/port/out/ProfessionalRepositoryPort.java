package com.fcv.citas.application.port.out;

import com.fcv.citas.application.model.NewProfessionalCommand;
import com.fcv.citas.domain.model.Location;
import com.fcv.citas.domain.model.ProfessionalAccount;
import com.fcv.citas.domain.model.ProfessionalSummary;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ProfessionalRepositoryPort {
    boolean emailExists(String email);

    boolean documentExists(String documentType, String documentNumber);

    boolean professionalCodeExists(String code);

    boolean licenseExists(String license);

    /** Crea usuario con rol PROFESSIONAL y su perfil profesional activo; devuelve el id del profesional. */
    long create(NewProfessionalCommand command, String passwordHash, Instant at);

    List<ProfessionalSummary> findAll();

    Optional<ProfessionalSummary> findById(long id);

    Optional<ProfessionalAccount> findAccountByUserId(long userId);

    /** Bloquea la fila del profesional hasta el fin de la transacción (serializa agenda y capacidades). */
    void lock(long professionalId);

    void replaceCapabilities(long id, Set<Long> specialtyIds, long primarySpecialtyId, Set<Long> locationIds,
                             boolean active, Instant at);

    /** Sede asignada (activa en el profesional) y activa en el catálogo. */
    boolean isLocationAssigned(long professionalId, long locationId);

    List<Location> findAllLocations();

    Optional<Location> findLocation(long id);

    Optional<Location> findLocationByCode(String code);
}
