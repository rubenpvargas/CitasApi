---
id: EP-003
tipo: epica
titulo: Profesionales y disponibilidad
estado: Pendiente de aprobación
historias: ["[[HU-010-crear-profesional]]", "[[HU-011-configurar-capacidades-profesional]]", "[[HU-012-crear-bloques-disponibilidad]]", "[[HU-013-modificar-bloques-futuros]]", "[[HU-014-consultar-calendario-profesional]]", "[[HU-015-consultar-disponibilidad]]"]
dependencias: ["[[EP-001-identidad-y-acceso]]", "[[EP-002-perfil-y-catalogos]]"]
---
# EP-003 — Profesionales y disponibilidad
## Objetivo
Configurar profesionales y convertir sus bloques válidos en disponibilidad reservable.
## Valor esperado
La oferta publicada refleja sede, especialidad, duración y reglas de agenda.
## Actores
- ADMIN, PROFESSIONAL, USER.
## Alcance
- Alta/configuración de profesionales, bloques, calendario y búsqueda.
## Fuera de alcance
- Citas o edición de bloques con compromisos.
## Reglas de negocio
- Sede/especialidad activa asignada; sin pasado/solape; slots de 30 min; especialidad define 30/60 min.
## Dependencias
- [[EP-002-perfil-y-catalogos]].
## Historias de usuario
- [[HU-010-crear-profesional]]
- [[HU-011-configurar-capacidades-profesional]]
- [[HU-012-crear-bloques-disponibilidad]]
- [[HU-013-modificar-bloques-futuros]]
- [[HU-014-consultar-calendario-profesional]]
- [[HU-015-consultar-disponibilidad]]
## Criterio de completitud de la épica
- [ ] Todas sus HU están `Completada` y la disponibilidad muestra solo franjas completas reservables.
## Riesgos e incógnitas
- La estrategia de concurrencia contra doble reserva se define antes de HU-016/HU-017.
