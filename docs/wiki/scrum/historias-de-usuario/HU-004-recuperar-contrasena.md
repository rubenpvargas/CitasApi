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
- [ ] **T-01 — Definir exposición controlada del token en desarrollo**. Dificultad: Medio.
- [ ] **T-02 — Modelar token de único uso y expiración**. Dificultad: Medio.
- [ ] **T-03 — Construir solicitud y cambio accesibles**. Dificultad: Medio.
- [ ] **T-04 — Probar uso, reuso y expiración**. Dificultad: Medio.
## Criterios de aceptación
### CA-01 — Solicitud
**Dado** email registrado **cuando** solicita recuperación **entonces** se genera un token temporal mediante el mecanismo seguro definido.
### CA-02 — Cambio válido
**Dado** token vigente de único uso **cuando** establece una contraseña válida **entonces** la contraseña se actualiza con hash adaptativo y el token se consume.
### CA-03 — Token inválido
**Dado** token vencido, usado o desconocido **cuando** intenta cambiar contraseña **entonces** se rechaza sin alterar credenciales.
## Definition of Done
- [ ] CA-01 a CA-03 validados; token no queda en logs inseguros.
- [ ] Migración Flyway cubre token temporal y expiración si aplica.
- [ ] Flujo frontend no revela existencia de cuenta indebidamente y presenta errores accesibles.
## Evidencia de validación
| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
| CA-01 | Pendiente | — | — |
| CA-02 | Pendiente | — | — |
| CA-03 / DoD | Pendiente | — | — |
## Historial de validación
- 2026-09-17 — Creada en `Pendiente de aprobación`.
## Notas y decisiones
- SMTP es opcional conforme al PRD.
