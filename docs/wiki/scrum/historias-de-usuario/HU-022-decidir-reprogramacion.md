---
id: HU-022
tipo: historia-de-usuario
titulo: Decidir reprogramación
estado: Completada
epica: "[[EP-005-reprogramacion]]"
esfuerzo: Alto
sprint_sugerido: Incremento 5
dependencias: ["[[HU-021-solicitar-reprogramacion]]"]
relacionadas: ["[[HU-020-cancelar-cita]]", "[[HU-025-gestionar-bandeja-administrativa]]"]
---
# HU-022 — Decidir reprogramación
## Historia de usuario
**COMO** ADMIN **QUIERO** aprobar o rechazar una reprogramación pendiente **PARA** resolver ambas franjas sin perder la cita original indebidamente.
## Alcance
- Aprobar traslado/liberación antigua; rechazar liberación provisional y conservación original; motivo cuando corresponda.
## Fuera de alcance
- Cambiar profesional/especialidad.
## Reglas de negocio
- Si aprueba, cita adopta nueva franja y libera antigua; si rechaza, libera nueva y mantiene original; auditoría de transiciones.
## Dependencias y relaciones
- Épica: [[EP-005-reprogramacion]]; depende de [[HU-021-solicitar-reprogramacion]].
## Esfuerzo
**Nivel:** Alto. **Justificación:** operación atómica sobre dos reservas y decisiones administrativas.
## Tareas de desarrollo
- [x] **T-01 — Definir contrato, estados y motivo de decisión**. Dificultad: Medio.
- [x] **T-02 — Implementar traslado/liberación atómicos y auditoría**. Dificultad: Alto.
- [x] **T-03 — Crear interfaz ADMIN de decisión**. Dificultad: Medio.
- [x] **T-04 — Probar aprobación, rechazo, reintento y disponibilidad resultante**. Dificultad: Alto.
## Criterios de aceptación
### CA-01 — Aprobación
**Dado** reprogramación PENDING **cuando** ADMIN aprueba **entonces** cita adopta nueva franja y se liberan slots antiguos.
### CA-02 — Rechazo
**Dado** reprogramación PENDING **cuando** ADMIN rechaza con motivo aplicable **entonces** nueva reserva se libera y la cita original se mantiene.
### CA-03 — Integridad
**Dado** decisión ya tomada o inválida **cuando** se intenta decidir de nuevo **entonces** no se alteran las franjas ni el historial previo.
## Definition of Done
- [x] CA-01 a CA-03 con pruebas transaccionales y de disponibilidad posterior.
- [x] Auditoría conserva actor, fuente, fecha, estado y motivo aplicable.
- [x] Contrato/UI diferencian claramente decisión aprobada, rechazada y pendiente.
## Evidencia de validación
| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
| CA-01 | Conforme | `RescheduleDecisionIT`, `RescheduleDecisionServiceTest` rl1: aprobar mueve la cita a la nueva franja, libera la antigua y asigna la retenida; E2E "reprogramación solicitada y aprobada"; evento `RESCHEDULE_APPROVED` entregado | API `1f0b78b`; web `39d1012` |
| CA-02 | Conforme | rl2: rechazar con motivo libera la retención y mantiene la original | — |
| CA-03 | Conforme | rl3: decisión repetida o rechazo sin motivo → 409 sin alterar franjas; bloqueo de solicitud y cita | — |
| DoD | Conforme | Historial ADMIN en solicitud y cita (actor, fuente, fecha, estado, motivo); UI distingue pendiente/aprobada/rechazada y compara franjas | — |

## Historial de validación
- 2026-09-17 — Creada en Pendiente de aprobación.
- 2026-10-04 — Validada con IT, prueba de escritorio, E2E desde base limpia y evento WF-002 (API `1f0b78b`; web `39d1012`).

## Notas y decisiones
- USER conserva la cita o puede cancelarla después de rechazo.
