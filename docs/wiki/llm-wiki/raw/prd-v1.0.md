# Extracto curado — PRD 1.0

Fuente: `PRD.md`. Estado: aprobado para ingestión; no sustituye al documento
canónico.

- El producto es un sistema académico de agendamiento con datos sintéticos.
- Actores: `USER`, `PROFESSIONAL` y `ADMIN`.
- El backend es Spring Boot REST y el frontend consume directamente; no existe
  Express/BFF.
- Las citas generales se aprueban automáticamente; las especializadas y las
  reprogramaciones requieren decisión administrativa.
- La disponibilidad se discretiza en slots de 30 minutos y las duraciones son
  30 o 60 minutos.
- Cancelaciones, rechazos y reprogramaciones deben liberar o conservar slots
  según la transición y dejar auditoría de estados.
- Quedan fuera de alcance historia clínica, facturación, pagos, diagnósticos,
  integraciones clínicas, SMS/WhatsApp y SMTP obligatorio.
