package com.fcv.citas.adapter.out.persistence;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

import java.util.Optional;

interface SpringDataUserRepository extends JpaRepository<UserJpaEntity, Long> {
    boolean existsByEmail(String email);
    boolean existsByDocumentTypeAndDocumentNumber(String documentType, String documentNumber);

    @EntityGraph(attributePaths = "roles")
    Optional<UserJpaEntity> findByEmail(String email);

    @Override
    @EntityGraph(attributePaths = "roles")
    Optional<UserJpaEntity> findById(Long id);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update UserJpaEntity u set u.passwordHash = :hash, u.updatedAt = :at where u.id = :id")
    int updatePasswordHash(@Param("id") Long id, @Param("hash") String hash, @Param("at") Instant at);
}
