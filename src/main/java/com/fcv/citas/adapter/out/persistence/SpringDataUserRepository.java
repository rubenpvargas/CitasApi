package com.fcv.citas.adapter.out.persistence;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface SpringDataUserRepository extends JpaRepository<UserJpaEntity, Long> {
    boolean existsByEmail(String email);
    boolean existsByDocumentTypeAndDocumentNumber(String documentType, String documentNumber);

    @EntityGraph(attributePaths = "roles")
    Optional<UserJpaEntity> findByEmail(String email);

    @Override
    @EntityGraph(attributePaths = "roles")
    Optional<UserJpaEntity> findById(Long id);
}
