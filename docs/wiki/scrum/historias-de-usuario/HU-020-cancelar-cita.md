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
- [ ] **T-01 — Definir estados cancelables y contrato**. Dificultad: Medio.
- [ ] **T-02 — Aplicar transición, liberación y auditoría atómica**. Dificultad: Alto.
- [ ] **T-03 — Integrar confirmación de cancelación UI**. Dificultad: Bajo.
- [ ] **T-04 — Probar propia/ajena, pasado, terminal y reintento**. Dificultad: Medio.
## Criterios de aceptación
### CA-01 — Cancelación válida
**Dado** cita propia futura no terminal **cuando** USER la cancela **entonces** queda `CANCELLED`, libera slots y registra historial.
### CA-02 — Restricción
**Dado** cita ajena, pasada o terminal **cuando** USER intenta cancelarla **entonces** se rechaza sin transición.
### CA-03 — Sin reactivación
**Dado** cita `CANCELLED` **cuando** se intenta reactivar directamente **entonces** el sistema la deniega.
## Definition of Done
- [ ] CA-01 a CA-03 validados con transacción y auditoría.
- [ ] Disponibilidad posterior refleja liberación sin doble reserva.
- [ ] UI muestra resultado y restringe acciones según respuesta autorizada.
## Evidencia de validación
| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
| CA-01..CA-03 / DoD | Pendiente | — | — |
## Historial de validación
- 2026-09-17 — Creada en `Pendiente de aprobación`.
## Notas y decisiones
- Cancelar tras rechazo de reprogramación se aborda en HU-022.
