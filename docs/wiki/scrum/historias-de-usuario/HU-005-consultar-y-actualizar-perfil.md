---
id: HU-005
tipo: historia-de-usuario
titulo: Consultar y actualizar perfil
estado: Completada
epica: "[[EP-002-perfil-y-catalogos]]"
esfuerzo: Medio
sprint_sugerido: Incremento 2
dependencias: ["[[HU-002-iniciar-sesion]]"]
relacionadas: ["[[HU-006-gestionar-afiliacion]]"]
---
# HU-005 — Consultar y actualizar perfil
## Historia de usuario
**COMO** USER **QUIERO** consultar y actualizar mis datos permitidos **PARA** mantener mi información de contacto vigente.
## Alcance
- Consulta propia y actualización de campos permitidos que se definirán en contrato.
## Fuera de alcance
- Modificar roles, datos de otros usuarios o afiliación.
## Reglas de negocio
- Ownership obligatorio; email/documento siguen siendo únicos si son editables.
## Dependencias y relaciones
- Épica: [[EP-002-perfil-y-catalogos]]; depende de [[HU-002-iniciar-sesion]].
## Esfuerzo
**Nivel:** Medio. **Justificación:** ownership, validación y coherencia perfil/UI.
## Tareas de desarrollo
- [ ] **T-01 — Acordar campos editables y contrato**. Dificultad: Medio.
- [ ] **T-02 — Aplicar autorización y validación server-side**. Dificultad: Medio.
- [ ] **T-03 — Implementar vista/formulario con estados UI**. Dificultad: Medio.
- [ ] **T-04 — Probar ownership y unicidad aplicable**. Dificultad: Medio.
## Criterios de aceptación
### CA-01 — Consulta propia
**Dado** USER autenticado **cuando** consulta perfil **entonces** visualiza solo su información permitida.
### CA-02 — Actualización válida
**Dado** datos permitidos válidos **cuando** los guarda **entonces** se persisten y se muestran actualizados.
### CA-03 — Protección
**Dado** una petición sobre otro usuario o dato inválido **cuando** se procesa **entonces** se deniega o valida sin modificar datos.
## Definition of Done
- [ ] CA-01 a CA-03 con pruebas de ownership y validación.
- [ ] Contrato REST, UI y persistencia no exponen campos sensibles.
- [ ] Si se altera esquema, Flyway y 3FN quedan cubiertos.
## Evidencia de validación
| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
| CA-01..CA-03 / DoD | Pendiente | — | — |
## Historial de validación
- 2026-09-17 — Creada en `Pendiente de aprobación`.
## Notas y decisiones
- Campos permitidos: pregunta abierta de contrato.
