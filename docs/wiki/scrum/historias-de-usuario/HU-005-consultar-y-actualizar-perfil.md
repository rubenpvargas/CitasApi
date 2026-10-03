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
- [x] **T-01 — Acordar campos editables y contrato**. Dificultad: Medio.
- [x] **T-02 — Aplicar autorización y validación server-side**. Dificultad: Medio.
- [x] **T-03 — Implementar vista/formulario con estados UI**. Dificultad: Medio.
- [x] **T-04 — Probar ownership y unicidad aplicable**. Dificultad: Medio.
## Criterios de aceptación
### CA-01 — Consulta propia
**Dado** USER autenticado **cuando** consulta perfil **entonces** visualiza solo su información permitida.
### CA-02 — Actualización válida
**Dado** datos permitidos válidos **cuando** los guarda **entonces** se persisten y se muestran actualizados.
### CA-03 — Protección
**Dado** una petición sobre otro usuario o dato inválido **cuando** se procesa **entonces** se deniega o valida sin modificar datos.
## Definition of Done
- [x] CA-01 a CA-03 con pruebas de ownership y validación.
- [x] Contrato REST, UI y persistencia no exponen campos sensibles.
- [x] Si se altera esquema, Flyway y 3FN quedan cubiertos.
## Evidencia de validación
| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
| CA-01 | Conforme | `ProfileIT`, `ProfileServiceTest`: `GET /me` devuelve solo datos propios por `sub` del JWT, sin hash; smoke 2026-10-03: 200, 0 campos hash | API `704db7e` |
| CA-02 | Conforme | `PATCH /me {firstName,lastName,phone}` → 200 y lectura posterior actualizada (IT y smoke) | Email/documento no editables en v1 |
| CA-03 | Conforme | Datos inválidos → 400 `VALIDATION_ERROR` sin modificar (smoke: nombre conservado); no existe ruta sobre otro usuario (ownership por token) | — |
| DoD UI | Conforme | Web `c5ea141`: perfil editable con estados carga/error/éxito/validación accesibles; specs del componente y `ProfileApi` | Sin esquema nuevo |
| Pruebas | Conforme | Verificación independiente `0ad72ce`: 62 unitarias + 55 IT, 0 fallos; web `8f6f48f`: lint OK, 187/187, build OK | — |

## Historial de validación
- 2026-09-17 — Creada en `Pendiente de aprobación`.
- 2026-10-03 — Validada con pruebas automatizadas, smoke HTTP y verificación independiente (API `704db7e`; web `c5ea141`).

## Notas y decisiones
- Campos permitidos: pregunta abierta de contrato.
