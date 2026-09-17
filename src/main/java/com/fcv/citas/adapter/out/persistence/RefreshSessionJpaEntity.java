package com.fcv.citas.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "refresh_sessions")
class RefreshSessionJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserJpaEntity user;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(nullable = false, unique = true, length = 36)
    private String jti;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "replaced_by_jti", length = 36)
    private String replacedByJti;

    protected RefreshSessionJpaEntity() {
    }

    RefreshSessionJpaEntity(UserJpaEntity user, String tokenHash, String jti,
                            Instant issuedAt, Instant expiresAt, Instant revokedAt,
                            String replacedByJti) {
        this.user = user;
        this.tokenHash = tokenHash;
        this.jti = jti;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
        this.revokedAt = revokedAt;
        this.replacedByJti = replacedByJti;
    }

    void updateRevocation(Instant revokedAt, String replacedByJti) {
        this.revokedAt = revokedAt;
        this.replacedByJti = replacedByJti;
    }

    Long getId() { return id; }
    UserJpaEntity getUser() { return user; }
    String getTokenHash() { return tokenHash; }
    String getJti() { return jti; }
    Instant getIssuedAt() { return issuedAt; }
    Instant getExpiresAt() { return expiresAt; }
    Instant getRevokedAt() { return revokedAt; }
    String getReplacedByJti() { return replacedByJti; }
}
