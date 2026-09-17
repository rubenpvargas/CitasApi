---
id: HU-016
tipo: historia-de-usuario
titulo: Reservar cita general
estado: Pendiente de aprobación
epica: "[[EP-004-ciclo-de-vida-de-citas]]"
esfuerzo: Alto
sprint_sugerido: Incremento 4
dependencias: ["[[HU-015-consultar-disponibilidad]]"]
relacionadas: ["[[HU-019-consultar-mis-citas]]", "[[HU-020-cancelar-cita]]"]
---
# HU-016 — Reservar cita general
## Historia de usuario
**COMO** USER **QUIERO** confirmar una cita de Medicina General con profesional y franja disponibles **PARA** obtener atención aprobada inmediatamente.
## Alcance
- Selección Medicina General, profesional, franja y creación `APPROVED`.
## Fuera de alcance
- Aprobación ADMIN o especialidades especializadas.
## Reglas de negocio
- Revalidar al confirmar; no doble reserva; estado automático `APPROVED`; auditoría fuente SYSTEM/USER según contrato.
## Dependencias y relaciones
- Épica: [[EP-004-ciclo-de-vida-de-citas]]; depende de [[HU-015-consultar-disponibilidad]].
## Esfuerzo
**Nivel:** Alto. **Justificación:** reserva atómica, concurrencia y transición auditada.
## Tareas de desarrollo
- [ ] **T-01 — Definir contrato de confirmación/idempotencia y errores de conflicto**. Dificultad: Alto.
- [ ] **T-02 — Implementar reserva atómica, estado y auditoría**. Dificultad: Alto.
- [ ] **T-03 — Integrar confirmación UI y conflicto de franja**. Dificultad: Medio.
- [ ] **T-04 — Probar doble intento concurrente y estado APPROVED**. Dificultad: Alto.
## Criterios de aceptación
### CA-01 — Reserva general
**Dado** Medicina General y una franja aún disponible **cuando** USER confirma **entonces** se crea cita `APPROVED` sin intervención ADMIN.
### CA-02 — Conflicto
**Dado** franja ocupada entre búsqueda y confirmación **cuando** USER confirma **entonces** no se crea cita y se informa conflicto seguro.
### CA-03 — Auditoría
**Dado** cita creada **cuando** se consulta su trazabilidad interna **entonces** existe transición con actor/fuente, fecha y estado.
## Definition of Done
- [ ] CA-01 a CA-03 validados, incluida prueba relevante de concurrencia.
- [ ] Persistencia/Flyway representa cita, slots y auditoría sin duplicar catálogos.
- [ ] Contrato backend/frontend y manejo UI de conflicto están verificados.
## Evidencia de validación
| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
| CA-01..CA-03 / DoD | Pendiente | — | — |
## Historial de validación
- 2026-09-17 — Creada en `Pendiente de aprobación`.
## Notas y decisiones
- La estrategia exacta de bloqueo se define sin alterar RN-01.
