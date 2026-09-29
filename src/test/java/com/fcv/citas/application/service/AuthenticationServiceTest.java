package com.fcv.citas.application.service;

import com.fcv.citas.application.exception.InvalidCredentialsException;
import com.fcv.citas.application.exception.InvalidRefreshTokenException;
import com.fcv.citas.application.model.RefreshTokenClaims;
import com.fcv.citas.application.model.TokenPair;
import com.fcv.citas.application.port.out.PasswordHashPort;
import com.fcv.citas.application.port.out.RefreshSessionRepositoryPort;
import com.fcv.citas.application.port.out.TokenHashPort;
import com.fcv.citas.application.port.out.TokenPort;
import com.fcv.citas.application.port.out.TransactionPort;
import com.fcv.citas.application.port.out.UserRepositoryPort;
import com.fcv.citas.domain.model.RefreshSession;
import com.fcv.citas.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthenticationServiceTest {
    private static final Instant NOW = Instant.parse("2026-09-22T12:00:00Z");
    private final User user = new User(7L, "Ana", "Gomez", "CC", "12345", "ana@example.test",
            "3001234567", "bcrypt-value", true, Set.of("USER"));
    private FakeSessions sessions;
    private FakeTokens tokens;
    private AuthenticationService service;

    @BeforeEach
    void setUp() {
        sessions = new FakeSessions();
        tokens = new FakeTokens();
        UserRepositoryPort users = new FixedUserRepository(user);
        PasswordHashPort passwords = new PasswordHashPort() {
            @Override public String hash(String rawPassword) { return "bcrypt-" + rawPassword; }
            @Override public boolean matches(String rawPassword, String encodedPassword) {
                return rawPassword.equals("correct-password") && encodedPassword.equals("bcrypt-value");
            }
        };
        TokenHashPort hashes = AuthenticationServiceTest::hash;
        TransactionPort transactions = new DirectTransactions();
        service = new AuthenticationService(users, sessions, passwords, tokens, hashes, transactions,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void loginEmitsSeparateAccessAndRefreshTokensAndPersistsOnlyRefreshHash() {
        var session = service.login("ANA@EXAMPLE.TEST", "correct-password");
        TokenPair pair = session.tokens();

        assertThat(session.user().firstName()).isEqualTo("Ana");
        assertThat(pair.accessToken()).isNotEqualTo(pair.refreshToken());
        RefreshSession saved = sessions.lastSaved();
        assertThat(saved.tokenHash()).isEqualTo(hash("refresh-1"));
        assertThat(saved.tokenHash()).doesNotContain(pair.refreshToken());
        assertThat(saved.jti()).isEqualTo(pair.refreshJti());
    }

    @Test
    void loginRejectsInvalidCredentialsWithoutIssuingASession() {
        assertThatThrownBy(() -> service.login("ana@example.test", "wrong-password"))
                .isInstanceOf(InvalidCredentialsException.class);
        assertThat(sessions.saved()).isEmpty();
    }

    @Test
    void refreshRotatesTheRefreshSessionAndRejectsReuseOfTheRevokedToken() {
        TokenPair original = service.login("ana@example.test", "correct-password").tokens();

        TokenPair replacement = service.refresh(original.refreshToken());

        assertThat(replacement.accessToken()).isNotEqualTo(original.accessToken());
        assertThat(replacement.refreshToken()).isNotEqualTo(original.refreshToken());
        RefreshSession oldSession = sessions.byHash(hash(original.refreshToken())).orElseThrow();
        assertThat(oldSession.revokedAt()).isEqualTo(NOW);
        assertThat(oldSession.replacedByJti()).isEqualTo(replacement.refreshJti());
        assertThat(sessions.byHash(hash(replacement.refreshToken()))).isPresent();
        assertThatThrownBy(() -> service.refresh(original.refreshToken()))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void logoutRevokesTheRefreshSessionAndPreventsFurtherRefresh() {
        TokenPair pair = service.login("ana@example.test", "correct-password").tokens();

        service.logout(pair.refreshToken());

        assertThat(sessions.byHash(hash(pair.refreshToken())).orElseThrow().revokedAt()).isEqualTo(NOW);
        assertThatThrownBy(() -> service.refresh(pair.refreshToken()))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void rejectsUnknownOrInvalidRefreshToken() {
        assertThatThrownBy(() -> service.refresh("unknown"))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    private static final class FixedUserRepository implements UserRepositoryPort {
        private final User user;
        private FixedUserRepository(User user) { this.user = user; }
        @Override public boolean existsByEmail(String email) { return false; }
        @Override public boolean existsByDocument(String type, String number) { return false; }
        @Override public User save(User ignored) { return user; }
        @Override public Optional<User> findByEmail(String email) {
            return user.email().equals(email) ? Optional.of(user) : Optional.empty();
        }
        @Override public Optional<User> findById(Long id) { return user.id().equals(id) ? Optional.of(user) : Optional.empty(); }
    }

    private static String hash(String token) {
        return Integer.toHexString(token.hashCode());
    }

    private static final class FakeSessions implements RefreshSessionRepositoryPort {
        private final Map<String, RefreshSession> byHash = new HashMap<>();
        private final List<RefreshSession> saved = new ArrayList<>();
        private long nextId = 1;
        @Override public RefreshSession save(RefreshSession session) {
            RefreshSession stored = session.id() == null
                    ? new RefreshSession(nextId++, session.userId(), session.tokenHash(), session.jti(), session.issuedAt(),
                    session.expiresAt(), session.revokedAt(), session.replacedByJti())
                    : session;
            byHash.put(stored.tokenHash(), stored);
            saved.add(stored);
            return stored;
        }
        @Override public Optional<RefreshSession> findByTokenHashForUpdate(String tokenHash) { return byHash(tokenHash); }
        Optional<RefreshSession> byHash(String hash) { return Optional.ofNullable(byHash.get(hash)); }
        RefreshSession lastSaved() { return saved.getLast(); }
        List<RefreshSession> saved() { return saved; }
    }

    private static final class FakeTokens implements TokenPort {
        private final Map<String, RefreshTokenClaims> claimsByToken = new HashMap<>();
        private int sequence;
        @Override public TokenPair issuePair(User ignored, Instant issuedAt) {
            sequence++;
            String refresh = "refresh-" + sequence;
            String jti = "jti-" + sequence;
            Instant expiresAt = issuedAt.plusSeconds(3600);
            claimsByToken.put(refresh, new RefreshTokenClaims(7L, jti, expiresAt));
            return new TokenPair("access-" + sequence, refresh, issuedAt.plusSeconds(900), expiresAt, jti);
        }
        @Override public RefreshTokenClaims parseRefresh(String token) {
            RefreshTokenClaims claims = claimsByToken.get(token);
            if (claims == null) throw new IllegalArgumentException("invalid refresh token");
            return claims;
        }
    }

    private static final class DirectTransactions implements TransactionPort {
        @Override public <T> T required(java.util.function.Supplier<T> work) { return work.get(); }
        @Override public void required(Runnable work) { work.run(); }
    }
}
