---
id: HU-018
tipo: historia-de-usuario
titulo: Decidir solicitud especializada
estado: Completada
epica: "[[EP-004-ciclo-de-vida-de-citas]]"
esfuerzo: Alto
sprint_sugerido: Incremento 4
dependencias: ["[[HU-017-solicitar-cita-especializada]]"]
relacionadas: ["[[HU-025-gestionar-bandeja-administrativa]]"]
---
# HU-018 — Decidir solicitud especializada
## Historia de usuario
**COMO** ADMIN **QUIERO** aprobar o rechazar una solicitud especializada **PARA** resolver la cita retenida de forma trazable.
## Alcance
- Aprobar a `APPROVED`; rechazar a `REJECTED` con motivo y liberar slots.
## Fuera de alcance
- Reprogramaciones.
## Reglas de negocio
- ADMIN decide; rechazo exige motivo; aprobación conserva reserva; rechazo libera slots y deja auditoría.
## Dependencias y relaciones
- Épica: [[EP-004-ciclo-de-vida-de-citas]]; depende de [[HU-017-solicitar-cita-especializada]].
## Esfuerzo
**Nivel:** Alto. **Justificación:** transición autorizada, liberación de recursos y auditoría.
## Tareas de desarrollo
- [ ] **T-01 — Definir contrato de decisión y motivo**. Dificultad: Medio.
- [ ] **T-02 — Implementar transiciones atómicas, liberación y auditoría**. Dificultad: Alto.
- [ ] **T-03 — Crear acción ADMIN con confirmación y errores**. Dificultad: Medio.
- [ ] **T-04 — Probar aprobación, rechazo sin motivo y doble decisión**. Dificultad: Alto.
## Criterios de aceptación
### CA-01 — Aprobación
**Dado** solicitud `REQUESTED` **cuando** ADMIN aprueba **entonces** pasa a `APPROVED` y conserva su franja.
### CA-02 — Rechazo
**Dado** solicitud `REQUESTED` **cuando** ADMIN rechaza con motivo **entonces** pasa a `REJECTED`, guarda motivo y libera slots.
### CA-03 — Transición inválida
**Dado** solicitud ya decidida o rechazo sin motivo **cuando** se decide **entonces** se rechaza sin cambio adicional.
## Definition of Done
- [ ] CA-01 a CA-03 validados con autorización ADMIN y auditoría.
- [ ] Pruebas verifican que la liberación ocurre solo al rechazo.
- [ ] Contrato/UI hacen visible el motivo solo al USER autorizado.
## Evidencia de validación
| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
| CA-01..CA-03 / DoD | Pendiente | — | — |
## Historial de validación
- 2026-09-17 — Creada en `Pendiente de aprobación`.
## Notas y decisiones
- Las reglas de transición se centralizan en dominio/aplicación, no en UI.
