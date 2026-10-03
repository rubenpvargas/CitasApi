package com.fcv.citas.application.service;

import com.fcv.citas.application.exception.BusinessRuleException;
import com.fcv.citas.application.exception.InvalidPasswordException;
import com.fcv.citas.application.model.PasswordResetRequestResult;
import com.fcv.citas.application.port.in.PasswordRecoveryUseCase;
import com.fcv.citas.application.port.out.PasswordHashPort;
import com.fcv.citas.application.port.out.PasswordResetTokenRepositoryPort;
import com.fcv.citas.application.port.out.RefreshSessionRevocationPort;
import com.fcv.citas.application.port.out.ResetTokenGeneratorPort;
import com.fcv.citas.application.port.out.TokenHashPort;
import com.fcv.citas.application.port.out.TransactionPort;
import com.fcv.citas.application.port.out.UserCredentialPort;
import com.fcv.citas.application.port.out.UserRepositoryPort;
import com.fcv.citas.domain.model.PasswordPolicy;
import com.fcv.citas.domain.model.PasswordResetToken;
import com.fcv.citas.domain.model.User;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * HU-004 — recuperación de contraseña no enumerable con token temporal de un solo uso.
 *
 * <p>El token en claro nunca se persiste ni se registra: solo su hash. Una solicitud nueva invalida los
 * tokens previos sin usar; la confirmación bloquea la fila del token, lo consume, fija el nuevo hash
 * adaptativo y revoca las sesiones de refresh activas del usuario, todo en una transacción.
 */
public final class PasswordRecoveryService implements PasswordRecoveryUseCase {
    static final String INVALID_RESET_TOKEN = "INVALID_RESET_TOKEN";

    private final UserRepositoryPort users;
    private final PasswordResetTokenRepositoryPort tokens;
    private final UserCredentialPort credentials;
    private final RefreshSessionRevocationPort sessions;
    private final PasswordHashPort passwords;
    private final TokenHashPort tokenHashes;
    private final ResetTokenGeneratorPort generator;
    private final TransactionPort transactions;
    private final Clock clock;
    private final Duration tokenTtl;
    private final boolean exposeDevelopmentToken;

    public PasswordRecoveryService(UserRepositoryPort users, PasswordResetTokenRepositoryPort tokens,
                                   UserCredentialPort credentials, RefreshSessionRevocationPort sessions,
                                   PasswordHashPort passwords, TokenHashPort tokenHashes,
                                   ResetTokenGeneratorPort generator, TransactionPort transactions,
                                   Clock clock, Duration tokenTtl, boolean exposeDevelopmentToken) {
        if (tokenTtl == null || tokenTtl.isNegative() || tokenTtl.isZero()) {
            throw new IllegalArgumentException("Password reset token TTL must be positive");
        }
        this.users = users;
        this.tokens = tokens;
        this.credentials = credentials;
        this.sessions = sessions;
        this.passwords = passwords;
        this.tokenHashes = tokenHashes;
        this.generator = generator;
        this.transactions = transactions;
        this.clock = clock;
        this.tokenTtl = tokenTtl;
        this.exposeDevelopmentToken = exposeDevelopmentToken;
    }

    @Override
    public PasswordResetRequestResult requestReset(String email) {
        String normalized = RegisterUserService.normalizeEmail(email);
        Optional<String> issued = transactions.required(() -> users.findByEmail(normalized)
                .filter(User::active)
                .map(user -> issueFor(user.id())));
        return issued.filter(token -> exposeDevelopmentToken)
                .map(PasswordResetRequestResult::withDevelopmentToken)
                .orElseGet(PasswordResetRequestResult::none);
    }

    private String issueFor(long userId) {
        Instant now = clock.instant();
        tokens.invalidateActiveForUser(userId, now);
        String rawToken = generator.newToken();
        tokens.save(new PasswordResetToken(null, userId, tokenHashes.hash(rawToken), now,
                now.plus(tokenTtl), null, null));
        return rawToken;
    }

    @Override
    public void confirmReset(String token, String newPassword) {
        if (!PasswordPolicy.isSatisfiedBy(newPassword)) {
            throw new InvalidPasswordException();
        }
        if (token == null || token.isBlank()) {
            throw invalidToken();
        }
        String tokenHash = tokenHashes.hash(token);
        String newPasswordHash = passwords.hash(newPassword);
        transactions.required(() -> {
            Instant now = clock.instant();
            PasswordResetToken current = tokens.findByTokenHashForUpdate(tokenHash)
                    .filter(candidate -> candidate.isUsableAt(now))
                    .orElseThrow(PasswordRecoveryService::invalidToken);
            tokens.save(current.consume(now));
            credentials.updatePasswordHash(current.userId(), newPasswordHash, now);
            sessions.revokeAllForUser(current.userId(), now);
        });
    }

    private static BusinessRuleException invalidToken() {
        return new BusinessRuleException(INVALID_RESET_TOKEN, "The recovery token is invalid or expired");
    }
}
