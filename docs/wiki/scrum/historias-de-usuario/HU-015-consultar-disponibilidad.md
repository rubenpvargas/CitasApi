---
id: HU-015
tipo: historia-de-usuario
titulo: Consultar disponibilidad
estado: Completada
epica: "[[EP-003-profesionales-y-disponibilidad]]"
esfuerzo: Alto
sprint_sugerido: Incremento 3
dependencias: ["[[HU-007-disponer-catalogos-fijos]]", "[[HU-009-gestionar-especialidades]]", "[[HU-011-configurar-capacidades-profesional]]", "[[HU-012-crear-bloques-disponibilidad]]"]
relacionadas: ["[[HU-016-reservar-cita-general]]", "[[HU-017-solicitar-cita-especializada]]"]
---
# HU-015 — Consultar disponibilidad
## Historia de usuario
**COMO** USER **QUIERO** filtrar horarios disponibles por sede, tipo, especialidad, profesional y fecha **PARA** seleccionar una franja que cubra mi cita.
## Alcance
- Filtros del PRD y resultados de disponibilidad completa.
## Fuera de alcance
- Confirmación/reserva.
## Reglas de negocio
- Solo especialidad activa asociada a profesional activo; 60 min exige dos slots consecutivos; no mostrar franjas reservadas/retenidas.
## Dependencias y relaciones
- Épica: [[EP-003-profesionales-y-disponibilidad]]; depende de [[HU-007-disponer-catalogos-fijos]], [[HU-009-gestionar-especialidades]], [[HU-011-configurar-capacidades-profesional]], [[HU-012-crear-bloques-disponibilidad]].
## Esfuerzo
**Nivel:** Alto. **Justificación:** filtros, duración, disponibilidad y límites de reserva.
## Tareas de desarrollo
- [x] **T-01 — Definir contrato de filtros y respuesta**. Dificultad: Medio.
- [x] **T-02 — Implementar cálculo de slots completos y filtros**. Dificultad: Alto.
- [x] **T-03 — Construir búsqueda accesible con estados UI**. Dificultad: Medio.
- [x] **T-04 — Probar 30/60 min, consecutividad, inactivos y vacíos**. Dificultad: Alto.
## Criterios de aceptación
### CA-01 — Filtros
**Dado** criterios válidos **cuando** USER busca **entonces** se muestran franjas de profesionales/sedes/especialidades que los satisfacen.
### CA-02 — Duración completa
**Dado** una especialidad de 60 minutos **cuando** se calcula disponibilidad **entonces** solo aparecen inicios con dos slots consecutivos libres.
### CA-03 — Exclusión
**Dado** franja reservada, retenida, profesional inactivo o especialidad no asociada/activa **cuando** se busca **entonces** no aparece disponible.
## Definition of Done
- [x] CA-01 a CA-03 validados con pruebas de cálculo y persistencia.
- [x] Contrato REST es consumido directamente por UI sin BFF y documenta errores/filtros.
- [x] Consultas/índices se justifican para agenda sin desnormalizar catálogos.
## Evidencia de validación
| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
| CA-01 | Conforme | `AvailabilityQueryIT` (4), `AvailabilityQueryServiceTest`: filtros `specialtyId`, `from`/`to` (≤31 días), sede y profesional | API `1becd7c` |
| CA-02 | Conforme | `AvailabilityCalculatorTest` (RED 5/6 → GREEN): 60 min exige dos slots consecutivos del mismo bloque | Ver PRUEBAS_DE_ESCRITORIO.md |
| CA-03 | Conforme | Excluye reservados, retenidos, profesional inactivo y especialidad inactiva/no asignada; solo inicios futuros | — |
| DoD contrato/UI | Conforme | Web `2a25c53`: la UI muestra solo franjas devueltas por la API (sin cálculo cliente); `booking.spec` (9) | REST directo sin BFF |
| DoD consultas | Conforme | Consulta por `ix_slot_start` y bloques del profesional; catálogos por FK sin desnormalizar | — |

## Historial de validación
- 2026-09-17 — Creada en `Pendiente de aprobación`.
- 2026-10-03 — Corregida (no exigía consecutivos para 60 min; UI inventaba franjas) y validada (API `1becd7c`; web `2a25c53`).

## Notas y decisiones
- La disponibilidad se revalida al confirmar en HU-016/HU-017.
