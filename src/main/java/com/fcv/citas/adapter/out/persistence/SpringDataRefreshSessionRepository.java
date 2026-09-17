package com.fcv.citas.adapter.out.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

interface SpringDataRefreshSessionRepository extends JpaRepository<RefreshSessionJpaEntity, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select session from RefreshSessionJpaEntity session join fetch session.user where session.tokenHash = :tokenHash")
    Optional<RefreshSessionJpaEntity> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);
}
