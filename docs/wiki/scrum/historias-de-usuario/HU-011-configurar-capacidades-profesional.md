---
id: HU-011
tipo: historia-de-usuario
titulo: Configurar capacidades del profesional
estado: Pendiente de aprobación
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
- [ ] **T-01 — Modelar tablas puente, claves e invariantes 3FN**. Dificultad: Alto.
- [ ] **T-02 — Definir contrato y operaciones ADMIN**. Dificultad: Medio.
- [ ] **T-03 — Crear UI de asignación y estado**. Dificultad: Medio.
- [ ] **T-04 — Probar primaria, sedes y desactivación**. Dificultad: Alto.
## Criterios de aceptación
### CA-01 — Capacidades válidas
**Dado** profesional existente **cuando** ADMIN asigna especialidades/sedes válidas **entonces** quedan asociadas y puede marcar una primaria asignada.
### CA-02 — Primaria consistente
**Dado** una especialidad no asociada **cuando** ADMIN intenta marcarla primaria **entonces** se rechaza.
### CA-03 — Estado operativo
**Dado** profesional desactivado **cuando** se consulta para agenda o reserva **entonces** no se ofrece como disponible.
## Definition of Done
- [ ] CA-01 a CA-03 validados con relaciones N:M y permisos ADMIN.
- [ ] Flyway y justificación 3FN cubren puentes, primaria y sedes fijas.
- [ ] HU-012/HU-015 consumen solo capacidades activas comprobadas.
## Evidencia de validación
| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
| CA-01..CA-03 / DoD | Pendiente | — | — |
## Historial de validación
- 2026-09-17 — Creada en `Pendiente de aprobación`.
## Notas y decisiones
- La regla para cambiar primaria se valida atómicamente.
