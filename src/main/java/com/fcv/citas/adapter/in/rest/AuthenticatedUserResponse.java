package com.fcv.citas.adapter.in.rest;

import com.fcv.citas.domain.model.User;

import java.util.Set;

public record AuthenticatedUserResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        Set<String> roles
) {
    static AuthenticatedUserResponse from(User user) {
        return new AuthenticatedUserResponse(user.id(), user.firstName(), user.lastName(), user.email(), user.roles());
    }
}
