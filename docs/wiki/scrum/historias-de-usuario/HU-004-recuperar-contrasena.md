---
id: HU-004
tipo: historia-de-usuario
titulo: Recuperar contraseña
estado: Completada
epica: "[[EP-001-identidad-y-acceso]]"
esfuerzo: Medio
sprint_sugerido: Incremento 2
dependencias: ["[[HU-001-registrar-usuario]]"]
relacionadas: ["[[HU-002-iniciar-sesion]]"]
---
# HU-004 — Recuperar contraseña
## Historia de usuario
**COMO** usuario registrado **QUIERO** restablecer mi contraseña **PARA** recuperar acceso sin intervención administrativa.
## Alcance
- Solicitud por email, token temporal de único uso y cambio de contraseña.
## Fuera de alcance
- SMTP obligatorio.
## Reglas de negocio
- Cambio consume/invalida token; en desarrollo la entrega es segura y controlada, nunca expuesta públicamente.
## Dependencias y relaciones
- Épica: [[EP-001-identidad-y-acceso]]; depende de [[HU-001-registrar-usuario]].
## Esfuerzo
**Nivel:** Medio. **Justificación:** token temporal, expiración y flujo UI seguro.
## Tareas de desarrollo
- [x] **T-01 — Definir exposición controlada del token en desarrollo**. Dificultad: Medio.
- [x] **T-02 — Modelar token de único uso y expiración**. Dificultad: Medio.
- [x] **T-03 — Construir solicitud y cambio accesibles**. Dificultad: Medio.
- [x] **T-04 — Probar uso, reuso y expiración**. Dificultad: Medio.
## Criterios de aceptación
### CA-01 — Solicitud
**Dado** email registrado **cuando** solicita recuperación **entonces** se genera un token temporal mediante el mecanismo seguro definido.
### CA-02 — Cambio válido
**Dado** token vigente de único uso **cuando** establece una contraseña válida **entonces** la contraseña se actualiza con hash adaptativo y el token se consume.
### CA-03 — Token inválido
**Dado** token vencido, usado o desconocido **cuando** intenta cambiar contraseña **entonces** se rechaza sin alterar credenciales.
## Definition of Done
- [x] CA-01 a CA-03 validados; token no queda en logs inseguros.
- [x] Migración Flyway cubre token temporal y expiración si aplica.
- [x] Flujo frontend no revela existencia de cuenta indebidamente y presenta errores accesibles.
## Evidencia de validación
| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
| CA-01 | Conforme | `PasswordResetIT`: 202 con cuerpo idéntico exista o no la cuenta; token SHA-256, TTL 30 min; nuevo token invalida anteriores | API `52b5684`; V8 `invalidated_at` |
| CA-02 | Conforme | `PasswordResetDevelopmentFlowIT`: confirmación 204, contraseña BCrypt, token consumido, sesiones refresh revocadas; smoke: login nuevo 200, antiguo 401 | Consumo atómico con bloqueo de fila; carrera de dos confirmaciones consume una vez |
| CA-03 | Conforme | Token desconocido, vencido, usado o invalidado → 409 `INVALID_RESET_TOKEN` sin cambiar credencial | RED previo: 200 en lugar de 202 |
| DoD logs | Conforme | Token no se registra; `developmentToken` solo con `PASSWORD_RESET_EXPOSE_DEV_TOKEN=true` (por defecto `false`) | Smoke: 0 apariciones del token en log |
| DoD UI | Conforme | Web `0b475f4`: mensaje neutro, errores `role=alert`, token desde `?token=`; `forgot-password.spec` (7), `reset-password.spec` (7) | Arquitectura hexagonal con puertos |

## Historial de validación
- 2026-09-17 — Creada en `Pendiente de aprobación`.
- 2026-10-02 — Corregida (enumeración y token expuesto) y validada (API `52b5684`; web `0b475f4`).

## Notas y decisiones
- SMTP es opcional conforme al PRD.
