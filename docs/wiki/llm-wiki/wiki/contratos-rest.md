# Contrato REST

## DECISIÓN — Autenticación v1

El incremento backend aprobado HU-001 a HU-003 fija estos endpoints públicos:

- `POST /api/v1/auth/register`: registra un USER y devuelve `201` sin password.
- `POST /api/v1/auth/login`: autentica email/password y devuelve access y refresh JWT.
- `POST /api/v1/auth/refresh`: rota un refresh válido y devuelve un nuevo par.
- `POST /api/v1/auth/logout`: revoca el refresh activo y devuelve `204`.

Los errores usan Problem Details con un `code` estable. Credenciales inválidas
comparten `INVALID_CREDENTIALS`; refresh inválido, vencido, revocado o
reutilizado comparte `INVALID_REFRESH_TOKEN`. El frontend consumirá este
contrato directamente, pero su implementación no pertenece a este incremento.

Fuente: aprobación explícita del usuario de HU-001, HU-002 y HU-003 el
2026-09-17; evidencia prevista en pruebas REST de `citas-api`.

## PREGUNTA ABIERTA

Los contratos de funcionalidades posteriores, paginación y política global de
fechas/zona horaria siguen pendientes de aprobación.
