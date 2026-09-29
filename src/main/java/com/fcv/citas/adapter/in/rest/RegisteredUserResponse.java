package com.fcv.citas.adapter.in.rest;

import com.fcv.citas.application.model.RegisteredUser;

import java.util.Set;

public record RegisteredUserResponse(
        Long id,
        String firstName,
        String lastName,
        String documentType,
        String documentNumber,
        String email,
        String phone,
        Set<String> roles
) {
    static RegisteredUserResponse from(RegisteredUser user) {
        return new RegisteredUserResponse(user.id(), user.firstName(), user.lastName(),
                user.documentType(), user.documentNumber(), user.email(), user.phone(), user.roles());
    }
}
