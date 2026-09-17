---
id: HU-017
tipo: historia-de-usuario
titulo: Solicitar cita especializada
estado: Pendiente de aprobación
epica: "[[EP-004-ciclo-de-vida-de-citas]]"
esfuerzo: Alto
sprint_sugerido: Incremento 4
dependencias: ["[[HU-015-consultar-disponibilidad]]"]
relacionadas: ["[[HU-018-decidir-solicitud-especializada]]"]
---
# HU-017 — Solicitar cita especializada
## Historia de usuario
**COMO** USER **QUIERO** solicitar una cita especializada con sede, profesional y horario **PARA** que ADMIN la evalúe manteniendo la franja retenida.
## Alcance
- Crear solicitud `REQUESTED` y retener slots requeridos.
## Fuera de alcance
- Decisión ADMIN.
## Reglas de negocio
- Especialidad activa asociada; no doble reserva; 60 min consecutivos; estado inicial `REQUESTED`.
## Dependencias y relaciones
- Épica: [[EP-004-ciclo-de-vida-de-citas]]; depende de [[HU-015-consultar-disponibilidad]].
## Esfuerzo
**Nivel:** Alto. **Justificación:** retención concurrente y flujo posterior de aprobación.
## Tareas de desarrollo
- [ ] **T-01 — Definir contrato y conflicto de solicitud**. Dificultad: Alto.
- [ ] **T-02 — Crear retención atómica, estado y auditoría**. Dificultad: Alto.
- [ ] **T-03 — Implementar solicitud UI y resultado pendiente**. Dificultad: Medio.
- [ ] **T-04 — Probar concurrencia y duración 30/60**. Dificultad: Alto.
## Criterios de aceptación
### CA-01 — Solicitud retenida
**Dado** selección especializada válida y disponible **cuando** USER confirma **entonces** se crea en `REQUESTED` y su franja queda retenida.
### CA-02 — No doble reserva
**Dado** retención existente o conflicto al confirmar **cuando** intenta solicitar **entonces** no se asignan los mismos slots a dos citas.
### CA-03 — Duración
**Dado** especialidad de 60 minutos **cuando** solicita **entonces** retiene dos slots consecutivos disponibles.
## Definition of Done
- [ ] CA-01 a CA-03 con pruebas de transacción/concurrencia.
- [ ] Auditoría registra creación y modelo mantiene reservas/retenciones en 3FN.
- [ ] UI muestra solicitud pendiente y error de conflicto sin asegurar aprobación.
## Evidencia de validación
| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
| CA-01..CA-03 / DoD | Pendiente | — | — |
## Historial de validación
- 2026-09-17 — Creada en `Pendiente de aprobación`.
## Notas y decisiones
- La política de expiración no fue especificada; no se inventa.
