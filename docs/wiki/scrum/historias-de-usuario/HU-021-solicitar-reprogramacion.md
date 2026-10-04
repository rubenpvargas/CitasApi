---
id: HU-021
tipo: historia-de-usuario
titulo: Solicitar reprogramación
estado: Completada
epica: "[[EP-005-reprogramacion]]"
esfuerzo: Alto
sprint_sugerido: Incremento 5
dependencias: ["[[HU-019-consultar-mis-citas]]", "[[HU-015-consultar-disponibilidad]]"]
relacionadas: ["[[HU-022-decidir-reprogramacion]]"]
---
# HU-021 — Solicitar reprogramación
## Historia de usuario
**COMO** USER **QUIERO** solicitar una nueva fecha/hora para mi cita aprobada futura **PARA** cambiarla sin perder la franja original mientras ADMIN decide.
## Alcance
- Solicitud PENDING, misma especialidad/profesional y retención provisional de nueva franja.
## Fuera de alcance
- Cambiar profesional; se trata como nueva cita.
## Reglas de negocio
- Solo cita APPROVED futura; original se conserva; nuevo horario disponible y con duración completa; auditoría.
## Dependencias y relaciones
- Épica: [[EP-005-reprogramacion]]; depende de [[HU-019-consultar-mis-citas]] y [[HU-015-consultar-disponibilidad]].
## Esfuerzo
**Nivel:** Alto. **Justificación:** dos franjas coexistentes, reglas de preservación y retención.
## Tareas de desarrollo
- [x] **T-01 — Definir modelo/contrato de solicitud y estados**. Dificultad: Alto.
- [x] **T-02 — Retener nueva franja sin liberar original**. Dificultad: Alto.
- [x] **T-03 — Crear flujo UI que conserve profesional/especialidad**. Dificultad: Medio.
- [x] **T-04 — Probar elegibilidad, conflicto y coexistencia de franjas**. Dificultad: Alto.
## Criterios de aceptación
### CA-01 — Solicitud válida
**Dado** cita propia APPROVED futura **cuando** USER elige nueva franja disponible **entonces** se crea reprogramación PENDING y retiene la nueva franja.
### CA-02 — Preservación
**Dado** reprogramación pendiente **cuando** se consulta la cita original **entonces** conserva su franja hasta decisión ADMIN.
### CA-03 — Restricción
**Dado** cita no aprobada/no futura o cambio de profesional **cuando** se solicita **entonces** se rechaza como reprogramación.
## Definition of Done
- [x] CA-01 a CA-03 validados, incluida coexistencia transaccional de franjas.
- [x] Flyway/3FN separan solicitud de reprogramación, cita y estados.
- [x] UI no permite alterar profesional/especialidad y comunica estado pendiente.
## Evidencia de validación
| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
| CA-01 | Conforme | `RescheduleRequestIT`: `APPROVED` futura propia → `PENDING` con nueva franja retenida; carrera por la misma franja → un 201 y un 409 | API `581ffa8`; web `63f99b5` |
| CA-02 | Conforme | La cita original conserva su franja hasta la decisión (IT y tabla de ciclo de vida) | — |
| CA-03 | Conforme | No aprobada, no futura o con reprogramación pendiente → 409; profesional/especialidad no se aceptan en el cuerpo | — |
| DoD | Conforme | V9 `reschedule_request_history` append-only y FK de retención; UI conserva profesional/especialidad y comunica pendiente; E2E de reprogramación en verde | — |

## Historial de validación
- 2026-09-17 — Creada en Pendiente de aprobación.
- 2026-10-04 — Validada; repetida desde base limpia en la simulación del stack (API `581ffa8`; web `63f99b5`).

## Notas y decisiones
- Retención no destruye ni sobrescribe la cita original.
