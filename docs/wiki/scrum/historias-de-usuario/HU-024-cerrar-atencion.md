---
id: HU-024
tipo: historia-de-usuario
titulo: Cerrar atención
estado: Pendiente de aprobación
epica: "[[EP-006-operacion-y-trazabilidad]]"
esfuerzo: Medio
sprint_sugerido: Incremento 5
dependencias: ["[[HU-023-consultar-agenda-profesional]]"]
relacionadas: []
---
# HU-024 — Cerrar atención
## Historia de usuario
**COMO** PROFESSIONAL **QUIERO** marcar una cita pasada/aplicable como completada o inasistencia **PARA** reflejar el resultado de atención.
## Alcance
- Transición de cita a COMPLETED o NO_SHOW y auditoría.
## Fuera de alcance
- Historia clínica, diagnóstico o tratamiento.
## Reglas de negocio
- Solo profesional de su propia cita; solo pasado/aplicable según regla a precisar; historial obligatorio.
## Dependencias y relaciones
- Épica: [[EP-006-operacion-y-trazabilidad]]; depende de [[HU-023-consultar-agenda-profesional]].
## Esfuerzo
**Nivel:** Medio. **Justificación:** transición autorizada y condición temporal pendiente de concretar.
## Tareas de desarrollo
- [ ] **T-01 — Precisar “pasada/aplicable” en contrato de negocio**. Dificultad: Medio.
- [ ] **T-02 — Implementar transiciones y auditoría por ownership**. Dificultad: Medio.
- [ ] **T-03 — Crear acción accesible en agenda**. Dificultad: Bajo.
- [ ] **T-04 — Probar estados, horario y profesional ajeno**. Dificultad: Medio.
## Criterios de aceptación
### CA-01 — Cierre autorizado
**Dado** cita propia elegible **cuando** PROFESSIONAL marca resultado **entonces** queda COMPLETED o NO_SHOW y se registra historial.
### CA-02 — Protección
**Dado** cita ajena o no elegible **cuando** PROFESSIONAL intenta cerrarla **entonces** se rechaza sin cambiar estado.
## Definition of Done
- [ ] CA-01 y CA-02 con evidencia de transición y ownership.
- [ ] Definición verificable de “aplicable” queda incorporada antes de validar.
- [ ] Auditoría no se modifica por CRUD ordinario.
## Evidencia de validación
| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
| CA-01, CA-02 / DoD | Pendiente | — | — |
## Historial de validación
- 2026-09-17 — Creada en Pendiente de aprobación.
## Notas y decisiones
- PREGUNTA ABIERTA: umbral exacto de cita aplicable.
