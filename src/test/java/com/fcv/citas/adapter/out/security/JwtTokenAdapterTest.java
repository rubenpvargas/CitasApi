package com.fcv.citas.adapter.out.security;

import com.fcv.citas.config.SecurityProperties;
import com.fcv.citas.domain.model.User;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenAdapterTest {
    private final JwtTokenAdapter adapter = new JwtTokenAdapter(new SecurityProperties("citas-api-test",
            "access-secret-with-at-least-thirty-two-characters",
            "refresh-secret-with-at-least-thirty-two-characters", 15, 7));

    @Test
    void issuesSeparatedTokensWithRoleClaimsAndParsesOnlyTheRefreshToken() {
        Instant issuedAt = Instant.now();
        User user = new User(7L, "Ana", "Gomez", "CC", "12345", "ana@example.test", "3001234567",
                "bcrypt-value", true, Set.of("USER"));

        var pair = adapter.issuePair(user, issuedAt);
        var access = adapter.accessDecoder().decode(pair.accessToken());
        var refresh = adapter.parseRefresh(pair.refreshToken());

        assertThat(pair.accessToken()).isNotEqualTo(pair.refreshToken());
        assertThat(access.getSubject()).isEqualTo("7");
        assertThat(access.getClaimAsStringList("roles")).containsExactly("USER");
        assertThat(refresh.userId()).isEqualTo(7L);
        assertThat(refresh.jti()).isEqualTo(pair.refreshJti());
        assertThatThrownBy(() -> adapter.parseRefresh(pair.accessToken()))
                .isInstanceOf(RuntimeException.class);
    }
}
