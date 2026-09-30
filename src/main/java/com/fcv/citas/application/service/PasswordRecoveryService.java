package com.fcv.citas.application.service;

import com.fcv.citas.application.exception.BusinessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;

@Service
public class PasswordRecoveryService {
    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwords;
    private final SecureRandom random = new SecureRandom();

    public PasswordRecoveryService(JdbcTemplate jdbc, PasswordEncoder passwords) {
        this.jdbc = jdbc;
        this.passwords = passwords;
    }

    @Transactional
    public String request(String email) {
        var users = jdbc.queryForList("SELECT id FROM users WHERE email=? AND active=TRUE", email.trim().toLowerCase());
        if (users.isEmpty()) return null;
        String token = randomToken();
        String hash = hash(token);
        jdbc.update("INSERT INTO password_reset_tokens(user_id,token_hash,expires_at,created_at) VALUES(?,?,DATE_ADD(NOW(6),INTERVAL 30 MINUTE),NOW(6))",
                users.get(0).get("id"), hash);
        // Development intentionally returns the one-time token to enable a local
        // SMTP-free demo. It is never logged and production can suppress it.
        return token;
    }

    @Transactional
    public void reset(String token, String newPassword) {
        Map<String,Object> row;
        try {
            row = jdbc.queryForMap("SELECT id,user_id FROM password_reset_tokens WHERE token_hash=? AND used_at IS NULL AND expires_at>NOW(6)", hash(token));
        } catch (Exception e) {
            throw new BusinessException("INVALID_RESET_TOKEN", "The recovery token is invalid or expired");
        }
        jdbc.update("UPDATE users SET password_hash=?,updated_at=NOW(6) WHERE id=?", passwords.encode(newPassword), row.get("user_id"));
        jdbc.update("UPDATE password_reset_tokens SET used_at=NOW(6) WHERE id=?", row.get("id"));
        jdbc.update("UPDATE refresh_sessions SET revoked_at=NOW(6) WHERE user_id=? AND revoked_at IS NULL", row.get("user_id"));
    }

    private String randomToken() {
        byte[] bytes = new byte[24];
        random.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private String hash(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception e) { throw new IllegalStateException(e); }
    }
}
