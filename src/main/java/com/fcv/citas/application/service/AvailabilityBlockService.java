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

    /**
     * HU-013 — solo bloques propios (ajeno o inexistente → 404), futuros y sin compromisos; el nuevo
     * horario se revalida completo con las reglas de HU-012 ignorando el propio bloque en el solape.
     */
    @Override
    public AvailabilityBlock update(long userId, long blockId, BlockCommand command) {
        ProfessionalAccount account = account(userId);
        BlockSchedule schedule = new BlockSchedule(command.date(), command.startTime(), command.endTime());
        BlockValidator.validateShape(schedule).ifPresent(AvailabilityBlockService::reject);
        return transactions.required(() -> {
            LocalDateTime now = LocalDateTime.now(clock);
            requireEditable(account, blockId, now);
            long locationId = assignedLocation(account, command.locationCode());
            professionals.lock(account.professionalId());
            BlockValidator.validate(schedule, now, blocks.findActiveSchedules(account.professionalId(), schedule.date()),
                    blockId).ifPresent(AvailabilityBlockService::reject);
            blocks.update(blockId, locationId, schedule, clock.instant());
            blocks.deleteFreeSlots(blockId);
            blocks.insertSlots(blockId, schedule.slots());
            return blocks.findActiveOwned(blockId, account.professionalId()).orElseThrow();
        });
    }

    /** HU-013 — baja lógica: el bloque queda inactivo y se retiran sus slots libres. */
    @Override
    public void delete(long userId, long blockId) {
        ProfessionalAccount account = account(userId);
        transactions.required(() -> {
            requireEditable(account, blockId, LocalDateTime.now(clock));
            blocks.deleteFreeSlots(blockId);
            blocks.deactivate(blockId, clock.instant());
        });
    }

    private void requireEditable(ProfessionalAccount account, long blockId, LocalDateTime now) {
        blocks.findActiveOwned(blockId, account.professionalId())
                .orElseThrow(() -> new NotFoundException("Availability block not found"));
        blocks.lockBlock(blockId);
        AvailabilityBlock current = blocks.findActiveOwned(blockId, account.professionalId()).orElseThrow();
        current.notEditableReason(now).ifPresent(reason -> {
            throw switch (reason) {
                case PAST_BLOCK -> new BusinessRuleException("PAST_BLOCK", "A past block cannot be changed");
                case BLOCK_COMMITTED -> new BusinessRuleException("BLOCK_COMMITTED",
                        "The block has booked or held slots");
            };
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
