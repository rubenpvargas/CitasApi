package com.fcv.citas.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface SpringDataRoleRepository extends JpaRepository<RoleJpaEntity, Short> {
    Optional<RoleJpaEntity> findByCode(String code);
}
