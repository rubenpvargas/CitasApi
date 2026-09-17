---
id: EP-004
tipo: epica
titulo: Ciclo de vida de citas
estado: Pendiente de aprobación
historias: ["[[HU-016-reservar-cita-general]]", "[[HU-017-solicitar-cita-especializada]]", "[[HU-018-decidir-solicitud-especializada]]", "[[HU-019-consultar-mis-citas]]", "[[HU-020-cancelar-cita]]"]
dependencias: ["[[EP-003-profesionales-y-disponibilidad]]"]
---
# EP-004 — Ciclo de vida de citas
## Objetivo
Permitir reservar, decidir, consultar y cancelar citas sin doble reserva.
## Valor esperado
USER obtiene citas generales inmediatas y solicitudes especializadas auditables.
## Actores
- USER, ADMIN.
## Alcance
- Cita general/especializada, decisión, consulta y cancelación.
## Fuera de alcance
- Reprogramación y cierre de atención.
## Reglas de negocio
- General `APPROVED`; especializada `REQUESTED`; rechazo con motivo; liberar slots al rechazar/cancelar; historial inmutable.
## Dependencias
- [[EP-003-profesionales-y-disponibilidad]].
## Historias de usuario
- [[HU-016-reservar-cita-general]]
- [[HU-017-solicitar-cita-especializada]]
- [[HU-018-decidir-solicitud-especializada]]
- [[HU-019-consultar-mis-citas]]
- [[HU-020-cancelar-cita]]
## Criterio de completitud de la épica
- [ ] Todas sus HU están `Completada`; cada transición y reserva tiene evidencia verificable.
## Riesgos e incógnitas
- Pruebas de concurrencia/retención requerirán definición de contrato y persistencia.
