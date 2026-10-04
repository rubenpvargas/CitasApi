---
id: HU-020
tipo: historia-de-usuario
titulo: Cancelar cita
estado: Completada
epica: "[[EP-004-ciclo-de-vida-de-citas]]"
esfuerzo: Medio
sprint_sugerido: Incremento 4
dependencias: ["[[HU-019-consultar-mis-citas]]"]
relacionadas: ["[[HU-021-solicitar-reprogramacion]]"]
---
# HU-020 — Cancelar cita
## Historia de usuario
**COMO** USER **QUIERO** cancelar una cita futura no terminal **PARA** liberar su horario sin reactivarla directamente.
## Alcance
- Transición a `CANCELLED`, liberación y auditoría.
## Fuera de alcance
- Reactivación directa o cancelación de cita terminal/pasada.
## Reglas de negocio
- Solo propia, futura y no terminal; liberar slots; historial inmutable.
## Dependencias y relaciones
- Épica: [[EP-004-ciclo-de-vida-de-citas]]; depende de [[HU-019-consultar-mis-citas]].
## Esfuerzo
**Nivel:** Medio. **Justificación:** transición y recursos de agenda.
## Tareas de desarrollo
- [x] **T-01 — Definir estados cancelables y contrato**. Dificultad: Medio.
- [x] **T-02 — Aplicar transición, liberación y auditoría atómica**. Dificultad: Alto.
- [x] **T-03 — Integrar confirmación de cancelación UI**. Dificultad: Bajo.
- [x] **T-04 — Probar propia/ajena, pasado, terminal y reintento**. Dificultad: Medio.
## Criterios de aceptación
### CA-01 — Cancelación válida
**Dado** cita propia futura no terminal **cuando** USER la cancela **entonces** queda `CANCELLED`, libera slots y registra historial.
### CA-02 — Restricción
**Dado** cita ajena, pasada o terminal **cuando** USER intenta cancelarla **entonces** se rechaza sin transición.
### CA-03 — Sin reactivación
**Dado** cita `CANCELLED` **cuando** se intenta reactivar directamente **entonces** el sistema la deniega.
## Definition of Done
- [x] CA-01 a CA-03 validados con transacción y auditoría.
- [x] Disponibilidad posterior refleja liberación sin doble reserva.
- [x] UI muestra resultado y restringe acciones según respuesta autorizada.
## Evidencia de validación
| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
| CA-01 | Conforme | `CancelAppointmentIT`, `AppointmentCancelTest`: propia futura no terminal → `CANCELLED`, slots liberados, historial; cierra reprogramación pendiente (fuente `SYSTEM`) y libera su retención | API `1588ed5` (V9); web `6e727b0` |
| CA-02 | Conforme | Ajena → 404; pasada o terminal → 409 `INVALID_TRANSITION` (tabla de escritorio estado × tiempo) | — |
| CA-03 | Conforme | No existe ruta de reactivación | — |
| DoD | Conforme | Disponibilidad posterior refleja la liberación; E2E reservar → cancelar en verde; simulación con n8n caído: cancelación 200 y evento reintentado hasta `SENT` | Diálogo accesible en lugar de `confirm()` |

## Historial de validación
- 2026-09-17 — Creada en `Pendiente de aprobación`.
- 2026-10-04 — Validada con IT, prueba de escritorio, E2E y simulación de fallo de n8n (API `1588ed5`; web `6e727b0`).

## Notas y decisiones
- Cancelar tras rechazo de reprogramación se aborda en HU-022.
