---
id: HU-007
tipo: historia-de-usuario
titulo: Disponer catálogos fijos
estado: Completada
epica: "[[EP-002-perfil-y-catalogos]]"
esfuerzo: Medio
sprint_sugerido: Incremento 1
dependencias: []
relacionadas: ["[[HU-001-registrar-usuario]]", "[[HU-006-gestionar-afiliacion]]", "[[HU-015-consultar-disponibilidad]]"]
---
# HU-007 — Disponer catálogos fijos
## Historia de usuario
**COMO** usuario del sistema **QUIERO** que roles, estados, regímenes y sedes estén disponibles como catálogos controlados **PARA** operar con valores consistentes.
## Alcance
- Seed y consulta de roles, estados de cita/reprogramación, regímenes y sedes HIC/ICV.
## Fuera de alcance
- CRUD de estos catálogos.
## Reglas de negocio
- Son precargados y solo lectura; datos sintéticos salvo sedes públicas del laboratorio.
## Dependencias y relaciones
- Épica: [[EP-002-perfil-y-catalogos]]; habilita [[HU-001-registrar-usuario]], [[HU-006-gestionar-afiliacion]] y [[HU-015-consultar-disponibilidad]].
## Esfuerzo
**Nivel:** Medio. **Justificación:** base de datos, seed reproducible y consumidores cross-repo.
## Tareas de desarrollo
- [x] **T-01 — Diseñar catálogos normalizados y migraciones**. Dificultad: Medio.
- [x] **T-02 — Preparar seed idempotente de valores del PRD**. Dificultad: Medio.
- [x] **T-03 — Definir consulta REST de solo lectura**. Dificultad: Bajo.
- [x] **T-04 — Probar invariabilidad y disponibilidad de seed**. Dificultad: Medio.
## Criterios de aceptación
### CA-01 — Valores precargados
**Dado** una instalación inicial **cuando** se prepara la base **entonces** existen roles, estados, regímenes y las dos sedes fijas requeridas.
### CA-02 — Solo lectura
**Dado** un consumidor autorizado **cuando** consulta catálogos **entonces** obtiene valores consistentes; no dispone de operación de modificación por CRUD.
## Definition of Done
- [x] CA-01 y CA-02 validados en persistencia y contrato.
- [x] Flyway/seed son reproducibles y no introducen secretos ni datos personales reales.
- [x] Modelo 3FN documenta claves y valores de catálogo.
## Evidencia de validación
| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
| CA-01 | Conforme | V3 aplicada; roles USER/PROFESSIONAL/ADMIN y sedes HIC/ICV verificadas en MySQL local | Catálogos y seeds reproducibles |
| CA-02 | Conforme | `GET /api/v1/catalogs`: 401 sin JWT, 200 con JWT; POST: 405 | Consulta autenticada, sin CRUD de escritura |
| Pruebas | Conforme | `docker compose exec -T citas-api-dev mvn test` | 11 pruebas, 0 fallos |
## Historial de validación
- 2026-09-17 — Creada en `Pendiente de aprobación`.
- 2026-09-24 — Inicio autorizado explícitamente por el usuario para probar y corregir la HU-007.
- 2026-09-24 — Completada: contrato, V3 Flyway, pruebas Maven y pruebas HTTP locales validadas.
## Notas y decisiones
- Las representaciones API se acuerdan antes de consumirlas.
