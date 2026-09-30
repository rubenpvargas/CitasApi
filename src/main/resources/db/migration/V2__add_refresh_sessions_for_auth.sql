-- The existing training schema was baselined at V1 before the current
-- authentication slice introduced refresh-session rotation.  Keep the legacy
-- refresh_tokens table untouched and add the table used by this application.
CREATE TABLE IF NOT EXISTS refresh_sessions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    token_hash CHAR(64) NOT NULL,
    jti CHAR(36) NOT NULL,
    issued_at TIMESTAMP(6) NOT NULL,
    expires_at TIMESTAMP(6) NOT NULL,
    revoked_at TIMESTAMP(6) NULL,
    replaced_by_jti CHAR(36) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_refresh_sessions_hash UNIQUE (token_hash),
    CONSTRAINT uk_refresh_sessions_jti UNIQUE (jti),
    CONSTRAINT fk_refresh_sessions_user FOREIGN KEY (user_id) REFERENCES users (id),
    INDEX idx_refresh_sessions_user_active (user_id, revoked_at, expires_at)
);
