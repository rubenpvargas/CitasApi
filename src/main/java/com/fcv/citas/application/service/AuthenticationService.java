package com.fcv.citas.application.service;

import com.fcv.citas.application.exception.InvalidCredentialsException;
import com.fcv.citas.application.exception.InvalidRefreshTokenException;
import com.fcv.citas.application.model.AuthenticatedSession;
import com.fcv.citas.application.model.RefreshTokenClaims;
import com.fcv.citas.application.model.TokenPair;
import com.fcv.citas.application.port.in.AuthenticationUseCase;
import com.fcv.citas.application.port.out.PasswordHashPort;
import com.fcv.citas.application.port.out.RefreshSessionRepositoryPort;
import com.fcv.citas.application.port.out.TokenHashPort;
import com.fcv.citas.application.port.out.TokenPort;
import com.fcv.citas.application.port.out.TransactionPort;
import com.fcv.citas.application.port.out.UserRepositoryPort;
import com.fcv.citas.domain.model.RefreshSession;
import com.fcv.citas.domain.model.User;

import java.time.Clock;
import java.time.Instant;

public final class AuthenticationService implements AuthenticationUseCase {
    private final UserRepositoryPort users;
    private final RefreshSessionRepositoryPort sessions;
    private final PasswordHashPort passwords;
    private final TokenPort tokens;
    private final TokenHashPort tokenHashes;
    private final TransactionPort transactions;
    private final Clock clock;

    public AuthenticationService(UserRepositoryPort users, RefreshSessionRepositoryPort sessions,
                                 PasswordHashPort passwords, TokenPort tokens, TokenHashPort tokenHashes,
                                 TransactionPort transactions, Clock clock) {
        this.users = users;
        this.sessions = sessions;
        this.passwords = passwords;
        this.tokens = tokens;
        this.tokenHashes = tokenHashes;
        this.transactions = transactions;
        this.clock = clock;
    }

    @Override
    public AuthenticatedSession login(String rawEmail, String password) {
        return transactions.required(() -> {
            User user = users.findByEmail(RegisterUserService.normalizeEmail(rawEmail))
                    .filter(User::active)
                    .orElseThrow(InvalidCredentialsException::new);
            if (!passwords.matches(password, user.passwordHash())) {
                throw new InvalidCredentialsException();
            }
            Instant now = clock.instant();
            TokenPair pair = tokens.issuePair(user, now);
            sessions.save(toSession(user, pair, now));
            return new AuthenticatedSession(user, pair);
        });
    }

    @Override
    public TokenPair refresh(String refreshToken) {
        RefreshTokenClaims claims = parse(refreshToken);
        String hash = tokenHashes.hash(refreshToken);
        return transactions.required(() -> {
            Instant now = clock.instant();
            RefreshSession current = sessions.findByTokenHashForUpdate(hash)
                    .filter(session -> session.isActiveAt(now))
                    .filter(session -> session.userId().equals(claims.userId()))
                    .filter(session -> session.jti().equals(claims.jti()))
                    .orElseThrow(InvalidRefreshTokenException::new);
            User user = users.findById(current.userId()).filter(User::active)
                    .orElseThrow(InvalidRefreshTokenException::new);
            TokenPair replacement = tokens.issuePair(user, now);
            sessions.save(current.revoke(now, replacement.refreshJti()));
            sessions.save(toSession(user, replacement, now));
            return replacement;
        });
    }

    @Override
    public void logout(String refreshToken) {
        RefreshTokenClaims claims = parse(refreshToken);
        String hash = tokenHashes.hash(refreshToken);
        transactions.required(() -> {
            Instant now = clock.instant();
            RefreshSession current = sessions.findByTokenHashForUpdate(hash)
                    .filter(session -> session.isActiveAt(now))
                    .filter(session -> session.userId().equals(claims.userId()))
                    .filter(session -> session.jti().equals(claims.jti()))
                    .orElseThrow(InvalidRefreshTokenException::new);
            sessions.save(current.revoke(now, null));
        });
    }

    private RefreshTokenClaims parse(String token) {
        try {
            return tokens.parseRefresh(token);
        } catch (RuntimeException exception) {
            throw new InvalidRefreshTokenException();
        }
    }

    private RefreshSession toSession(User user, TokenPair pair, Instant issuedAt) {
        return new RefreshSession(null, user.id(), tokenHashes.hash(pair.refreshToken()),
                pair.refreshJti(), issuedAt, pair.refreshExpiresAt(), null, null);
    }
}
