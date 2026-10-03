---
id: HU-002
tipo: historia-de-usuario
titulo: Iniciar sesión
estado: Completada
epica: "[[EP-001-identidad-y-acceso]]"
esfuerzo: Alto
sprint_sugerido: Incremento 1
dependencias: ["[[HU-001-registrar-usuario]]"]
relacionadas: ["[[HU-003-renovar-y-cerrar-sesion]]"]
---
# HU-002 — Iniciar sesión
## Historia de usuario
**COMO** usuario registrado **QUIERO** iniciar sesión con email y contraseña **PARA** acceder según mi rol.
## Alcance
- Autenticación por email/contraseña y emisión de access/refresh JWT separados.
## Fuera de alcance
- Renovación, revocación y recuperación.
## Reglas de negocio
- Roles forman contexto de autorización; no exponer password ni tokens en logs.
## Dependencias y relaciones
- Épica: [[EP-001-identidad-y-acceso]]; depende de [[HU-001-registrar-usuario]].
## Esfuerzo
**Nivel:** Alto. **Justificación:** seguridad, contratos, persistencia de refresh y UI por rol.
## Tareas de desarrollo
- [x] **T-01 — Diseñar contrato de login y respuesta segura**. Dificultad: Alto.
- [x] **T-02 — Implementar autenticación y emisión JWT**. Dificultad: Alto. Spring Security, hash y claims de rol.
- [x] **T-03 — Integrar pantalla y contexto de sesión**. Dificultad: Medio. Estados de error y navegación autorizada.
- [x] **T-04 — Probar credenciales válidas, inválidas y roles**. Dificultad: Alto.
## Criterios de aceptación
### CA-01 — Acceso válido
**Dado** credenciales válidas **cuando** inicia sesión **entonces** recibe sesión con access y refresh separados y accede a su contexto de rol.
### CA-02 — Rechazo seguro
**Dado** credenciales inválidas **cuando** intenta iniciar sesión **entonces** recibe error seguro sin revelar cuál dato falló.
### CA-03 — Aislamiento por rol
**Dado** una sesión autenticada **cuando** solicita una capacidad no autorizada **entonces** el backend la deniega.
## Definition of Done
- [x] CA-01 a CA-03 validados; tokens/contraseñas no se registran.
- [x] Secretos se obtienen solo desde configuración de entorno.
- [x] Contrato, cliente REST y protección de rutas UI son coherentes; backend conserva la autoridad.
- [x] Pruebas de seguridad y trazabilidad Scrum disponibles.
## Evidencia de validación
| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
| CA-01 | Conforme | `LoginAndRoleIsolationIT`, `AuthenticationServiceTest`, `JwtTokenAdapterTest`: access y refresh separados, `roles` solo en access; smoke: 200 | API `2fdb2a8` |
| CA-02 | Conforme | Email desconocido y clave errónea devuelven cuerpo idéntico `INVALID_CREDENTIALS` (401) | Sin revelar el dato fallido |
| CA-03 | Conforme | USER → 403 en `/admin/eps`, `/admin/inbox`, `/professional/calendar`; sin token, token alterado, expirado o refresh usado como access → 401; smoke: 403 | Autoridad en backend |
| DoD secretos | Conforme | `SecurityPropertiesTest`: secretos JWT solo por entorno, sin valores por defecto, longitud y distinción exigidas | — |
| DoD UI | Conforme | Web `6099f8e`, `bd91419`: rutas con `authGuard`/`roleGuard`, login sin credenciales precargadas, error `role=alert`; `auth.guards.spec` (10), `login.spec` (11) | Guards son solo UX |

## Historial de validación
- 2026-09-17 — Creada en `Pendiente de aprobación`.
- 2026-09-17 — Aprobada explícitamente por el usuario para el incremento backend HU-001 a HU-003.
- 2026-09-17 — Desarrollo backend iniciado en la rama `develop`; UI permanece fuera de alcance.
- 2026-10-02 — Revalidada: pruebas de seguridad 401/403 y UI con guards por rol (API `2fdb2a8`; web `6099f8e`, `bd91419`).

## Notas y decisiones
- La ubicación de tokens en el cliente se definirá con el contrato de seguridad.
