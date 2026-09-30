---
id: HU-001
tipo: historia-de-usuario
titulo: Registrar usuario
estado: Completada
epica: "[[EP-001-identidad-y-acceso]]"
esfuerzo: Medio
sprint_sugerido: Incremento 1
dependencias: ["[[HU-007-disponer-catalogos-fijos]]"]
relacionadas: ["[[HU-002-iniciar-sesion]]"]
---
# HU-001 — Registrar usuario
## Historia de usuario
**COMO** visitante **QUIERO** crear una cuenta USER con mis datos mínimos **PARA** usar el agendamiento.
## Alcance
- Nombres, apellidos, tipo/número de documento, email, teléfono y contraseña; cuenta USER sintética.
## Fuera de alcance
- Alta de PROFESSIONAL o ADMIN.
## Reglas de negocio
- Email y documento son únicos; contraseña con hash adaptativo, nunca texto plano.
## Dependencias y relaciones
- Épica: [[EP-001-identidad-y-acceso]]; depende de [[HU-007-disponer-catalogos-fijos]].
## Esfuerzo
**Nivel:** Medio. **Justificación:** valida identidad y seguridad en persistencia y UI.
## Tareas de desarrollo
- [ ] **T-01 — Definir contrato de registro y errores**. Dificultad: Medio. Coordinar DTO/validaciones REST y consumidor frontend.
- [ ] **T-02 — Modelar usuario en 3FN y unicidad**. Dificultad: Medio. Migración Flyway, índices y hash en adaptador de seguridad.
- [ ] **T-03 — Crear formulario accesible**. Dificultad: Medio. Estados loading/error/success y mensajes seguros.
- [ ] **T-04 — Probar validaciones y duplicados**. Dificultad: Medio.
## Criterios de aceptación
### CA-01 — Registro válido
**Dado** datos mínimos válidos **cuando** el visitante los envía **entonces** se crea un USER y recibe confirmación sin revelar datos sensibles.
### CA-02 — Unicidad
**Dado** email o documento existente **cuando** intenta registrarse **entonces** se rechaza de forma segura y no se crea duplicado.
### CA-03 — Credencial protegida
**Dado** una cuenta creada **cuando** se persiste **entonces** la contraseña no queda en texto plano ni se devuelve.
## Definition of Done
- [ ] CA-01 a CA-03 validados con pruebas relevantes.
- [ ] Migración Flyway e índices de unicidad justifican el modelo 3FN.
- [ ] Contrato REST y formulario frontend coinciden; validación server-side presente.
- [ ] Trazabilidad Scrum actualizada sin secretos.
## Evidencia de validación
| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
| CA-01 | Pendiente | — | — |
| CA-02 | Pendiente | — | — |
| CA-03 / DoD | Pendiente | — | — |
## Historial de validación
- 2026-09-17 — Creada en `Pendiente de aprobación`.
- 2026-09-17 — Aprobada explícitamente por el usuario para el incremento backend HU-001 a HU-003.
- 2026-09-17 — Desarrollo backend iniciado en la rama `develop`; UI permanece fuera de alcance.
## Notas y decisiones
- No declarar aprobada sin revisión explícita del usuario.
