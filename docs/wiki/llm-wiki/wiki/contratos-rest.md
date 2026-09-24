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

## PLAN CROSS-REPO — identidad visible tras login

**Cambio aditivo de contrato.** `POST /api/v1/auth/login` incorporará `user`
en su respuesta: `id`, `firstName`, `lastName`, `email` y `roles`. Se mantienen
los campos existentes de tokens y expiraciones. `refresh` conserva su respuesta
de tokens, pues no altera el perfil de la sesión ya conocida por el cliente.

- **citas-api:** modelo de sesión autenticada, respuesta REST de login y pruebas
  de aplicación/controlador.
- **citas-web:** tipos del cliente, cliente REST y estado visible de paciente.
- **Evidencia:** las pruebas Maven y el build Angular pasan; un login válido
  muestra el nombre devuelto por la API y uno inválido conserva el error seguro.

**Validado el 2026-09-24.** La respuesta de login incluye `user`; la interfaz
actualiza el estado visible con `firstName` y `lastName` de esa respuesta. La
verificación con un usuario sintético mostró su nombre en el dashboard.

## PREGUNTA ABIERTA

Los contratos de funcionalidades posteriores, paginación y política global de
fechas/zona horaria siguen pendientes de aprobación.
