---
id: EP-006
tipo: epica
titulo: Operación y trazabilidad
estado: Pendiente de aprobación
historias: ["[[HU-023-consultar-agenda-profesional]]", "[[HU-024-cerrar-atencion]]", "[[HU-025-gestionar-bandeja-administrativa]]"]
dependencias: ["[[EP-004-ciclo-de-vida-de-citas]]", "[[EP-005-reprogramacion]]"]
---
# EP-006 — Operación y trazabilidad
## Objetivo
Dar vistas y acciones operativas por rol con transiciones auditables.
## Valor esperado
PROFESSIONAL atiende su agenda y ADMIN prioriza pendientes sin acceder a datos ajenos.
## Actores
- PROFESSIONAL, ADMIN.
## Alcance
- Agenda profesional, cierre y bandeja administrativa.
## Fuera de alcance
- Historia clínica, facturación o automatización n8n.
## Reglas de negocio
- Ownership profesional; estados `COMPLETED`/`NO_SHOW`; historial con actor, fuente, fecha y motivo opcional.
## Dependencias
- [[EP-004-ciclo-de-vida-de-citas]], [[EP-005-reprogramacion]].
## Historias de usuario
- [[HU-023-consultar-agenda-profesional]]
- [[HU-024-cerrar-atencion]]
- [[HU-025-gestionar-bandeja-administrativa]]
## Criterio de completitud de la épica
- [ ] Todas sus HU están `Completada` y roles ven solamente datos autorizados.
## Riesgos e incógnitas
- Precisar qué citas “pasadas/aplicables” admite cierre en el contrato de negocio.
