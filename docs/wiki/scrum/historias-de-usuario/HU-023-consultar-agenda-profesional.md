---
id: HU-023
tipo: historia-de-usuario
titulo: Consultar agenda profesional
estado: Completada
epica: "[[EP-006-operacion-y-trazabilidad]]"
esfuerzo: Medio
sprint_sugerido: Incremento 5
dependencias: ["[[HU-016-reservar-cita-general]]", "[[HU-018-decidir-solicitud-especializada]]"]
relacionadas: ["[[HU-024-cerrar-atencion]]"]
---
# HU-023 — Consultar agenda profesional
## Historia de usuario
**COMO** PROFESSIONAL **QUIERO** consultar mis citas aprobadas por día/semana y sede **PARA** preparar mi atención sin ver datos ajenos.
## Alcance
- Agenda propia de APPROVED filtrable por día/semana/sede.
## Fuera de alcance
- Cierre de atención o agenda de otros profesionales.
## Reglas de negocio
- Solo citas propias; no muestra datos de usuarios fuera de sus citas.
## Dependencias y relaciones
- Épica: [[EP-006-operacion-y-trazabilidad]]; depende de [[HU-016-reservar-cita-general]] y [[HU-018-decidir-solicitud-especializada]].
## Esfuerzo
**Nivel:** Medio. **Justificación:** ownership, filtros y minimización de datos.
## Tareas de desarrollo
- [x] **T-01 — Definir datos mínimos y contrato de agenda**. Dificultad: Medio.
- [x] **T-02 — Aplicar filtros/ownership e índices de consulta**. Dificultad: Medio.
- [x] **T-03 — Renderizar agenda accesible por periodo/sede**. Dificultad: Medio.
- [x] **T-04 — Probar aislamiento y filtros**. Dificultad: Medio.
## Criterios de aceptación
### CA-01 — Agenda propia
**Dado** PROFESSIONAL autenticado **cuando** filtra por día/semana y sede **entonces** ve sus citas APPROVED coincidentes.
### CA-02 — Privacidad
**Dado** agenda de otro profesional o cita no propia **cuando** intenta acceder **entonces** no obtiene sus datos.
## Definition of Done
- [x] CA-01 y CA-02 validados con pruebas de ownership.
- [x] Contrato limita campos a lo necesario y UI maneja vacío/error/carga.
- [x] La agenda no incluye estados distintos de APPROVED salvo cambio aprobado de alcance.
## Evidencia de validación
| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
| CA-01 | Conforme | `ProfessionalAgendaIT`, `ProfessionalAgendaServiceTest`: citas propias `APPROVED` por rango (≤31 días) y sede; E2E "la agenda semanal muestra citas aprobadas…" en verde | API `9ae3120`; web `0385e8e` |
| CA-02 | Conforme | Solo agenda del profesional del token; campos `{id, startAt, endAt, locationCode, specialtyName, patientName, closable}` | Sin documento, email ni teléfono |
| DoD | Conforme | Solo `APPROVED`; UI con carga/vacío/error (`agenda.spec`) | — |

## Historial de validación
- 2026-09-17 — Creada en Pendiente de aprobación.
- 2026-10-04 — Validada (API `9ae3120`; web `0385e8e`).

## Notas y decisiones
- Esta HU es distinta del calendario de bloques HU-014.
