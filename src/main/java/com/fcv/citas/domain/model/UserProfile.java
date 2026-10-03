package com.fcv.citas.domain.model;

import java.util.Set;

/** Datos de perfil visibles para su titular; nunca incluye credenciales. */
public record UserProfile(Long id, String firstName, String lastName, String email, String documentType,
                          String documentNumber, String phone, Set<String> roles) {
    public UserProfile {
        roles = roles == null ? Set.of() : Set.copyOf(roles);
    }
}
