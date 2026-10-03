package com.fcv.citas.adapter.out.persistence;

import com.fcv.citas.application.port.out.PasswordResetTokenRepositoryPort;
import com.fcv.citas.domain.model.PasswordResetToken;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

@Component
public class PasswordResetTokenPersistenceAdapter implements PasswordResetTokenRepositoryPort {
    private final SpringDataPasswordResetTokenRepository tokens;

    public PasswordResetTokenPersistenceAdapter(SpringDataPasswordResetTokenRepository tokens) {
        this.tokens = tokens;
    }

    @Override
    public void invalidateActiveForUser(long userId, Instant at) {
        tokens.invalidateActiveForUser(userId, at);
    }

    @Override
    public PasswordResetToken save(PasswordResetToken token) {
        PasswordResetTokenJpaEntity entity;
        if (token.id() == null) {
            entity = new PasswordResetTokenJpaEntity(token.userId(), token.tokenHash(), token.createdAt(),
                    token.expiresAt(), token.usedAt(), token.invalidatedAt());
        } else {
            entity = tokens.findById(token.id())
                    .orElseThrow(() -> new IllegalStateException("Password reset token disappeared"));
            entity.updateState(token.usedAt(), token.invalidatedAt());
        }
        return toDomain(tokens.saveAndFlush(entity));
    }

    @Override
    public Optional<PasswordResetToken> findByTokenHashForUpdate(String tokenHash) {
        return tokens.findByTokenHashForUpdate(tokenHash).map(PasswordResetTokenPersistenceAdapter::toDomain);
    }

    private static PasswordResetToken toDomain(PasswordResetTokenJpaEntity entity) {
        return new PasswordResetToken(entity.getId(), entity.getUserId(), entity.getTokenHash(),
                entity.getCreatedAt(), entity.getExpiresAt(), entity.getUsedAt(), entity.getInvalidatedAt());
    }
}
