package com.fcv.citas.adapter.out.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class BcryptPasswordAdapterTest {
    @Test
    void hashesPasswordsWithBcryptAndVerifiesOnlyTheOriginalPassword() {
        BcryptPasswordAdapter adapter = new BcryptPasswordAdapter(new BCryptPasswordEncoder(10));

        String hash = adapter.hash("correct horse battery staple");

        assertThat(hash).startsWith("$2");
        assertThat(hash).doesNotContain("correct horse battery staple");
        assertThat(adapter.matches("correct horse battery staple", hash)).isTrue();
        assertThat(adapter.matches("incorrect", hash)).isFalse();
    }
}
