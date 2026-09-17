package com.fcv.citas.application.port.out;

import com.fcv.citas.domain.model.User;

import java.util.Optional;

public interface UserRepositoryPort {
    boolean existsByEmail(String email);
    boolean existsByDocument(String documentType, String documentNumber);
    User save(User user);
    Optional<User> findByEmail(String email);
    Optional<User> findById(Long id);
}
