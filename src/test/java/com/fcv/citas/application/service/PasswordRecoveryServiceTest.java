package com.fcv.citas.application.service;

import com.fcv.citas.application.exception.BusinessRuleException;
import com.fcv.citas.application.exception.InvalidPasswordException;
import com.fcv.citas.application.model.PasswordResetRequestResult;
import com.fcv.citas.application.port.out.PasswordHashPort;
import com.fcv.citas.application.port.out.PasswordResetTokenRepositoryPort;
import com.fcv.citas.application.port.out.RefreshSessionRevocationPort;
import com.fcv.citas.application.port.out.ResetTokenGeneratorPort;
import com.fcv.citas.application.port.out.TokenHashPort;
import com.fcv.citas.application.port.out.TransactionPort;
import com.fcv.citas.application.port.out.UserCredentialPort;
import com.fcv.citas.application.port.out.UserRepositoryPort;
import com.fcv.citas.domain.model.PasswordResetToken;
import com.fcv.citas.domain.model.User;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
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

class PasswordRecoveryServiceTest {
    private static final Instant NOW = Instant.parse("2026-10-02T12:00:00Z");
    private static final Duration TTL = Duration.ofMinutes(30);
    private static final String NEW_PASSWORD = "NuevaClave2026";

    private final User user = new User(7L, "Ana", "Gomez", "CC", "12345", "ana@example.test",
            "3001234567", "old-hash", true, Set.of("USER"));
    private final User inactive = new User(8L, "Inactiva", "Gomez", "CC", "999", "off@example.test",
            "3001234567", "old-hash", false, Set.of("USER"));
    private final FakeTokens tokens = new FakeTokens();
    private final FakeCredentials credentials = new FakeCredentials();
    private final List<Long> revokedSessionsFor = new ArrayList<>();
    private final MutableClock clock = new MutableClock(NOW);

    private PasswordRecoveryService service(boolean exposeDevelopmentToken) {
        UserRepositoryPort users = new UsersByEmail(Map.of(user.email(), user, inactive.email(), inactive));
        PasswordHashPort passwords = new PasswordHashPort() {
            @Override public String hash(String raw) { return "bcrypt(" + raw + ")"; }
            @Override public boolean matches(String raw, String encoded) { return encoded.equals(hash(raw)); }
        };
        TokenHashPort hashes = raw -> "sha256(" + raw + ")";
        ResetTokenGeneratorPort generator = new SequenceGenerator();
        RefreshSessionRevocationPort sessions = (userId, at) -> revokedSessionsFor.add(userId);
        return new PasswordRecoveryService(users, tokens, credentials, sessions, passwords, hashes, generator,
                new DirectTransactions(), clock, TTL, exposeDevelopmentToken);
    }

    @Test
    void requestStoresOnlyTheHashWithExpiryAndHidesTokenByDefault() {
        PasswordResetRequestResult result = service(false).requestReset(" ANA@example.test ");

        assertThat(result.developmentToken()).isEmpty();
        PasswordResetToken stored = tokens.single();
        assertThat(stored.userId()).isEqualTo(7L);
        assertThat(stored.tokenHash()).isEqualTo("sha256(raw-token-1)");
        assertThat(stored.expiresAt()).isEqualTo(NOW.plus(TTL));
        assertThat(stored.usedAt()).isNull();
    }

    @Test
    void requestExposesDevelopmentTokenOnlyWhenFlagIsEnabled() {
        PasswordResetRequestResult result = service(true).requestReset("ana@example.test");

        assertThat(result.developmentToken()).contains("raw-token-1");
    }

    @Test
    void requestForUnknownOrInactiveEmailIsIndistinguishableAndCreatesNothing() {
        PasswordResetRequestResult unknown = service(false).requestReset("nobody@example.test");
        PasswordResetRequestResult off = service(false).requestReset("off@example.test");
        PasswordResetRequestResult known = service(false).requestReset("ana@example.test");

        assertThat(unknown).isEqualTo(known).isEqualTo(off);
        assertThat(tokens.all()).hasSize(1);
        assertThat(service(true).requestReset("nobody@example.test").developmentToken()).isEmpty();
    }

    @Test
    void newRequestInvalidatesPreviousUnusedTokensOfTheUser() {
        var service = service(true);
        String first = service.requestReset("ana@example.test").developmentToken().orElseThrow();
        String second = service.requestReset("ana@example.test").developmentToken().orElseThrow();

        assertThatThrownBy(() -> service.confirmReset(first, NEW_PASSWORD))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code").isEqualTo("INVALID_RESET_TOKEN");
        service.confirmReset(second, NEW_PASSWORD);
        assertThat(credentials.hashFor(7L)).isEqualTo("bcrypt(" + NEW_PASSWORD + ")");
    }

