---
id: HU-019
tipo: historia-de-usuario
titulo: Consultar mis citas
estado: Pendiente de aprobación
epica: "[[EP-004-ciclo-de-vida-de-citas]]"
esfuerzo: Medio
sprint_sugerido: Incremento 4
dependencias: ["[[HU-016-reservar-cita-general]]", "[[HU-017-solicitar-cita-especializada]]"]
relacionadas: ["[[HU-020-cancelar-cita]]", "[[HU-021-solicitar-reprogramacion]]"]
---
# HU-019 — Consultar mis citas
## Historia de usuario
**COMO** USER **QUIERO** consultar y filtrar mis citas **PARA** conocer su estado y gestionar acciones posteriores.
## Alcance
- Filtro estado/fecha y detalle mínimo de sede, profesional, especialidad, fecha/hora, duración, estado y motivo de rechazo.
## Fuera de alcance
- Modificar una cita desde el listado.
## Reglas de negocio
- Solo citas propias; motivo de rechazo cuando exista.
## Dependencias y relaciones
- Épica: [[EP-004-ciclo-de-vida-de-citas]]; depende de [[HU-016-reservar-cita-general]] y [[HU-017-solicitar-cita-especializada]].
## Esfuerzo
**Nivel:** Medio. **Justificación:** filtros, detalle y ownership.
## Tareas de desarrollo
- [ ] **T-01 — Definir consulta, filtros y detalle mínimo**. Dificultad: Medio.
- [ ] **T-02 — Aplicar ownership e índices de consulta**. Dificultad: Medio.
- [ ] **T-03 — Crear listado/detalle accesible**. Dificultad: Medio.
- [ ] **T-04 — Probar filtros, vacío y acceso ajeno**. Dificultad: Medio.
## Criterios de aceptación
### CA-01 — Información mínima
**Dado** USER con citas **cuando** consulta **entonces** ve los campos mínimos exigidos, incluido motivo de rechazo si existe.
### CA-02 — Filtros
**Dado** filtros de estado y/o fecha **cuando** los aplica **entonces** solo ve sus citas coincidentes.
### CA-03 — Ownership
**Dado** intento de detalle de otra cuenta **cuando** se procesa **entonces** el backend lo deniega.
## Definition of Done
- [ ] CA-01 a CA-03 validados con pruebas de ownership y filtros.
- [ ] UI tiene loading, vacío, error, éxito y navegación accesible.
- [ ] Respuesta no replica nombres de catálogo innecesariamente en persistencia.
## Evidencia de validación
| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
| CA-01..CA-03 / DoD | Pendiente | — | — |
## Historial de validación
- 2026-09-17 — Creada en `Pendiente de aprobación`.
## Notas y decisiones
- Acciones se habilitan según el estado devuelto por backend.
