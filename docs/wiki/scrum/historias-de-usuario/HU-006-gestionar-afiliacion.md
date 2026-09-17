---
id: HU-006
tipo: historia-de-usuario
titulo: Gestionar afiliación
estado: Pendiente de aprobación
epica: "[[EP-002-perfil-y-catalogos]]"
esfuerzo: Medio
sprint_sugerido: Incremento 2
dependencias: ["[[HU-005-consultar-y-actualizar-perfil]]", "[[HU-008-gestionar-eps-y-planes]]", "[[HU-007-disponer-catalogos-fijos]]"]
relacionadas: []
---
# HU-006 — Gestionar afiliación
## Historia de usuario
**COMO** USER **QUIERO** asociar mi EPS, plan y régimen **PARA** completar mi afiliación sin duplicar catálogos.
## Alcance
- Consulta y asociación propia de EPS, plan y régimen.
## Fuera de alcance
- Crear catálogos desde perfil.
## Reglas de negocio
- Referencias normalizadas; plan corresponde a EPS; no se duplican nombres de catálogo en usuario.
## Dependencias y relaciones
- Épica: [[EP-002-perfil-y-catalogos]]; depende de [[HU-005-consultar-y-actualizar-perfil]], [[HU-007-disponer-catalogos-fijos]] y [[HU-008-gestionar-eps-y-planes]].
## Esfuerzo
**Nivel:** Medio. **Justificación:** integridad entre tres catálogos y ownership.
## Tareas de desarrollo
- [ ] **T-01 — Definir relación de afiliación 3FN y contrato**. Dificultad: Medio.
- [ ] **T-02 — Validar EPS-plan-régimen activos**. Dificultad: Medio.
- [ ] **T-03 — Crear selector accesible dependiente de catálogo**. Dificultad: Medio.
- [ ] **T-04 — Probar combinaciones inválidas y ownership**. Dificultad: Medio.
## Criterios de aceptación
### CA-01 — Asociación válida
**Dado** catálogos activos compatibles **cuando** USER guarda afiliación **entonces** queda asociada a su perfil mediante referencias.
### CA-02 — Integridad
**Dado** un plan que no pertenece a la EPS o catálogo inactivo **cuando** lo envía **entonces** se rechaza y conserva la afiliación previa.
## Definition of Done
- [ ] CA-01 y CA-02 validados con integridad referencial.
- [ ] Flyway/modelo justifican ausencia de dependencias transitivas y duplicación.
- [ ] UI refleja carga, vacío, error y éxito del catálogo.
## Evidencia de validación
| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
| CA-01, CA-02 / DoD | Pendiente | — | — |
## Historial de validación
- 2026-09-17 — Creada en `Pendiente de aprobación`.
## Notas y decisiones
- La cardinalidad exacta de afiliación se confirma en el diseño 3FN.
