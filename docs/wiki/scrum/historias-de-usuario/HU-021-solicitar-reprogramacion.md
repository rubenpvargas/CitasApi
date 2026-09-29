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
- [ ] **T-01 — Definir modelo/contrato de solicitud y estados**. Dificultad: Alto.
- [ ] **T-02 — Retener nueva franja sin liberar original**. Dificultad: Alto.
- [ ] **T-03 — Crear flujo UI que conserve profesional/especialidad**. Dificultad: Medio.
- [ ] **T-04 — Probar elegibilidad, conflicto y coexistencia de franjas**. Dificultad: Alto.
## Criterios de aceptación
### CA-01 — Solicitud válida
**Dado** cita propia APPROVED futura **cuando** USER elige nueva franja disponible **entonces** se crea reprogramación PENDING y retiene la nueva franja.
### CA-02 — Preservación
**Dado** reprogramación pendiente **cuando** se consulta la cita original **entonces** conserva su franja hasta decisión ADMIN.
### CA-03 — Restricción
**Dado** cita no aprobada/no futura o cambio de profesional **cuando** se solicita **entonces** se rechaza como reprogramación.
## Definition of Done
- [ ] CA-01 a CA-03 validados, incluida coexistencia transaccional de franjas.
- [ ] Flyway/3FN separan solicitud de reprogramación, cita y estados.
- [ ] UI no permite alterar profesional/especialidad y comunica estado pendiente.
## Evidencia de validación
| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
| CA-01..CA-03 / DoD | Pendiente | — | — |
## Historial de validación
- 2026-09-17 — Creada en Pendiente de aprobación.
## Notas y decisiones
- Retención no destruye ni sobrescribe la cita original.
