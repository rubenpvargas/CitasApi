package com.fcv.citas.application.model;

public record RegisterCommand(
        String firstName,
        String lastName,
        String documentType,
        String documentNumber,
        String email,
        String phone,
        String password
) {
}
