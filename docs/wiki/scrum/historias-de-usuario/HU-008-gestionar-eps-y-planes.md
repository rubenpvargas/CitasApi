---
id: HU-008
tipo: historia-de-usuario
titulo: Gestionar EPS y planes
estado: Pendiente de aprobación
epica: "[[EP-002-perfil-y-catalogos]]"
esfuerzo: Medio
sprint_sugerido: Incremento 2
dependencias: ["[[HU-002-iniciar-sesion]]"]
relacionadas: ["[[HU-006-gestionar-afiliacion]]"]
---
# HU-008 — Gestionar EPS y planes
## Historia de usuario
**COMO** ADMIN **QUIERO** gestionar EPS y sus planes **PARA** ofrecer opciones de afiliación vigentes.
## Alcance
- CRUD de EPS y planes, incluyendo activación/desactivación.
## Fuera de alcance
- Borrado físico si existe referencia transaccional.
## Reglas de negocio
- Un plan depende de una EPS; registros referenciados se desactivan.
## Dependencias y relaciones
- Épica: [[EP-002-perfil-y-catalogos]]; depende de [[HU-002-iniciar-sesion]]; habilita [[HU-006-gestionar-afiliacion]].
## Esfuerzo
**Nivel:** Medio. **Justificación:** relación jerárquica, reglas de borrado y administración por rol.
## Tareas de desarrollo
- [ ] **T-01 — Definir modelo/contrato de EPS y plan**. Dificultad: Medio.
- [ ] **T-02 — Aplicar CRUD ADMIN y desactivación referencial**. Dificultad: Medio.
- [ ] **T-03 — Crear pantallas accesibles de gestión**. Dificultad: Medio.
- [ ] **T-04 — Probar permisos, relación y baja referenciada**. Dificultad: Medio.
## Criterios de aceptación
### CA-01 — Gestión autorizada
**Dado** ADMIN autenticado **cuando** crea, edita o lista EPS/planes válidos **entonces** los cambios quedan disponibles según su relación.
### CA-02 — Baja segura
**Dado** EPS o plan referenciado **cuando** ADMIN intenta retirarlo **entonces** no se elimina físicamente y puede quedar inactivo.
### CA-03 — Restricción de rol
**Dado** otro rol **cuando** intenta gestionar catálogo **entonces** el backend lo deniega.
## Definition of Done
- [ ] CA-01 a CA-03 validados con autorización e integridad referencial.
- [ ] Migración/indexación conserva relación EPS-plan en 3FN.
- [ ] UI comunica actividad, errores y estados vacíos sin asumir reglas cliente.
## Evidencia de validación
| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
| CA-01..CA-03 / DoD | Pendiente | — | — |
## Historial de validación
- 2026-09-17 — Creada en `Pendiente de aprobación`.
## Notas y decisiones
- El conjunto de campos configurables se definirá en contrato.
