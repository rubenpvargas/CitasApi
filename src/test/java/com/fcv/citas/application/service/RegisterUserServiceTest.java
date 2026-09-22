package com.fcv.citas.application.service;

import com.fcv.citas.application.exception.DuplicateIdentifierException;
import com.fcv.citas.application.model.RegisterCommand;
import com.fcv.citas.application.port.out.PasswordHashPort;
import com.fcv.citas.application.port.out.TransactionPort;
import com.fcv.citas.application.port.out.UserRepositoryPort;
import com.fcv.citas.domain.model.User;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RegisterUserServiceTest {
    private final InMemoryUsers users = new InMemoryUsers();
    private final RegisterUserService service = new RegisterUserService(users, new PasswordHashPort() {
        @Override public String hash(String rawPassword) { return "synthetic-adaptive-hash"; }
        @Override public boolean matches(String rawPassword, String encodedPassword) { return false; }
    }, new DirectTransactions());

    @Test
    void registersOnlyUserWithNormalizedUniqueIdentifiersAndHashedPassword() {
        var registered = service.register(command(" Ana@Example.test ", " cc ", " 12345 "));

        assertThat(registered.roles()).containsExactly("USER");
        User persisted = users.findByEmail("ana@example.test").orElseThrow();
        assertThat(persisted.documentType()).isEqualTo("CC");
        assertThat(persisted.documentNumber()).isEqualTo("12345");
        assertThat(persisted.passwordHash()).isEqualTo("synthetic-adaptive-hash");
        assertThat(persisted.passwordHash()).doesNotContain("correct horse battery staple");
    }

    @Test
    void rejectsDuplicateEmailWithoutPersistingAnotherUser() {
        service.register(command("ana@example.test", "CC", "12345"));

        assertThatThrownBy(() -> service.register(command("ANA@example.test", "TI", "999")))
                .isInstanceOf(DuplicateIdentifierException.class);
        assertThat(users.size()).isOne();
    }

    @Test
    void rejectsDuplicateDocumentWithoutPersistingAnotherUser() {
        service.register(command("ana@example.test", "CC", "12345"));

        assertThatThrownBy(() -> service.register(command("other@example.test", "cc", "12345")))
                .isInstanceOf(DuplicateIdentifierException.class);
        assertThat(users.size()).isOne();
    }

    private static RegisterCommand command(String email, String documentType, String documentNumber) {
        return new RegisterCommand("Ana", "Gomez", documentType, documentNumber, email,
                "3001234567", "correct horse battery staple");
    }

    private static final class InMemoryUsers implements UserRepositoryPort {
        private final Map<String, User> byEmail = new LinkedHashMap<>();
        private long nextId = 1;

        @Override public boolean existsByEmail(String email) { return byEmail.containsKey(email); }
        @Override public boolean existsByDocument(String type, String number) {
            return byEmail.values().stream().anyMatch(user -> user.documentType().equals(type)
                    && user.documentNumber().equals(number));
        }
        @Override public User save(User user) {
            User saved = new User(nextId++, user.firstName(), user.lastName(), user.documentType(),
                    user.documentNumber(), user.email(), user.phone(), user.passwordHash(), user.active(), user.roles());
            byEmail.put(saved.email(), saved);
            return saved;
        }
        @Override public Optional<User> findByEmail(String email) { return Optional.ofNullable(byEmail.get(email)); }
        @Override public Optional<User> findById(Long id) {
            return byEmail.values().stream().filter(user -> user.id().equals(id)).findFirst();
        }
        int size() { return byEmail.size(); }
    }

    private static final class DirectTransactions implements TransactionPort {
        @Override public <T> T required(java.util.function.Supplier<T> work) { return work.get(); }
        @Override public void required(Runnable work) { work.run(); }
    }
}
