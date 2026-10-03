package com.fcv.citas.application.service;

import com.fcv.citas.application.exception.NotFoundException;
import com.fcv.citas.application.model.ProfileUpdateCommand;
import com.fcv.citas.application.port.in.ProfileUseCase;
import com.fcv.citas.application.port.out.ProfileRepositoryPort;
import com.fcv.citas.application.port.out.TransactionPort;
import com.fcv.citas.domain.model.UserProfile;

import java.time.Clock;

/** HU-005 — perfil propio; el titular siempre se deriva del sub del JWT. */
public final class ProfileService implements ProfileUseCase {
    private final ProfileRepositoryPort profiles;
    private final TransactionPort transactions;
    private final Clock clock;

    public ProfileService(ProfileRepositoryPort profiles, TransactionPort transactions, Clock clock) {
        this.profiles = profiles;
        this.transactions = transactions;
        this.clock = clock;
    }

    @Override
    public UserProfile getProfile(long userId) {
        return profiles.findActiveById(userId).orElseThrow(() -> new NotFoundException("Profile not found"));
    }

    @Override
    public UserProfile updateProfile(long userId, ProfileUpdateCommand command) {
        return transactions.required(() -> {
            if (!profiles.updateContact(userId, command.firstName().trim(), command.lastName().trim(),
                    command.phone().trim(), clock.instant())) {
                throw new NotFoundException("Profile not found");
            }
            return getProfile(userId);
        });
    }
}
