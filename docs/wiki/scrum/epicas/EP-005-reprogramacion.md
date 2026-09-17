---
id: EP-005
tipo: epica
titulo: Reprogramación preservando la cita original
estado: Pendiente de aprobación
historias: ["[[HU-021-solicitar-reprogramacion]]", "[[HU-022-decidir-reprogramacion]]"]
dependencias: ["[[EP-004-ciclo-de-vida-de-citas]]"]
---
# EP-005 — Reprogramación preservando la cita original
## Objetivo
Gestionar una franja provisional sin perder la cita original hasta decisión administrativa.
## Valor esperado
USER solicita un cambio seguro y ADMIN lo resuelve trazablemente.
## Actores
- USER, ADMIN.
## Alcance
- Solicitud `PENDING`, retención, decisión y liberación/traslado de slots.
## Fuera de alcance
- Cambio de profesional como reprogramación.
## Reglas de negocio
- Solo cita futura aprobada; conserva profesional/especialidad; rechazo con motivo cuando aplique; original se mantiene hasta aprobación.
## Dependencias
- [[EP-004-ciclo-de-vida-de-citas]].
## Historias de usuario
- [[HU-021-solicitar-reprogramacion]]
- [[HU-022-decidir-reprogramacion]]
## Criterio de completitud de la épica
- [ ] Todas sus HU están `Completada` y no existe pérdida de franja original sin aprobación.
## Riesgos e incógnitas
- Definir invariantes transaccionales de la reserva provisional.
