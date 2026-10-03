---
id: HU-010
tipo: historia-de-usuario
titulo: Crear profesional
estado: Completada
epica: "[[EP-003-profesionales-y-disponibilidad]]"
esfuerzo: Medio
sprint_sugerido: Incremento 2
dependencias: ["[[HU-002-iniciar-sesion]]"]
relacionadas: ["[[HU-011-configurar-capacidades-profesional]]"]
---
# HU-010 — Crear profesional
## Historia de usuario
**COMO** ADMIN **QUIERO** crear un usuario PROFESSIONAL con código y matrícula ficticia **PARA** habilitar su configuración asistencial.
## Alcance
- Alta de usuario PROFESSIONAL, código profesional y matrícula sintética.
## Fuera de alcance
- Especialidades, sedes y agenda.
## Reglas de negocio
- Datos exclusivamente sintéticos; ADMIN es quien crea PROFESSIONAL.
## Dependencias y relaciones
- Épica: [[EP-003-profesionales-y-disponibilidad]]; depende de [[HU-002-iniciar-sesion]]; habilita [[HU-011-configurar-capacidades-profesional]].
## Esfuerzo
**Nivel:** Medio. **Justificación:** identidad especializada y permisos por rol.
## Tareas de desarrollo
- [x] **T-01 — Diseñar extensión profesional normalizada y contrato**. Dificultad: Medio.
- [x] **T-02 — Implementar alta ADMIN y seguridad**. Dificultad: Medio.
- [x] **T-03 — Crear formulario/listado administrativo accesible**. Dificultad: Medio.
- [x] **T-04 — Probar rol, datos sintéticos y unicidad aplicable**. Dificultad: Medio.
## Criterios de aceptación
### CA-01 — Alta autorizada
**Dado** ADMIN autenticado **cuando** registra datos profesionales válidos **entonces** se crea un usuario con rol PROFESSIONAL y sus identificadores ficticios.
### CA-02 — Protección
**Dado** USER o PROFESSIONAL **cuando** intenta crear profesional **entonces** la operación es denegada.
## Definition of Done
- [x] CA-01 y CA-02 validados con autorización.
- [x] Modelo separa datos de usuario y datos profesionales conforme a 3FN.
- [x] UI/API no usan ni exponen datos personales reales.
## Evidencia de validación
| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
| CA-01 | Conforme | `ProfessionalAdminIT` (4), `ProfessionalAdminServiceTest`: `POST /admin/professionals` → 201 con usuario PROFESSIONAL, email en minúsculas, política de contraseña de dominio; duplicados → 409 `DUPLICATE_EMAIL|DOCUMENT|PROFESSIONAL_CODE|LICENSE` | API `5f020c3` |
| CA-02 | Conforme | USER/PROFESSIONAL → 403 (IT) | — |
| DoD 3FN | Conforme | `users` (identidad) separado de `professionals` (código y matrícula) con FK única 1:1 | Sin migración nueva |
| DoD datos | Conforme | Respuesta sin password/hash; UI con placeholders sintéticos | — |
| DoD UI / E2E | Conforme | Web `61653b7`; E2E Playwright UI+API real: login ADMIN y gestión de catálogos en verde | Verificación independiente `028ecf8`: 109 unitarias + 79 IT |

## Historial de validación
- 2026-09-17 — Creada en `Pendiente de aprobación`.
- 2026-10-03 — Validada con IT, verificación independiente y UI (API `5f020c3`; web `61653b7`).

## Notas y decisiones
- Los campos exactos no requeridos por PRD se mantienen abiertos.
