---
id: EP-002
tipo: epica
titulo: Perfil y catálogos
estado: Pendiente de aprobación
historias: ["[[HU-005-consultar-y-actualizar-perfil]]", "[[HU-006-gestionar-afiliacion]]", "[[HU-007-disponer-catalogos-fijos]]", "[[HU-008-gestionar-eps-y-planes]]", "[[HU-009-gestionar-especialidades]]"]
dependencias: ["[[EP-001-identidad-y-acceso]]"]
---
# EP-002 — Perfil y catálogos
## Objetivo
Mantener información de usuario y oferta asistencial normalizada y administrable.
## Valor esperado
Usuarios gestionan su perfil y ADMIN mantiene catálogos reutilizables sin inconsistencias.
## Actores
- USER, ADMIN.
## Alcance
- Perfil, afiliación, catálogos fijos, EPS/planes y especialidades.
## Fuera de alcance
- Borrado físico de catálogos referenciados.
## Reglas de negocio
- Catálogos fijos son seed y solo lectura; EPS, régimen y plan se referencian sin duplicación; bajas por activación/desactivación.
## Dependencias
- [[EP-001-identidad-y-acceso]].
## Historias de usuario
- [[HU-005-consultar-y-actualizar-perfil]]
- [[HU-006-gestionar-afiliacion]]
- [[HU-007-disponer-catalogos-fijos]]
- [[HU-008-gestionar-eps-y-planes]]
- [[HU-009-gestionar-especialidades]]
## Criterio de completitud de la épica
- [ ] Todas sus HU están `Completada` con evidencia y relaciones en 3FN justificadas.
## Riesgos e incógnitas
- Definir campos de perfil editables sin alterar los datos mínimos del PRD.
