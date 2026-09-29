---
id: HU-025
tipo: historia-de-usuario
titulo: Gestionar bandeja administrativa
estado: Completada
epica: "[[EP-006-operacion-y-trazabilidad]]"
esfuerzo: Medio
sprint_sugerido: Incremento 5
dependencias: ["[[HU-017-solicitar-cita-especializada]]", "[[HU-021-solicitar-reprogramacion]]"]
relacionadas: ["[[HU-018-decidir-solicitud-especializada]]", "[[HU-022-decidir-reprogramacion]]"]
---
# HU-025 — Gestionar bandeja administrativa
## Historia de usuario
**COMO** ADMIN **QUIERO** consultar solicitudes especializadas y reprogramaciones pendientes con filtros **PARA** decidirlas de forma priorizada.
## Alcance
- Bandeja de citas REQUESTED y reprogramaciones PENDING; filtros sede, profesional, especialidad y fecha.
## Fuera de alcance
- Ejecutar las decisiones, cubiertas en HU-018 y HU-022.
## Reglas de negocio
- Solo ADMIN; muestra únicamente pendientes y sus filtros requeridos.
## Dependencias y relaciones
- Épica: [[EP-006-operacion-y-trazabilidad]]; depende de [[HU-017-solicitar-cita-especializada]] y [[HU-021-solicitar-reprogramacion]].
## Esfuerzo
**Nivel:** Medio. **Justificación:** agrega dos clases de pendiente y cuatro filtros.
## Tareas de desarrollo
- [ ] **T-01 — Definir respuesta unificada o vistas separadas de bandeja**. Dificultad: Medio.
- [ ] **T-02 — Implementar consulta ADMIN, filtros e índices**. Dificultad: Medio.
- [ ] **T-03 — Crear bandeja accesible y enlaces a decisión**. Dificultad: Medio.
- [ ] **T-04 — Probar rol, filtros y exclusión de estados resueltos**. Dificultad: Medio.
## Criterios de aceptación
### CA-01 — Pendientes visibles
**Dado** ADMIN autenticado **cuando** abre bandeja **entonces** ve citas especializadas REQUESTED y reprogramaciones PENDING.
### CA-02 — Filtros
**Dado** criterios sede, profesional, especialidad y/o fecha **cuando** los aplica **entonces** visualiza solo pendientes coincidentes.
### CA-03 — Restricción
**Dado** rol distinto de ADMIN **cuando** solicita la bandeja **entonces** el backend la deniega.
## Definition of Done
- [ ] CA-01 a CA-03 validados con autorización y filtros.
- [ ] UI maneja estados loading/vacío/error/éxito y dirige a HU-018/HU-022.
- [ ] Consultas no incluyen solicitudes ya resueltas como pendientes.
## Evidencia de validación
| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
| CA-01..CA-03 / DoD | Pendiente | — | — |
## Historial de validación
- 2026-09-17 — Creada en Pendiente de aprobación.
## Notas y decisiones
- Las decisiones permanecen separadas para conservar HU pequeñas.
