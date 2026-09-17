---
id: HU-009
tipo: historia-de-usuario
titulo: Gestionar especialidades
estado: Pendiente de aprobación
epica: "[[EP-002-perfil-y-catalogos]]"
esfuerzo: Medio
sprint_sugerido: Incremento 2
dependencias: ["[[HU-002-iniciar-sesion]]"]
relacionadas: ["[[HU-011-configurar-capacidades-profesional]]", "[[HU-015-consultar-disponibilidad]]"]
---
# HU-009 — Gestionar especialidades
## Historia de usuario
**COMO** ADMIN **QUIERO** gestionar especialidades y su duración **PARA** definir oferta y slots requeridos para una cita.
## Alcance
- CRUD, activación/desactivación y duración de 30 o 60 minutos.
## Fuera de alcance
- Elegir duración por profesional.
## Reglas de negocio
- Solo 30/60 min; especialidad referenciada no se borra físicamente; duración la determina catálogo.
## Dependencias y relaciones
- Épica: [[EP-002-perfil-y-catalogos]]; depende de [[HU-002-iniciar-sesion]]; habilita [[HU-011-configurar-capacidades-profesional]] y [[HU-015-consultar-disponibilidad]].
## Esfuerzo
**Nivel:** Medio. **Justificación:** catálogo administrativo que gobierna disponibilidad y reserva.
## Tareas de desarrollo
- [ ] **T-01 — Definir contrato y restricciones de duración**. Dificultad: Medio.
- [ ] **T-02 — Persistir catálogo y proteger bajas referenciadas**. Dificultad: Medio.
- [ ] **T-03 — Construir CRUD ADMIN con validación**. Dificultad: Medio.
- [ ] **T-04 — Probar duración inválida, rol y desactivación**. Dificultad: Medio.
## Criterios de aceptación
### CA-01 — Duración válida
**Dado** ADMIN **cuando** crea o edita una especialidad **entonces** solo puede asignar 30 o 60 minutos.
### CA-02 — Ciclo seguro
**Dado** especialidad usada **cuando** ADMIN intenta eliminarla **entonces** se evita borrado físico y puede desactivarse.
### CA-03 — Uso posterior
**Dado** una especialidad activa **cuando** se consume en agenda/disponibilidad **entonces** su duración es la referencia aplicable.
## Definition of Done
- [ ] CA-01 a CA-03 validados; API y UI restringen valores sin sustituir validación servidor.
- [ ] Migración/documentación 3FN mantiene especialidad como catálogo único.
- [ ] Dependencias de HU-011/HU-015 actualizadas.
## Evidencia de validación
| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
| CA-01..CA-03 / DoD | Pendiente | — | — |
## Historial de validación
- 2026-09-17 — Creada en `Pendiente de aprobación`.
## Notas y decisiones
- Medicina General se identifica en el catálogo sin fijar implementación.
