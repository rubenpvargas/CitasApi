package com.fcv.citas.application.model;

import java.util.Set;

public record RegisteredUser(
        Long id,
        String firstName,
        String lastName,
        String documentType,
        String documentNumber,
        String email,
        String phone,
        Set<String> roles
) {
}
