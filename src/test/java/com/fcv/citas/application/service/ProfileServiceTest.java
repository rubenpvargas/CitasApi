package com.fcv.citas.application.service;

import com.fcv.citas.application.exception.NotFoundException;
import com.fcv.citas.application.model.ProfileUpdateCommand;
import com.fcv.citas.application.port.out.ProfileRepositoryPort;
import com.fcv.citas.domain.model.UserProfile;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProfileServiceTest {
    private static final Instant NOW = Instant.parse("2026-10-02T15:00:00Z");
    private final InMemoryProfiles profiles = new InMemoryProfiles();
    private final ProfileService service = new ProfileService(profiles, new DirectTransactions(),
            Clock.fixed(NOW, ZoneId.of("America/Bogota")));

    @Test
    void returnsOnlyTheOwnersProfile() {
        profiles.put(new UserProfile(7L, "Ana", "Gomez", "ana@example.test", "CC", "1", "300", Set.of("USER")));
        profiles.put(new UserProfile(8L, "Otro", "Usuario", "otro@example.test", "CC", "2", "301", Set.of("USER")));

        assertThat(service.getProfile(7L).email()).isEqualTo("ana@example.test");
    }

    @Test
    void updatesOnlyContactFieldsTrimmedAndKeepsIdentifiers() {
        profiles.put(new UserProfile(7L, "Ana", "Gomez", "ana@example.test", "CC", "1", "300", Set.of("USER")));

        UserProfile updated = service.updateProfile(7L, new ProfileUpdateCommand("  Ana Maria ", " Gomez Ruiz ", " 3109998877 "));

        assertThat(updated.firstName()).isEqualTo("Ana Maria");
        assertThat(updated.lastName()).isEqualTo("Gomez Ruiz");
        assertThat(updated.phone()).isEqualTo("3109998877");
        assertThat(updated.email()).isEqualTo("ana@example.test");
        assertThat(updated.documentNumber()).isEqualTo("1");
        assertThat(profiles.lastUpdateAt).isEqualTo(NOW);
    }

    @Test
    void unknownOrInactiveUserIsNotFound() {
        assertThatThrownBy(() -> service.getProfile(99L)).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.updateProfile(99L, new ProfileUpdateCommand("A", "B", "1")))
                .isInstanceOf(NotFoundException.class);
    }

    private static final class InMemoryProfiles implements ProfileRepositoryPort {
        private final Map<Long, UserProfile> byId = new HashMap<>();
        private Instant lastUpdateAt;
        void put(UserProfile profile) { byId.put(profile.id(), profile); }
        @Override public Optional<UserProfile> findActiveById(long userId) { return Optional.ofNullable(byId.get(userId)); }
        @Override public boolean updateContact(long userId, String firstName, String lastName, String phone, Instant at) {
            UserProfile p = byId.get(userId);
            if (p == null) return false;
            byId.put(userId, new UserProfile(p.id(), firstName, lastName, p.email(), p.documentType(), p.documentNumber(), phone, p.roles()));
            lastUpdateAt = at;
            return true;
        }
    }
}
