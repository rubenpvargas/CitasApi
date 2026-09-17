package com.fcv.citas.adapter.out.persistence;

import com.fcv.citas.application.port.out.RefreshSessionRepositoryPort;
import com.fcv.citas.domain.model.RefreshSession;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class RefreshSessionPersistenceAdapter implements RefreshSessionRepositoryPort {
    private final SpringDataRefreshSessionRepository sessions;
    private final SpringDataUserRepository users;

    public RefreshSessionPersistenceAdapter(SpringDataRefreshSessionRepository sessions,
                                            SpringDataUserRepository users) {
        this.sessions = sessions;
        this.users = users;
    }

    @Override
    public RefreshSession save(RefreshSession session) {
        RefreshSessionJpaEntity entity;
        if (session.id() == null) {
            UserJpaEntity user = users.getReferenceById(session.userId());
            entity = new RefreshSessionJpaEntity(user, session.tokenHash(), session.jti(),
                    session.issuedAt(), session.expiresAt(), session.revokedAt(), session.replacedByJti());
        } else {
            entity = sessions.findById(session.id())
                    .orElseThrow(() -> new IllegalStateException("Refresh session disappeared"));
            entity.updateRevocation(session.revokedAt(), session.replacedByJti());
        }
        return toDomain(sessions.save(entity));
    }

    @Override
    public Optional<RefreshSession> findByTokenHashForUpdate(String tokenHash) {
        return sessions.findByTokenHashForUpdate(tokenHash).map(this::toDomain);
    }

    private RefreshSession toDomain(RefreshSessionJpaEntity entity) {
        return new RefreshSession(entity.getId(), entity.getUser().getId(), entity.getTokenHash(),
                entity.getJti(), entity.getIssuedAt(), entity.getExpiresAt(), entity.getRevokedAt(),
                entity.getReplacedByJti());
    }
}
