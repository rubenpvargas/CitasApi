---
id: HU-012
tipo: historia-de-usuario
titulo: Crear bloques de disponibilidad
estado: Completada
epica: "[[EP-003-profesionales-y-disponibilidad]]"
esfuerzo: Alto
sprint_sugerido: Incremento 3
dependencias: ["[[HU-011-configurar-capacidades-profesional]]"]
relacionadas: ["[[HU-013-modificar-bloques-futuros]]", "[[HU-015-consultar-disponibilidad]]"]
---
# HU-012 — Crear bloques de disponibilidad
## Historia de usuario
**COMO** PROFESSIONAL **QUIERO** crear bloques de agenda por fecha y sede **PARA** publicar mi disponibilidad.
## Alcance
- Múltiples bloques diarios, sede por bloque y discretización en slots de 30 min.
## Fuera de alcance
- Editar/eliminar bloques o reservar citas.
## Reglas de negocio
- No pasado ni solape; sede asignada; bloques discretizados a slots de 30 min.
## Dependencias y relaciones
- Épica: [[EP-003-profesionales-y-disponibilidad]]; depende de [[HU-011-configurar-capacidades-profesional]].
## Esfuerzo
**Nivel:** Alto. **Justificación:** validación temporal, ownership, sede y representación de slots.
## Tareas de desarrollo
- [ ] **T-01 — Diseñar bloque/slot e índices de agenda**. Dificultad: Alto.
- [ ] **T-02 — Implementar validaciones de fecha, solape, sede y ownership**. Dificultad: Alto.
- [ ] **T-03 — Crear calendario/formulario accesible**. Dificultad: Medio.
- [ ] **T-04 — Probar pasado, solape, sede no asignada y varios bloques**. Dificultad: Alto.
## Criterios de aceptación
### CA-01 — Bloque válido
**Dado** PROFESSIONAL activo y sede asignada **cuando** crea un bloque futuro no solapado **entonces** queda publicado y discretizado en slots de 30 min.
### CA-02 — Restricción temporal/sede
**Dado** bloque en pasado, solapado o en sede no asignada **cuando** intenta guardarlo **entonces** se rechaza sin afectar agenda previa.
### CA-03 — Ownership
**Dado** otro profesional **cuando** intenta crear bloque ajeno **entonces** el backend lo deniega.
## Definition of Done
- [ ] CA-01 a CA-03 validados incluyendo bordes temporales.
- [ ] Migración Flyway, índices y modelo 3FN soportan consultas de agenda.
- [ ] UI muestra estados loading/error/success y no permite asumir disponibilidad final.
## Evidencia de validación
| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
| CA-01..CA-03 / DoD | Pendiente | — | — |
## Historial de validación
- 2026-09-17 — Creada en `Pendiente de aprobación`.
## Notas y decisiones
- Zona horaria/formato se acordará en contrato.
