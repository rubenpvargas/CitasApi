---
tipo: indice-scrum
estado: Pendiente de aprobación
---

# Mapa Scrum / Spec-Driven Development — Sistema de Citas

## Propósito y límites

Mapa trazable del PRD v1.0 para implementar el producto por incrementos verificables. Todas las historias están **Pendiente de aprobación**: ninguna autoriza implementación hasta revisión explícita. Se usan únicamente datos sintéticos. El frontend será React o Angular tras la decisión de Stitch/AI Studio; consumirá REST/JSON directamente desde Spring Boot, sin BFF.

## Arquitectura y datos que condicionan las HU

- API: Java 21, Spring Boot 3.5.x, Maven, arquitectura hexagonal, Spring Data JPA, MySQL 8.4, Flyway, Spring Security y JWT access/refresh.
- Datos: 3FN; catálogos fijos por seed; relaciones N:M para roles, especialidades y sedes; auditoría inmutable; no duplicar atributos de catálogos en usuario/cita.
- Integración: antes de cada HU cross-repo se documentará el contrato REST canónico, sus consumidores y la evidencia backend/frontend. No se presuponen rutas, DTO ni tablas.

## Épicas

- [[EP-001-identidad-y-acceso]] — [[HU-001-registrar-usuario]], [[HU-002-iniciar-sesion]], [[HU-003-renovar-y-cerrar-sesion]], [[HU-004-recuperar-contrasena]]
- [[EP-002-perfil-y-catalogos]] — [[HU-005-consultar-y-actualizar-perfil]], [[HU-006-gestionar-afiliacion]], [[HU-007-disponer-catalogos-fijos]], [[HU-008-gestionar-eps-y-planes]], [[HU-009-gestionar-especialidades]]
- [[EP-003-profesionales-y-disponibilidad]] — [[HU-010-crear-profesional]], [[HU-011-configurar-capacidades-profesional]], [[HU-012-crear-bloques-disponibilidad]], [[HU-013-modificar-bloques-futuros]], [[HU-014-consultar-calendario-profesional]], [[HU-015-consultar-disponibilidad]]
- [[EP-004-ciclo-de-vida-de-citas]] — [[HU-016-reservar-cita-general]], [[HU-017-solicitar-cita-especializada]], [[HU-018-decidir-solicitud-especializada]], [[HU-019-consultar-mis-citas]], [[HU-020-cancelar-cita]]
- [[EP-005-reprogramacion]] — [[HU-021-solicitar-reprogramacion]], [[HU-022-decidir-reprogramacion]]
- [[EP-006-operacion-y-trazabilidad]] — [[HU-023-consultar-agenda-profesional]], [[HU-024-cerrar-atencion]], [[HU-025-gestionar-bandeja-administrativa]]

## Incrementos sugeridos (sin estimación temporal)

1. **Incremento 1 — Identidad y base navegable:** HU-007 → HU-001 → HU-002 → HU-003. Resultado: acceso seguro sobre catálogos fijos; HU-004 queda como continuación de identidad.
2. **Incremento 2 — Administración y perfil:** HU-004 → HU-008 → HU-009 → HU-010 → HU-011 → HU-005 → HU-006. Resultado: perfiles y oferta asistencial administrables.
3. **Incremento 3 — Agenda consultable:** HU-012 → HU-013 → HU-014 → HU-015. Resultado: disponibilidad publicada y verificable.
4. **Incremento 4 — Cita de extremo a extremo:** HU-016 → HU-017 → HU-018 → HU-019 → HU-020. Resultado: reserva, decisión y cancelación trazables.
5. **Incremento 5 — Operación posterior:** HU-021 → HU-022 → HU-023 → HU-024 → HU-025. Resultado: reprogramación y operación por rol.

## Decisiones e incógnitas abiertas

- **PREGUNTA ABIERTA:** elegir React o Angular solo después de diseño/exportación aprobados.
- **PREGUNTA ABIERTA:** contrato REST canónico, convención de errores y mecanismo seguro de recuperación en desarrollo se diseñarán por HU antes de implementar.
- **DECISIÓN (fuente):** los trabajos n8n de S5/S6 no cambian el núcleo y no forman parte de estas HU.
