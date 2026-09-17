---
id: HU-010
tipo: historia-de-usuario
titulo: Crear profesional
estado: Pendiente de aprobación
epica: "[[EP-003-profesionales-y-disponibilidad]]"
esfuerzo: Medio
sprint_sugerido: Incremento 2
dependencias: ["[[HU-002-iniciar-sesion]]"]
relacionadas: ["[[HU-011-configurar-capacidades-profesional]]"]
---
# HU-010 — Crear profesional
## Historia de usuario
**COMO** ADMIN **QUIERO** crear un usuario PROFESSIONAL con código y matrícula ficticia **PARA** habilitar su configuración asistencial.
## Alcance
- Alta de usuario PROFESSIONAL, código profesional y matrícula sintética.
## Fuera de alcance
- Especialidades, sedes y agenda.
## Reglas de negocio
- Datos exclusivamente sintéticos; ADMIN es quien crea PROFESSIONAL.
## Dependencias y relaciones
- Épica: [[EP-003-profesionales-y-disponibilidad]]; depende de [[HU-002-iniciar-sesion]]; habilita [[HU-011-configurar-capacidades-profesional]].
## Esfuerzo
**Nivel:** Medio. **Justificación:** identidad especializada y permisos por rol.
## Tareas de desarrollo
- [ ] **T-01 — Diseñar extensión profesional normalizada y contrato**. Dificultad: Medio.
- [ ] **T-02 — Implementar alta ADMIN y seguridad**. Dificultad: Medio.
- [ ] **T-03 — Crear formulario/listado administrativo accesible**. Dificultad: Medio.
- [ ] **T-04 — Probar rol, datos sintéticos y unicidad aplicable**. Dificultad: Medio.
## Criterios de aceptación
### CA-01 — Alta autorizada
**Dado** ADMIN autenticado **cuando** registra datos profesionales válidos **entonces** se crea un usuario con rol PROFESSIONAL y sus identificadores ficticios.
### CA-02 — Protección
**Dado** USER o PROFESSIONAL **cuando** intenta crear profesional **entonces** la operación es denegada.
## Definition of Done
- [ ] CA-01 y CA-02 validados con autorización.
- [ ] Modelo separa datos de usuario y datos profesionales conforme a 3FN.
- [ ] UI/API no usan ni exponen datos personales reales.
## Evidencia de validación
| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
| CA-01, CA-02 / DoD | Pendiente | — | — |
## Historial de validación
- 2026-09-17 — Creada en `Pendiente de aprobación`.
## Notas y decisiones
- Los campos exactos no requeridos por PRD se mantienen abiertos.
