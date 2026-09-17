package com.fcv.citas.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
class UserJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "document_type", nullable = false, length = 32)
    private String documentType;

    @Column(name = "document_number", nullable = false, length = 64)
    private String documentNumber;

    @Column(nullable = false, unique = true, length = 254)
    private String email;

    @Column(nullable = false, length = 32)
    private String phone;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<RoleJpaEntity> roles = new HashSet<>();

    protected UserJpaEntity() {
    }

    UserJpaEntity(String firstName, String lastName, String documentType, String documentNumber,
                  String email, String phone, String passwordHash, boolean active,
                  Instant createdAt, Instant updatedAt, Set<RoleJpaEntity> roles) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.documentType = documentType;
        this.documentNumber = documentNumber;
        this.email = email;
        this.phone = phone;
        this.passwordHash = passwordHash;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.roles = new HashSet<>(roles);
    }

    Long getId() { return id; }
    String getFirstName() { return firstName; }
    String getLastName() { return lastName; }
    String getDocumentType() { return documentType; }
    String getDocumentNumber() { return documentNumber; }
    String getEmail() { return email; }
    String getPhone() { return phone; }
    String getPasswordHash() { return passwordHash; }
    boolean isActive() { return active; }
    Set<RoleJpaEntity> getRoles() { return roles; }
}
