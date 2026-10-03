---
id: HU-011
tipo: historia-de-usuario
titulo: Configurar capacidades del profesional
estado: Completada
epica: "[[EP-003-profesionales-y-disponibilidad]]"
esfuerzo: Alto
sprint_sugerido: Incremento 2
dependencias: ["[[HU-010-crear-profesional]]", "[[HU-007-disponer-catalogos-fijos]]", "[[HU-009-gestionar-especialidades]]"]
relacionadas: ["[[HU-012-crear-bloques-disponibilidad]]"]
---
# HU-011 — Configurar capacidades del profesional
## Historia de usuario
**COMO** ADMIN **QUIERO** asignar especialidades, especialidad primaria, sedes y estado a un PROFESSIONAL **PARA** controlar dónde y qué puede atender.
## Alcance
- N:M profesional-especialidad, primaria, N:M profesional-sede y activar/desactivar.
## Fuera de alcance
- Crear bloques o cambiar duración por profesional.
## Reglas de negocio
- Una o varias especialidades, una primaria entre las asignadas, una o ambas sedes; agenda solo en sede asignada.
## Dependencias y relaciones
- Épica: [[EP-003-profesionales-y-disponibilidad]]; depende de [[HU-010-crear-profesional]], [[HU-007-disponer-catalogos-fijos]], [[HU-009-gestionar-especialidades]].
## Esfuerzo
**Nivel:** Alto. **Justificación:** dos relaciones N:M, regla de primaria y consecuencias operativas.
## Tareas de desarrollo
- [x] **T-01 — Modelar tablas puente, claves e invariantes 3FN**. Dificultad: Alto.
- [x] **T-02 — Definir contrato y operaciones ADMIN**. Dificultad: Medio.
- [x] **T-03 — Crear UI de asignación y estado**. Dificultad: Medio.
- [x] **T-04 — Probar primaria, sedes y desactivación**. Dificultad: Alto.
## Criterios de aceptación
### CA-01 — Capacidades válidas
**Dado** profesional existente **cuando** ADMIN asigna especialidades/sedes válidas **entonces** quedan asociadas y puede marcar una primaria asignada.
### CA-02 — Primaria consistente
**Dado** una especialidad no asociada **cuando** ADMIN intenta marcarla primaria **entonces** se rechaza.
### CA-03 — Estado operativo
**Dado** profesional desactivado **cuando** se consulta para agenda o reserva **entonces** no se ofrece como disponible.
## Definition of Done
- [x] CA-01 a CA-03 validados con relaciones N:M y permisos ADMIN.
- [x] Flyway y justificación 3FN cubren puentes, primaria y sedes fijas.
- [x] HU-012/HU-015 consumen solo capacidades activas comprobadas.
## Evidencia de validación
| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
| CA-01 | Conforme | `ProfessionalCapabilitiesIT` (4): especialidades y sedes N:M con primaria asignada | API `e57fbf5` |
| CA-02 | Conforme | Primaria no incluida → 409 `PRIMARY_NOT_ASSIGNED`; listas vacías → 400; inexistentes → 404 (`ProfessionalCapabilitiesTest` RED 2 fallos + 2 errores → GREEN) | — |
| CA-03 | Conforme | Profesional inactivo no publica bloques, no aparece en disponibilidad y su reserva → 409 `PROFESSIONAL_INACTIVE` | Guardia también en la reserva vigente |
| DoD 3FN | Conforme | Puentes `professional_specialties(is_primary)` y `professional_locations` con PK compuesta (V4) | — |
| DoD UI | Conforme | Web `3c6e111`, `dc78221`: selector de primaria limitado a las asignadas; preselección por `specialtyIds`/`locationIds` | — |

## Historial de validación
- 2026-09-17 — Creada en `Pendiente de aprobación`.
- 2026-10-03 — Corregida (primaria no asignada aceptada, inactivo reservable) y validada (API `e57fbf5`; web `3c6e111`).

## Notas y decisiones
- La regla para cambiar primaria se valida atómicamente.
