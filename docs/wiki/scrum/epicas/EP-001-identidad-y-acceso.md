---
id: EP-001
tipo: epica
titulo: Identidad y acceso
estado: Pendiente de aprobación
historias: ["[[HU-001-registrar-usuario]]", "[[HU-002-iniciar-sesion]]", "[[HU-003-renovar-y-cerrar-sesion]]", "[[HU-004-recuperar-contrasena]]"]
dependencias: []
---
# EP-001 — Identidad y acceso
## Objetivo
Permitir acceso seguro de usuarios ficticios y establecer su contexto de autorización.
## Valor esperado
Los actores autenticados acceden solo a capacidades de su rol.
## Actores
- Visitante, USER, PROFESSIONAL, ADMIN.
## Alcance
- Registro USER, JWT access/refresh, logout y recuperación de contraseña.
## Fuera de alcance
- Integración SMTP obligatoria o credenciales reales.
## Reglas de negocio
- Email/documento únicos; hash adaptativo; no exponer ni registrar secretos; roles en autorización.
## Dependencias
- Ninguna externa; [[HU-007-disponer-catalogos-fijos]] habilita roles persistidos.
## Historias de usuario
- [[HU-001-registrar-usuario]]
- [[HU-002-iniciar-sesion]]
- [[HU-003-renovar-y-cerrar-sesion]]
- [[HU-004-recuperar-contrasena]]
## Criterio de completitud de la épica
- [ ] Todas sus HU están `Completada` con evidencia.
- [ ] Autenticación y autorización no exponen credenciales, contraseñas ni tokens.
## Riesgos e incógnitas
- Definir contrato de errores y canal seguro de token de recuperación en desarrollo.
