package com.fcv.citas.application.port.out;

import com.fcv.citas.domain.model.RefreshSession;

import java.util.Optional;

public interface RefreshSessionRepositoryPort {
    RefreshSession save(RefreshSession session);
    Optional<RefreshSession> findByTokenHashForUpdate(String tokenHash);
}
