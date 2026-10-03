package com.fcv.citas.application.port.out;

import com.fcv.citas.domain.model.UserProfile;

import java.time.Instant;
import java.util.Optional;

public interface ProfileRepositoryPort {
    Optional<UserProfile> findActiveById(long userId);

    /** @return false si el usuario no existe o está inactivo */
    boolean updateContact(long userId, String firstName, String lastName, String phone, Instant at);
}
