---
id: HU-003
tipo: historia-de-usuario
titulo: Renovar y cerrar sesión
estado: Completada
epica: "[[EP-001-identidad-y-acceso]]"
esfuerzo: Medio
sprint_sugerido: Incremento 1
dependencias: ["[[HU-002-iniciar-sesion]]"]
relacionadas: []
---
# HU-003 — Renovar y cerrar sesión
## Historia de usuario
**COMO** usuario autenticado **QUIERO** renovar una sesión válida y cerrarla **PARA** mantener acceso controlado.
## Alcance
- Refresh con token válido y logout/revocación.
## Fuera de alcance
- Recuperación de contraseña.
## Reglas de negocio
- Refresh token separado; logout invalida/revoca la sesión correspondiente.
## Dependencias y relaciones
- Épica: [[EP-001-identidad-y-acceso]]; depende de [[HU-002-iniciar-sesion]].
## Esfuerzo
**Nivel:** Medio. **Justificación:** ciclo de token y sincronía entre seguridad y cliente.
## Tareas de desarrollo
- [x] **T-01 — Definir contratos de refresh/logout**. Dificultad: Medio.
- [x] **T-02 — Persistir y revocar refresh de forma segura**. Dificultad: Medio.
- [x] **T-03 — Manejar renovación/cierre en cliente**. Dificultad: Medio.
- [x] **T-04 — Probar token válido, revocado y vencido**. Dificultad: Medio.
## Criterios de aceptación
### CA-01 — Renovación válida
**Dado** refresh vigente **cuando** solicita renovación **entonces** recibe un nuevo access sin reingresar credenciales.
### CA-02 — Token no válido
**Dado** refresh vencido o revocado **cuando** solicita renovación **entonces** se deniega y el cliente vuelve a estado no autenticado.
### CA-03 — Logout
**Dado** sesión activa **cuando** cierra sesión **entonces** su refresh no puede renovar acceso posterior.
## Definition of Done
- [x] CA-01 a CA-03 validados con evidencia de revocación.
- [x] No se exponen tokens en logs/respuestas ajenas al titular.
- [x] Cliente y API manejan expiración sin dejar UI con privilegios obsoletos.
## Evidencia de validación
| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
| CA-01 | Conforme | `RefreshAndLogoutIT`: refresh rota el par y el nuevo access es válido; smoke: 200 | API `51b50e6` |
| CA-02 | Conforme | Refresh reutilizado, vencido, revocado, access o basura → 401 `INVALID_REFRESH_TOKEN`; interceptor web limpia sesión y redirige a login (`auth.interceptor.spec`, 9) | Single-flight de refresh |
| CA-03 | Conforme | Logout 204 y refresh posterior 401 (IT y smoke) | — |
| DoD tokens | Conforme | Solo hash de refresh persistido; tokens nunca registrados (revisión de log en smoke: 0 apariciones) | — |
| DoD expiración UI | Conforme | Web `74ad56a`: sesión expira con temporizador, logout limpia estado aunque falle la API; `session.store.spec` (7), `profile.spec` (4) | Sin privilegios obsoletos |

## Historial de validación
- 2026-09-17 — Creada en `Pendiente de aprobación`.
- 2026-09-17 — Aprobada explícitamente por el usuario para el incremento backend HU-001 a HU-003, incluido logout/revocación.
- 2026-09-17 — Desarrollo backend iniciado en la rama `develop`; UI permanece fuera de alcance.
- 2026-10-02 — Revalidada con IT de rotación/revocación y manejo de expiración en UI (API `51b50e6`; web `74ad56a`).

## Notas y decisiones
- La política de rotación queda abierta para el diseño de seguridad.
