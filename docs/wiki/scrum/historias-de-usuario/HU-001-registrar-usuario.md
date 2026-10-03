---
id: HU-001
tipo: historia-de-usuario
titulo: Registrar usuario
estado: Completada
epica: "[[EP-001-identidad-y-acceso]]"
esfuerzo: Medio
sprint_sugerido: Incremento 1
dependencias: ["[[HU-007-disponer-catalogos-fijos]]"]
relacionadas: ["[[HU-002-iniciar-sesion]]"]
---
# HU-001 — Registrar usuario
## Historia de usuario
**COMO** visitante **QUIERO** crear una cuenta USER con mis datos mínimos **PARA** usar el agendamiento.
## Alcance
- Nombres, apellidos, tipo/número de documento, email, teléfono y contraseña; cuenta USER sintética.
## Fuera de alcance
- Alta de PROFESSIONAL o ADMIN.
## Reglas de negocio
- Email y documento son únicos; contraseña con hash adaptativo, nunca texto plano.
## Dependencias y relaciones
- Épica: [[EP-001-identidad-y-acceso]]; depende de [[HU-007-disponer-catalogos-fijos]].
## Esfuerzo
**Nivel:** Medio. **Justificación:** valida identidad y seguridad en persistencia y UI.
## Tareas de desarrollo
- [x] **T-01 — Definir contrato de registro y errores**. Dificultad: Medio. Coordinar DTO/validaciones REST y consumidor frontend.
- [x] **T-02 — Modelar usuario en 3FN y unicidad**. Dificultad: Medio. Migración Flyway, índices y hash en adaptador de seguridad.
- [x] **T-03 — Crear formulario accesible**. Dificultad: Medio. Estados loading/error/success y mensajes seguros.
- [x] **T-04 — Probar validaciones y duplicados**. Dificultad: Medio.
## Criterios de aceptación
### CA-01 — Registro válido
**Dado** datos mínimos válidos **cuando** el visitante los envía **entonces** se crea un USER y recibe confirmación sin revelar datos sensibles.
### CA-02 — Unicidad
**Dado** email o documento existente **cuando** intenta registrarse **entonces** se rechaza de forma segura y no se crea duplicado.
### CA-03 — Credencial protegida
**Dado** una cuenta creada **cuando** se persiste **entonces** la contraseña no queda en texto plano ni se devuelve.
## Definition of Done
- [x] CA-01 a CA-03 validados con pruebas relevantes.
- [x] Migración Flyway e índices de unicidad justifican el modelo 3FN.
- [x] Contrato REST y formulario frontend coinciden; validación server-side presente.
- [x] Trazabilidad Scrum actualizada sin secretos.
## Evidencia de validación
| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
| CA-01 | Conforme | `RegistrationIT` (201 USER sin campo password); smoke HTTP 2026-10-02: 201 | API `d424977` |
| CA-02 | Conforme | `RegistrationIT`, `RegisterUserServiceTest`: email (sin distinguir mayúsculas) y documento duplicados → 409 `IDENTIFIER_ALREADY_REGISTERED` sin fila extra; smoke: 409 | El código no revela qué dato existe |
| CA-03 | Conforme | `RegistrationIT` verifica hash BCrypt `$2…`; `BcryptPasswordAdapterTest`; `PasswordPolicyTest` (9) | RED: 3 contraseñas débiles devolvían 201 → GREEN 400 |
| DoD contrato/UI | Conforme | Web `44c8701`: payload = `RegisterRequest`, errores 400/409 accesibles, `register.spec.ts` (9) | Política 8–72, mayúscula y dígito en API y UI |
| Pruebas | Conforme | `mvn -Pit verify` desde esquema vacío: 42 unitarias + 38 IT, 0 fallos (verificación independiente en worktree `7db6f21`); web: lint OK, 91/91 specs, build OK | Rama `develop` |

## Historial de validación
- 2026-09-17 — Creada en `Pendiente de aprobación`.
- 2026-09-17 — Aprobada explícitamente por el usuario para el incremento backend HU-001 a HU-003.
- 2026-09-17 — Desarrollo backend iniciado en la rama `develop`; UI permanece fuera de alcance.
- 2026-10-02 — Revalidada con evidencia automatizada (API `0896045`, `d424977`; web `6099f8e`, `44c8701`); CA y DoD marcados.

## Notas y decisiones
- No declarar aprobada sin revisión explícita del usuario.
