package com.fcv.citas.application.service;

import com.fcv.citas.application.exception.BusinessRuleException;
import com.fcv.citas.application.exception.ForbiddenException;
import com.fcv.citas.application.exception.NotFoundException;
import com.fcv.citas.application.exception.RequestValidationException;
import com.fcv.citas.application.model.BlockCommand;
import com.fcv.citas.application.port.in.AvailabilityBlockUseCase;
import com.fcv.citas.application.port.out.AvailabilityBlockRepositoryPort;
import com.fcv.citas.application.port.out.ProfessionalRepositoryPort;
import com.fcv.citas.application.port.out.TransactionPort;
import com.fcv.citas.domain.model.AvailabilityBlock;
import com.fcv.citas.domain.model.BlockSchedule;
import com.fcv.citas.domain.model.BlockValidator;
import com.fcv.citas.domain.model.BlockViolation;
import com.fcv.citas.domain.model.Location;
import com.fcv.citas.domain.model.ProfessionalAccount;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * HU-012 — publicación de bloques del PROFESSIONAL autenticado. Las reglas temporales y de solape son
 * del dominio ({@link BlockValidator}); aquí se resuelven actor, estado, sede y persistencia, bajo un
 * bloqueo de fila del profesional que serializa publicaciones concurrentes.
 */
public final class AvailabilityBlockService implements AvailabilityBlockUseCase {
    private final AvailabilityBlockRepositoryPort blocks;
    private final ProfessionalRepositoryPort professionals;
    private final TransactionPort transactions;
    private final Clock clock;

    public AvailabilityBlockService(AvailabilityBlockRepositoryPort blocks, ProfessionalRepositoryPort professionals,
                                    TransactionPort transactions, Clock clock) {
        this.blocks = blocks;
        this.professionals = professionals;
        this.transactions = transactions;
        this.clock = clock;
    }

    @Override
    public AvailabilityBlock create(long userId, BlockCommand command) {
        ProfessionalAccount account = account(userId);
        BlockSchedule schedule = new BlockSchedule(command.date(), command.startTime(), command.endTime());
        BlockValidator.validateShape(schedule).ifPresent(AvailabilityBlockService::reject);
        return transactions.required(() -> {
            long locationId = assignedLocation(account, command.locationCode());
            professionals.lock(account.professionalId());
            BlockValidator.validate(schedule, LocalDateTime.now(clock),
                    blocks.findActiveSchedules(account.professionalId(), schedule.date()), null)
                    .ifPresent(AvailabilityBlockService::reject);
            long id = blocks.insert(account.professionalId(), locationId, schedule, clock.instant());
            blocks.insertSlots(id, schedule.slots());
            return blocks.findActiveOwned(id, account.professionalId()).orElseThrow();
        });
    }

    ProfessionalAccount account(long userId) {
        return professionals.findAccountByUserId(userId)
                .orElseThrow(() -> new ForbiddenException("An active professional profile is required"));
    }

    long assignedLocation(ProfessionalAccount account, String locationCode) {
        if (!account.active()) {
            throw new BusinessRuleException("PROFESSIONAL_INACTIVE", "The professional is not active");
        }
        Location location = professionals.findLocationByCode(locationCode.trim().toUpperCase(java.util.Locale.ROOT))
                .orElseThrow(() -> new NotFoundException("Location not found"));
        if (!professionals.isLocationAssigned(account.professionalId(), location.id())) {
            throw new BusinessRuleException("LOCATION_NOT_ASSIGNED", "The professional is not enabled at this location");
        }
        return location.id();
    }

    static void reject(BlockViolation violation) {
        throw switch (violation) {
            case START_NOT_ALIGNED -> new RequestValidationException("startTime", "must be aligned to :00 or :30");
            case END_NOT_ALIGNED -> new RequestValidationException("endTime", "must be aligned to :00 or :30");
            case END_NOT_AFTER_START -> new RequestValidationException("endTime", "must be after startTime");
            case PAST_BLOCK -> new BusinessRuleException("PAST_BLOCK", "A block must start in the future");
            case BLOCK_OVERLAP -> new BusinessRuleException("BLOCK_OVERLAP",
                    "The block overlaps another active block of the professional");
        };
    }
}
