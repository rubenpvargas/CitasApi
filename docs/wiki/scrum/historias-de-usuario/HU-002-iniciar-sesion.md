---
id: HU-002
tipo: historia-de-usuario
titulo: Iniciar sesión
estado: En desarrollo
epica: "[[EP-001-identidad-y-acceso]]"
esfuerzo: Alto
sprint_sugerido: Incremento 1
dependencias: ["[[HU-001-registrar-usuario]]"]
relacionadas: ["[[HU-003-renovar-y-cerrar-sesion]]"]
---
# HU-002 — Iniciar sesión
## Historia de usuario
**COMO** usuario registrado **QUIERO** iniciar sesión con email y contraseña **PARA** acceder según mi rol.
## Alcance
- Autenticación por email/contraseña y emisión de access/refresh JWT separados.
## Fuera de alcance
- Renovación, revocación y recuperación.
## Reglas de negocio
- Roles forman contexto de autorización; no exponer password ni tokens en logs.
## Dependencias y relaciones
- Épica: [[EP-001-identidad-y-acceso]]; depende de [[HU-001-registrar-usuario]].
## Esfuerzo
**Nivel:** Alto. **Justificación:** seguridad, contratos, persistencia de refresh y UI por rol.
## Tareas de desarrollo
- [ ] **T-01 — Diseñar contrato de login y respuesta segura**. Dificultad: Alto.
- [ ] **T-02 — Implementar autenticación y emisión JWT**. Dificultad: Alto. Spring Security, hash y claims de rol.
- [ ] **T-03 — Integrar pantalla y contexto de sesión**. Dificultad: Medio. Estados de error y navegación autorizada.
- [ ] **T-04 — Probar credenciales válidas, inválidas y roles**. Dificultad: Alto.
## Criterios de aceptación
### CA-01 — Acceso válido
**Dado** credenciales válidas **cuando** inicia sesión **entonces** recibe sesión con access y refresh separados y accede a su contexto de rol.
### CA-02 — Rechazo seguro
**Dado** credenciales inválidas **cuando** intenta iniciar sesión **entonces** recibe error seguro sin revelar cuál dato falló.
### CA-03 — Aislamiento por rol
**Dado** una sesión autenticada **cuando** solicita una capacidad no autorizada **entonces** el backend la deniega.
## Definition of Done
- [ ] CA-01 a CA-03 validados; tokens/contraseñas no se registran.
- [ ] Secretos se obtienen solo desde configuración de entorno.
- [ ] Contrato, cliente REST y protección de rutas UI son coherentes; backend conserva la autoridad.
- [ ] Pruebas de seguridad y trazabilidad Scrum disponibles.
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
- La ubicación de tokens en el cliente se definirá con el contrato de seguridad.
