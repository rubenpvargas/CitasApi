# Guía para `citas-api`

## Estado verificado del repositorio

El repositorio es un esqueleto: todavía no contiene `pom.xml` ni `src/`. No
asumir estructura de paquetes, comandos de build, librerías adicionales ni un
contrato REST ya existente. Antes de implementar, inspeccionar el proyecto que
se haya inicializado y actualizar estas instrucciones solo con evidencia.

## Alcance de este repositorio

Implementar únicamente el backend del sistema de citas: Java 21, Spring Boot
3.5.x, Maven, REST/JSON, seguridad con JWT access/refresh, MySQL 8.4, Spring
Data JPA, Flyway, reglas del PRD, pruebas y contratos backend.

No editar `citas-web`, no acoplar la API a React/Angular y no crear Express ni
un BFF. La LLM Wiki bajo `docs/wiki/llm-wiki/` es global y la mantiene el agente
orquestador: este agente puede consultarla, pero no la mantiene ni la usa como
registro de trabajo propio.

## Arquitectura exigida

- El dominio no depende de Spring, JPA, HTTP ni detalles de infraestructura.
- Los casos de uso pertenecen a aplicación.
- Los puertos expresan dependencias de entrada y salida.
- REST, persistencia, seguridad y configuración son adaptadores.
- Los controladores traducen HTTP, validan el borde y delegan; no concentran
  reglas de negocio.
- Todo cambio de esquema incluye migración Flyway, justificación de la
  normalización/índices y pruebas de persistencia relevantes.

## Reglas de producto que no se pueden relajar

- No permitir doble reserva o retención de slots; 60 minutos requiere dos slots
  consecutivos.
- No permitir bloques ni citas en el pasado; el profesional solo opera en sedes
  asignadas y con especialidades activas asociadas.
- Las citas generales se aprueban automáticamente. Las especializadas nacen
  `REQUESTED`; aprobar/rechazar es responsabilidad de ADMIN y el rechazo exige
  motivo.
- Cancelar o rechazar libera los slots correspondientes. Una reprogramación
  conserva la cita/franja original hasta ser aprobada y deja auditoría.
- Las transiciones de cita son explícitas, verificables y auditables; la
  auditoría no se trata como CRUD ordinario.

## Seguridad y datos

- Hash adaptativo para passwords; nunca almacenar texto plano.
- Secretos solo por variables de entorno; `.env.example` no contiene valores
  reales. No abrir, mostrar ni versionar `.env`.
- No registrar passwords, JWT, refresh tokens ni credenciales.
- Aplicar autorización por rol y ownership, CORS explícito y validación
  server-side.
- Usar exclusivamente datos sintéticos; no introducir datos privados reales de
  FCV.

## Flujo por incremento

1. Localizar una HU aprobada y su DoD en `docs/wiki/scrum/`. Si no existen,
   detener implementación y solicitar o esperar la especificación aprobada.
2. Identificar reglas del PRD, puertos/adaptadores, migraciones y contrato REST
   afectados.
3. Antes de editar, presentar el plan con archivos, riesgo de compatibilidad y
   evidencia esperada. Si cambia REST, coordinar la evidencia con `citas-web`
   sin editarlo desde aquí.
4. Implementar el mínimo coherente, preservando los límites hexagonales.
5. Ejecutar pruebas de dominio, aplicación y, cuando aplique, integración
   REST/persistencia. Descubrir primero los comandos desde `pom.xml` o el
   wrapper; no inventarlos mientras el proyecto no exista.
6. Verificar arquitectura y DoD, y resumir pruebas ejecutadas, evidencia y lo
   no verificado.

## Git y automatizaciones

Usar `develop` para trabajo y reservar `main` para incrementos estables. Si la
rama no existe, informar antes de crearla. No reescribir historial para borrar
progreso. Los workflows n8n se versionan como JSON en `automations/n8n/` y no
incluyen secretos.
