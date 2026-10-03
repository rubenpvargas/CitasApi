package com.fcv.citas.application.port.out;

import com.fcv.citas.domain.model.Affiliation;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AffiliationRepositoryPort {
    /** Serializa cambios concurrentes de afiliación del mismo usuario. */
    void lockOwner(long userId);

    List<Affiliation> findCurrent(long userId);

    Optional<Long> findId(long userId, long planId, String membershipNumber);

    void closeCurrent(long userId, LocalDate validTo);

    void reactivate(long id, LocalDate validFrom);

    void insert(long userId, long planId, String membershipNumber, LocalDate validFrom, Instant at);
}
