package com.fcv.citas.application.service;

import com.fcv.citas.application.exception.BusinessRuleException;
import com.fcv.citas.application.exception.InvalidPasswordException;
import com.fcv.citas.application.exception.NotFoundException;
import com.fcv.citas.application.model.CapabilitiesCommand;
import com.fcv.citas.application.model.NewProfessionalCommand;
import com.fcv.citas.application.port.in.ProfessionalAdminUseCase;
import com.fcv.citas.application.port.out.PasswordHashPort;
import com.fcv.citas.application.port.out.ProfessionalRepositoryPort;
import com.fcv.citas.application.port.out.SpecialtyRepositoryPort;
import com.fcv.citas.application.port.out.TransactionPort;
import com.fcv.citas.domain.model.Location;
import com.fcv.citas.domain.model.PasswordPolicy;
import com.fcv.citas.domain.model.ProfessionalSummary;
import com.fcv.citas.domain.model.Specialty;

import java.time.Clock;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * HU-010 — ADMIN crea profesionales sintéticos (usuario PROFESSIONAL + perfil activo) con
 * identificadores normalizados y únicos. HU-011 — capacidades consistentes y estado operativo.
 */
public final class ProfessionalAdminService implements ProfessionalAdminUseCase {
    private final ProfessionalRepositoryPort professionals;
    private final SpecialtyRepositoryPort specialties;
    private final PasswordHashPort passwords;
    private final TransactionPort transactions;
    private final Clock clock;

    public ProfessionalAdminService(ProfessionalRepositoryPort professionals, SpecialtyRepositoryPort specialties,
                                    PasswordHashPort passwords, TransactionPort transactions, Clock clock) {
        this.professionals = professionals;
        this.specialties = specialties;
        this.passwords = passwords;
        this.transactions = transactions;
        this.clock = clock;
    }

    @Override
    public ProfessionalSummary create(NewProfessionalCommand raw) {
        if (!PasswordPolicy.isSatisfiedBy(raw.password())) {
            throw new InvalidPasswordException();
        }
        NewProfessionalCommand command = new NewProfessionalCommand(raw.firstName().trim(), raw.lastName().trim(),
                upper(raw.documentType()), upper(raw.documentNumber()), RegisterUserService.normalizeEmail(raw.email()),
                raw.phone().trim(), raw.password(), upper(raw.professionalCode()), raw.licenseNumber().trim());
        String hash = passwords.hash(command.password());
        return transactions.required(() -> {
            if (professionals.emailExists(command.email())) {
                throw duplicate("DUPLICATE_EMAIL", "email");
            }
            if (professionals.documentExists(command.documentType(), command.documentNumber())) {
                throw duplicate("DUPLICATE_DOCUMENT", "document");
            }
            if (professionals.professionalCodeExists(command.professionalCode())) {
                throw duplicate("DUPLICATE_PROFESSIONAL_CODE", "professional code");
            }
            if (professionals.licenseExists(command.licenseNumber())) {
                throw duplicate("DUPLICATE_LICENSE", "license number");
            }
            long id = professionals.create(command, hash, clock.instant());
            return professionals.findById(id).orElseThrow();
        });
    }

    @Override
    public List<ProfessionalSummary> list() {
        return professionals.findAll();
    }

    @Override
    public ProfessionalSummary configure(long professionalId, CapabilitiesCommand command) {
        Set<Long> specialtyIds = new LinkedHashSet<>(command.specialtyIds());
        Set<Long> locationIds = new LinkedHashSet<>(command.locationIds());
        return transactions.required(() -> {
            professionals.findById(professionalId).orElseThrow(() -> new NotFoundException("Professional not found"));
            professionals.lock(professionalId);
            if (!specialtyIds.contains(command.primarySpecialtyId())) {
                throw new BusinessRuleException("PRIMARY_NOT_ASSIGNED",
                        "The primary specialty must be one of the assigned specialties");
            }
            for (Long id : specialtyIds) {
                Specialty specialty = specialties.findById(id).orElseThrow(() -> new NotFoundException("Specialty not found"));
                if (!specialty.active()) {
                    throw inactive();
                }
            }
            for (Long id : locationIds) {
                Location location = professionals.findLocation(id).orElseThrow(() -> new NotFoundException("Location not found"));
                if (!location.active()) {
                    throw inactive();
                }
            }
            professionals.replaceCapabilities(professionalId, specialtyIds, command.primarySpecialtyId(), locationIds,
                    command.active(), clock.instant());
            return professionals.findById(professionalId).orElseThrow();
        });
    }

    @Override
    public List<Location> locations() {
        return professionals.findAllLocations();
    }

    private static String upper(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private static BusinessRuleException duplicate(String code, String what) {
        return new BusinessRuleException(code, "A user or professional with this " + what + " already exists");
    }

    private static BusinessRuleException inactive() {
        return new BusinessRuleException("CATALOG_INACTIVE", "An assigned specialty or location is inactive");
    }
}
