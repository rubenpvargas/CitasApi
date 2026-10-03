package com.fcv.citas.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordPolicyTest {
    @ParameterizedTest
    @ValueSource(strings = {"Sintetica2026", "Abcdefg1", "Demo1234*"})
    void acceptsPasswordsWithLengthUppercaseAndDigit(String candidate) {
        assertThat(PasswordPolicy.isSatisfiedBy(candidate)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "Short1", "sinmayuscula2026", "SINNUMEROSaqui", "12345678"})
    void rejectsWeakPasswords(String candidate) {
        assertThat(PasswordPolicy.isSatisfiedBy(candidate)).isFalse();
    }

    @Test
    void rejectsNullAndPasswordsLongerThanBcryptLimit() {
        assertThat(PasswordPolicy.isSatisfiedBy(null)).isFalse();
        assertThat(PasswordPolicy.isSatisfiedBy("A1" + "a".repeat(71))).isFalse();
        assertThat(PasswordPolicy.isSatisfiedBy("A1" + "a".repeat(70))).isTrue();
    }
}
