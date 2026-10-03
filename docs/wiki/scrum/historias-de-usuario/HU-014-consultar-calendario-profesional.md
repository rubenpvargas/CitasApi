---
id: HU-014
tipo: historia-de-usuario
titulo: Consultar calendario profesional
estado: Completada
epica: "[[EP-003-profesionales-y-disponibilidad]]"
esfuerzo: Bajo
sprint_sugerido: Incremento 3
dependencias: ["[[HU-012-crear-bloques-disponibilidad]]"]
relacionadas: ["[[HU-013-modificar-bloques-futuros]]"]
---
# HU-014 — Consultar calendario profesional
## Historia de usuario
**COMO** PROFESSIONAL **QUIERO** consultar mi calendario de bloques **PARA** revisar mi disponibilidad publicada.
## Alcance
- Vista de bloques propios por fecha/sede acordada.
## Fuera de alcance
- Agenda de citas aprobadas.
## Reglas de negocio
- Solo muestra bloques del profesional autenticado.
## Dependencias y relaciones
- Épica: [[EP-003-profesionales-y-disponibilidad]]; depende de [[HU-012-crear-bloques-disponibilidad]].
## Esfuerzo
**Nivel:** Bajo. **Justificación:** lectura propia sobre bloque existente.
## Tareas de desarrollo
- [x] **T-01 — Definir consulta y filtros de calendario**. Dificultad: Bajo.
- [x] **T-02 — Aplicar ownership y paginación/intervalo si se requiere**. Dificultad: Medio.
- [x] **T-03 — Renderizar calendario accesible con vacíos/errores**. Dificultad: Medio.
## Criterios de aceptación
### CA-01 — Consulta propia
**Dado** PROFESSIONAL autenticado **cuando** consulta su calendario **entonces** visualiza sus bloques por fecha y sede.
### CA-02 — Aislamiento
**Dado** un identificador de otro profesional **cuando** intenta consultarlo **entonces** no accede a sus bloques.
## Definition of Done
- [x] CA-01 y CA-02 validados con autorización.
- [x] UI cubre loading, vacío, error y éxito con navegación por teclado.
- [x] Contrato no expone información no necesaria de otros profesionales.
## Evidencia de validación
| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
| CA-01 | Conforme | `ProfessionalCalendarIT` (2), `CalendarServiceTest`, `DateRangeTest`: bloques propios por rango (≤31 días) y sede con `totalSlots`, `committedSlots`, `editable` | API `227ffcc` |
| CA-02 | Conforme | Solo bloques del profesional del token; sin datos de pacientes ni de otros profesionales | — |
| DoD UI | Conforme | Web `41f5a7e`: calendario con carga/vacío/error/éxito navegable por teclado | — |

## Historial de validación
- 2026-09-17 — Creada en `Pendiente de aprobación`.
- 2026-10-03 — Validada (API `227ffcc`; web `41f5a7e`).

## Notas y decisiones
- La vista no sustituye [[HU-023-consultar-agenda-profesional]].
