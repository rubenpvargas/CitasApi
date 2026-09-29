package com.fcv.citas.adapter.in.rest;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @NotBlank @Size(max = 32) String documentType,
        @NotBlank @Size(max = 64) String documentNumber,
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(max = 32) String phone,
        @NotBlank @Size(min = 8, max = 72) String password
) {
}
