-- HU-004: una solicitud nueva invalida los tokens de recuperación previos sin usar.
-- 3FN: invalidated_at depende funcionalmente solo de la clave del token (id) y se distingue
-- semánticamente de used_at (consumo efectivo con cambio de contraseña), por lo que no se
-- sobrecarga used_at. La invalidación filtra por user_id, cubierto por el prefijo del índice
-- existente idx_password_reset_tokens_user_active; el consumo busca por token_hash (UNIQUE).
-- No se requiere índice adicional.
ALTER TABLE password_reset_tokens
    ADD COLUMN invalidated_at TIMESTAMP(6) NULL AFTER used_at;
