---
id: HU-015
tipo: historia-de-usuario
titulo: Consultar disponibilidad
estado: Pendiente de aprobación
epica: "[[EP-003-profesionales-y-disponibilidad]]"
esfuerzo: Alto
sprint_sugerido: Incremento 3
dependencias: ["[[HU-007-disponer-catalogos-fijos]]", "[[HU-009-gestionar-especialidades]]", "[[HU-011-configurar-capacidades-profesional]]", "[[HU-012-crear-bloques-disponibilidad]]"]
relacionadas: ["[[HU-016-reservar-cita-general]]", "[[HU-017-solicitar-cita-especializada]]"]
---
# HU-015 — Consultar disponibilidad
## Historia de usuario
**COMO** USER **QUIERO** filtrar horarios disponibles por sede, tipo, especialidad, profesional y fecha **PARA** seleccionar una franja que cubra mi cita.
## Alcance
- Filtros del PRD y resultados de disponibilidad completa.
## Fuera de alcance
- Confirmación/reserva.
## Reglas de negocio
- Solo especialidad activa asociada a profesional activo; 60 min exige dos slots consecutivos; no mostrar franjas reservadas/retenidas.
## Dependencias y relaciones
- Épica: [[EP-003-profesionales-y-disponibilidad]]; depende de [[HU-007-disponer-catalogos-fijos]], [[HU-009-gestionar-especialidades]], [[HU-011-configurar-capacidades-profesional]], [[HU-012-crear-bloques-disponibilidad]].
## Esfuerzo
**Nivel:** Alto. **Justificación:** filtros, duración, disponibilidad y límites de reserva.
## Tareas de desarrollo
- [ ] **T-01 — Definir contrato de filtros y respuesta**. Dificultad: Medio.
- [ ] **T-02 — Implementar cálculo de slots completos y filtros**. Dificultad: Alto.
- [ ] **T-03 — Construir búsqueda accesible con estados UI**. Dificultad: Medio.
- [ ] **T-04 — Probar 30/60 min, consecutividad, inactivos y vacíos**. Dificultad: Alto.
## Criterios de aceptación
### CA-01 — Filtros
**Dado** criterios válidos **cuando** USER busca **entonces** se muestran franjas de profesionales/sedes/especialidades que los satisfacen.
### CA-02 — Duración completa
**Dado** una especialidad de 60 minutos **cuando** se calcula disponibilidad **entonces** solo aparecen inicios con dos slots consecutivos libres.
### CA-03 — Exclusión
**Dado** franja reservada, retenida, profesional inactivo o especialidad no asociada/activa **cuando** se busca **entonces** no aparece disponible.
## Definition of Done
- [ ] CA-01 a CA-03 validados con pruebas de cálculo y persistencia.
- [ ] Contrato REST es consumido directamente por UI sin BFF y documenta errores/filtros.
- [ ] Consultas/índices se justifican para agenda sin desnormalizar catálogos.
## Evidencia de validación
| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
| CA-01..CA-03 / DoD | Pendiente | — | — |
## Historial de validación
- 2026-09-17 — Creada en `Pendiente de aprobación`.
## Notas y decisiones
- La disponibilidad se revalida al confirmar en HU-016/HU-017.