    @Test
    void confirmUpdatesHashConsumesTokenAndRevokesRefreshSessions() {
        var service = service(true);
        String token = service.requestReset("ana@example.test").developmentToken().orElseThrow();

        service.confirmReset(token, NEW_PASSWORD);

        assertThat(credentials.hashFor(7L)).isEqualTo("bcrypt(" + NEW_PASSWORD + ")");
        assertThat(tokens.single().usedAt()).isEqualTo(NOW);
        assertThat(revokedSessionsFor).containsExactly(7L);
    }

    @Test
    void tokenIsSingleUse() {
        var service = service(true);
        String token = service.requestReset("ana@example.test").developmentToken().orElseThrow();
        service.confirmReset(token, NEW_PASSWORD);

        assertThatThrownBy(() -> service.confirmReset(token, "OtraClave2026"))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code").isEqualTo("INVALID_RESET_TOKEN");
        assertThat(credentials.hashFor(7L)).isEqualTo("bcrypt(" + NEW_PASSWORD + ")");
    }

    @Test
    void expiredTokenIsRejectedWithoutChangingPassword() {
        var service = service(true);
        String token = service.requestReset("ana@example.test").developmentToken().orElseThrow();
        clock.set(NOW.plus(TTL));

        assertThatThrownBy(() -> service.confirmReset(token, NEW_PASSWORD))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code").isEqualTo("INVALID_RESET_TOKEN");
        assertThat(credentials.hashFor(7L)).isNull();
        assertThat(tokens.single().usedAt()).isNull();
    }

    @Test
    void unknownTokenIsRejected() {
        assertThatThrownBy(() -> service(false).confirmReset("never-issued", NEW_PASSWORD))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code").isEqualTo("INVALID_RESET_TOKEN");
        assertThat(revokedSessionsFor).isEmpty();
    }

    @Test
    void weakNewPasswordIsRejectedBeforeConsumingTheToken() {
        var service = service(true);
        String token = service.requestReset("ana@example.test").developmentToken().orElseThrow();

        assertThatThrownBy(() -> service.confirmReset(token, "debil"))
                .isInstanceOf(InvalidPasswordException.class);
        assertThat(tokens.single().usedAt()).isNull();
        assertThat(credentials.hashFor(7L)).isNull();
    }

    private static final class FakeTokens implements PasswordResetTokenRepositoryPort {
        private final Map<Long, PasswordResetToken> byId = new HashMap<>();
        private long nextId = 1;

        @Override public void invalidateActiveForUser(long userId, Instant at) {
            byId.replaceAll((id, t) -> t.userId() == userId && t.usedAt() == null && t.invalidatedAt() == null
                    ? t.invalidate(at) : t);
        }
        @Override public PasswordResetToken save(PasswordResetToken token) {
            PasswordResetToken stored = token.id() == null
                    ? new PasswordResetToken(nextId++, token.userId(), token.tokenHash(), token.createdAt(),
                    token.expiresAt(), token.usedAt(), token.invalidatedAt())
                    : token;
            byId.put(stored.id(), stored);
            return stored;
        }
        @Override public Optional<PasswordResetToken> findByTokenHashForUpdate(String tokenHash) {
            return byId.values().stream().filter(t -> t.tokenHash().equals(tokenHash)).findFirst();
        }
        List<PasswordResetToken> all() { return List.copyOf(byId.values()); }
        PasswordResetToken single() { assertThat(byId).hasSize(1); return byId.values().iterator().next(); }
    }

    private static final class FakeCredentials implements UserCredentialPort {
        private final Map<Long, String> hashes = new HashMap<>();
        @Override public void updatePasswordHash(long userId, String passwordHash, Instant at) { hashes.put(userId, passwordHash); }
        String hashFor(long userId) { return hashes.get(userId); }
    }

    private static final class SequenceGenerator implements ResetTokenGeneratorPort {
        private int sequence;
        @Override public String newToken() { return "raw-token-" + (++sequence); }
    }

    private record UsersByEmail(Map<String, User> users) implements UserRepositoryPort {
        @Override public boolean existsByEmail(String email) { return users.containsKey(email); }
        @Override public boolean existsByDocument(String type, String number) { return false; }
        @Override public User save(User user) { throw new UnsupportedOperationException(); }
        @Override public Optional<User> findByEmail(String email) { return Optional.ofNullable(users.get(email)); }
        @Override public Optional<User> findById(Long id) {
            return users.values().stream().filter(u -> u.id().equals(id)).findFirst();
        }
    }

    private static final class MutableClock extends Clock {
        private Instant now;
        MutableClock(Instant now) { this.now = now; }
        void set(Instant now) { this.now = now; }
        @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }

    private static final class DirectTransactions implements TransactionPort {
        @Override public <T> T required(java.util.function.Supplier<T> work) { return work.get(); }
        @Override public void required(Runnable work) { work.run(); }
    }
}
