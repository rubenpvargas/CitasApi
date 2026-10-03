package com.fcv.citas.application.service;

import com.fcv.citas.application.exception.BusinessRuleException;
import com.fcv.citas.application.exception.NotFoundException;
import com.fcv.citas.application.exception.RequestValidationException;
import com.fcv.citas.application.port.in.SpecialtyUseCase;
import com.fcv.citas.application.port.out.SpecialtyRepositoryPort;
import com.fcv.citas.application.port.out.TransactionPort;
import com.fcv.citas.domain.model.Specialty;

import java.util.List;

/**
 * HU-009 — duración 30/60, a lo sumo una especialidad general activa y sin borrado físico.
 * Cambiar la duración no altera citas existentes (conservan su scheduled_end_at).
 */
public final class SpecialtyService implements SpecialtyUseCase {
    private final SpecialtyRepositoryPort specialties;
    private final TransactionPort transactions;

    public SpecialtyService(SpecialtyRepositoryPort specialties, TransactionPort transactions) {
        this.specialties = specialties;
        this.transactions = transactions;
    }

    @Override
    public List<Specialty> listAll() {
        return specialties.findAll();
    }

    @Override
    public List<Specialty> listActive() {
        return specialties.findAll().stream().filter(Specialty::active).toList();
    }

    @Override
    public Specialty create(String code, String name, int durationMinutes, boolean general) {
        requireDuration(durationMinutes);
        String normalizedCode = CatalogCodes.normalize(code);
        String trimmedName = name.trim();
        return transactions.required(() -> {
            if (specialties.codeExists(normalizedCode)) {
                throw new BusinessRuleException("DUPLICATE_CODE", "A specialty with this code already exists");
            }
            if (specialties.nameExists(trimmedName, null)) {
                throw new BusinessRuleException("DUPLICATE_NAME", "A specialty with this name already exists");
            }
            if (general && specialties.activeGeneralExists(null)) {
                throw generalConflict();
            }
            return specialties.insert(normalizedCode, trimmedName, durationMinutes, general);
        });
    }

    @Override
    public Specialty update(long id, String name, int durationMinutes, boolean active) {
        requireDuration(durationMinutes);
        String trimmedName = name.trim();
        return transactions.required(() -> {
            Specialty current = specialties.findById(id).orElseThrow(() -> new NotFoundException("Specialty not found"));
            if (specialties.nameExists(trimmedName, id)) {
                throw new BusinessRuleException("DUPLICATE_NAME", "A specialty with this name already exists");
            }
            if (current.general() && active && specialties.activeGeneralExists(id)) {
                throw generalConflict();
            }
            specialties.update(id, trimmedName, durationMinutes, active);
            return specialties.findById(id).orElseThrow();
        });
    }

    private static void requireDuration(int minutes) {
        if (!Specialty.isAllowedDuration(minutes)) {
            throw new RequestValidationException("durationMinutes", "must be 30 or 60");
        }
    }

    private static BusinessRuleException generalConflict() {
        return new BusinessRuleException("GENERAL_SPECIALTY_CONFLICT", "Only one active general specialty is allowed");
    }
}
