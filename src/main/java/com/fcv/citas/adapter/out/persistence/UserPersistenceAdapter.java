package com.fcv.citas.adapter.out.persistence;

import com.fcv.citas.application.exception.DuplicateIdentifierException;
import com.fcv.citas.application.port.out.UserRepositoryPort;
import com.fcv.citas.domain.model.User;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class UserPersistenceAdapter implements UserRepositoryPort {
    private final SpringDataUserRepository users;
    private final SpringDataRoleRepository roles;
    private final Clock clock;

    public UserPersistenceAdapter(SpringDataUserRepository users, SpringDataRoleRepository roles, Clock clock) {
        this.users = users;
        this.roles = roles;
        this.clock = clock;
    }

    @Override
    public boolean existsByEmail(String email) {
        return users.existsByEmail(email);
    }

    @Override
    public boolean existsByDocument(String documentType, String documentNumber) {
        return users.existsByDocumentTypeAndDocumentNumber(documentType, documentNumber);
    }

    @Override
    public User save(User user) {
        try {
            Set<RoleJpaEntity> assignedRoles = user.roles().stream()
                    .map(code -> roles.findByCode(code)
                            .orElseThrow(() -> new IllegalStateException("Required role is not seeded")))
                    .collect(Collectors.toSet());
            Instant now = clock.instant();
            UserJpaEntity saved = users.saveAndFlush(new UserJpaEntity(user.firstName(), user.lastName(),
                    user.documentType(), user.documentNumber(), user.email(), user.phone(),
                    user.passwordHash(), user.active(), now, now, assignedRoles));
            return toDomain(saved);
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateIdentifierException();
        }
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return users.findByEmail(email).map(this::toDomain);
    }

    @Override
    public Optional<User> findById(Long id) {
        return users.findById(id).map(this::toDomain);
    }

    private User toDomain(UserJpaEntity entity) {
        return new User(entity.getId(), entity.getFirstName(), entity.getLastName(),
                entity.getDocumentType(), entity.getDocumentNumber(), entity.getEmail(), entity.getPhone(),
                entity.getPasswordHash(), entity.isActive(), entity.getRoles().stream()
                .map(RoleJpaEntity::getCode).collect(Collectors.toSet()));
    }
}
