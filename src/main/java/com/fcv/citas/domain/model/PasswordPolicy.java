package com.fcv.citas.domain.model;

/**
 * Política de contraseña compartida por registro y restablecimiento: 8 a 72 caracteres
 * (límite de BCrypt), al menos una mayúscula y al menos un dígito.
 */
public final class PasswordPolicy {
    public static final int MIN_LENGTH = 8;
    public static final int MAX_LENGTH = 72;
    public static final String DESCRIPTION =
            "must have 8 to 72 characters, at least one uppercase letter and one digit";

    private PasswordPolicy() {
    }

    public static boolean isSatisfiedBy(String rawPassword) {
        if (rawPassword == null || rawPassword.length() < MIN_LENGTH || rawPassword.length() > MAX_LENGTH) {
            return false;
        }
        return rawPassword.chars().anyMatch(Character::isUpperCase)
                && rawPassword.chars().anyMatch(Character::isDigit);
    }
}
