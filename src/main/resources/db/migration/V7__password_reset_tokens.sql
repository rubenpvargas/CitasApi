-- V2 is historical and immutable. Recovery tokens are added independently so
-- existing databases do not require a checksum repair.
CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    token_hash CHAR(64) NOT NULL,
    expires_at TIMESTAMP(6) NOT NULL,
    used_at TIMESTAMP(6) NULL,
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_password_reset_tokens_hash UNIQUE (token_hash),
    CONSTRAINT fk_password_reset_tokens_user FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_password_reset_tokens_user_active (user_id, used_at, expires_at)
);
