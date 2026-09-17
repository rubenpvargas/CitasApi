---
id: HU-013
tipo: historia-de-usuario
titulo: Modificar bloques futuros
estado: Pendiente de aprobación
epica: "[[EP-003-profesionales-y-disponibilidad]]"
esfuerzo: Alto
sprint_sugerido: Incremento 3
dependencias: ["[[HU-012-crear-bloques-disponibilidad]]"]
relacionadas: ["[[HU-014-consultar-calendario-profesional]]"]
---
# HU-013 — Modificar bloques futuros
## Historia de usuario
**COMO** PROFESSIONAL **QUIERO** editar o eliminar mis bloques futuros sin citas comprometidas **PARA** mantener mi agenda correcta.
## Alcance
- Edición/eliminación de bloques propios futuros sin reserva/retención comprometida.
## Fuera de alcance
- Cambiar bloques pasados o comprometidos.
## Reglas de negocio
- No se permite editar/eliminar si existen citas comprometidas; toda edición conserva reglas de HU-012.
## Dependencias y relaciones
- Épica: [[EP-003-profesionales-y-disponibilidad]]; depende de [[HU-012-crear-bloques-disponibilidad]].
## Esfuerzo
**Nivel:** Alto. **Justificación:** integridad entre agenda y compromisos futuros.
## Tareas de desarrollo
- [ ] **T-01 — Definir estado comprometido y contrato de modificación**. Dificultad: Alto.
- [ ] **T-02 — Aplicar ownership y revalidar solapes**. Dificultad: Alto.
- [ ] **T-03 — Ofrecer acciones claras/inhabilitadas en calendario**. Dificultad: Medio.
- [ ] **T-04 — Probar bloque pasado, ajeno y comprometido**. Dificultad: Alto.
## Criterios de aceptación
### CA-01 — Edición válida
**Dado** bloque propio futuro sin citas comprometidas **cuando** lo edita con datos válidos **entonces** se actualiza sin solapes.
### CA-02 — Eliminación válida
**Dado** bloque propio futuro sin compromisos **cuando** lo elimina **entonces** deja de estar disponible.
### CA-03 — Protección de compromiso
**Dado** bloque pasado, ajeno o comprometido **cuando** intenta cambiarlo **entonces** se rechaza y no altera disponibilidad.
## Definition of Done
- [ ] CA-01 a CA-03 validados contra reservas/retenciones cuando existan.
- [ ] API/UI representan por qué una acción no está disponible sin filtrar datos ajenos.
- [ ] Pruebas cubren revalidación de solapes y ownership.
## Evidencia de validación
| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
| CA-01..CA-03 / DoD | Pendiente | — | — |
## Historial de validación
- 2026-09-17 — Creada en `Pendiente de aprobación`.
## Notas y decisiones
- “Comprometida” incluirá estados según contrato de reserva.
