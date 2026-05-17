# CLAUDE.md

Guidance for Claude Code working on the **backend** of Al Toro Gastrobar.

**Stack:** Spring Boot 3.5 · Java 21 · PostgreSQL 15 · RabbitMQ 3.13.

**Detailed references** (lee bajo demanda, no se carga en cada turno):
- `docs/api-conventions.md` — Convenciones de naming y diseño de endpoints
- `docs/coding-patterns.md` — Services, Mappers, DTOs, Tests, ApiResponse, WebSocket, Git Commits
- `docs/postman-conventions.md` — Manual + Automated collections
- `docs/testing.md` — JaCoCo y estrategia de cobertura
- `docs/components.md` — Diagrama C4 Nivel 3: capas por módulo, componentes futuros e interacciones entre módulos
- `ENDPOINTS.md` — Listado completo de endpoints

---

## Planning Approval Protocol

**MANDATORY CHECKPOINT** — Antes de implementar CUALQUIER feature, obtener aprobación explícita de un plan que cubra:

1. **Resumen Ejecutivo:** Qué se construirá, qué problema resuelve, quién lo usará
2. **Lógica de Implementación:** Flujo Controller→Service→Repository, validaciones, mappers, side effects (RabbitMQ/WebSocket)
3. **Pruebas Propuestas:** Tests unitarios (ServiceTest, ControllerTest, MapperTest) y Postman (collection, test cases)
4. **DTOs Structure:** Todos los DTOs nuevos/modificados con campos completos y tipos
5. **Controller Access Rules:** `@PreAuthorize` por endpoint + ownership validation si aplica
6. **Functional Clarifications:** Assumptions de negocio que necesitan confirmación
7. **Scope Confirmation:** Qué INCLUYE y qué EXCLUYE la implementación

**Format:** Presentar las 7 secciones, esperar `'approved'` antes de implementar.

---

## Commands

### Backend (desde `backend/`)
```bash
./mvnw spring-boot:run                   # requires PostgreSQL + RabbitMQ
./mvnw test                              # all unit tests
./mvnw clean test jacoco:report          # tests + coverage report
./mvnw clean package                     # build JAR
./mvnw clean compile                     # quick compile check
./mvnw flyway:clean flyway:migrate       # reset DB (dev only)
```

Windows con Maven global:
```bash
"C:/Program Files/Apache/Maven/bin/mvn" -f backend/pom.xml clean compile -q
```

### Docker (desde raíz del proyecto)
```bash
docker compose up --build                # dev (uses override)
docker compose logs -f api               # stream backend logs
docker compose down -v                   # stop + remove volumes
```

`.env.prod.example` → `.env.prod` para producción. Dev usa defaults de `docker-compose.override.yml`.

---

## Architecture

Modular Spring Boot. Cada módulo: `controller → service → repository → entity + DTOs + mapper` bajo `modules/`.

| Módulo | Responsabilidad |
|---|---|
| `auth` | JWT issue/refresh, sesiones (`Sesion`) |
| `usuarios` | `Cliente` (puntos), `Empleado`, `UsuarioRol`, canje |
| `reservas` | Reservas, zonas, decoraciones, bloques, WhatsApp |
| `produccion` | Productos del menú, categorías, menú especial |
| `mesas_comandas` | `Mesa`, `Comanda`, `Visita`, notificaciones |
| `pagos_caja` | `Venta`, `Abono` |
| `inventario` | Insumos, recetas, modificadores, movimientos |
| `notificaciones` | Alertas en tiempo real (RabbitMQ + WebSocket) |
| `reportes` | Analítica (placeholder) |

**Cross-cutting** (`shared/`): SecurityConfig, RabbitMQConfig, WebSocketConfig, exception handler, `ApiResponse<T>`.

**Roles:** `CLIENTE`, `MESERO`, `CAJERO`, `COCINERO`, `BARTENDER`, `ADMIN` (un usuario puede tener varios).

**Spring profiles:** `dev` (validate, SQL logging, Swagger en `/swagger-ui.html`) | `prod` (Flyway only, Swagger off).

---

## Domain Model

Todas las entidades extienden `AuditableEntity` (createdAt, updatedAt). Schema: `restaurante`.

| Entidad | Notas |
|--------|-------|
| `Usuario` | `usuarioEmail` (unique), `usuarioPassword` (BCrypt) |
| `Sesion` | `sesionToken`, `sesionRefreshToken`, `sesionActiva` |
| `UsuarioRol` | PK compuesta (usuarioId, rolNombre) |
| `Cliente` | `clientePuntos` (canjeable), `clientePuntosAcumulados` (lifetime) |
| `Empleado` | PK = FK a `Usuario`. Email vía `empleado.getUsuario().getUsuarioEmail()` |
| `Zona` | `zonaCapacidadPersonas`; M:N con `Decoracion` |
| `Decoracion` | `decoracionCostoAdicional` (NULL=free, >0=paid min 1.00) |
| `Reserva` | States: `PENDIENTE→CONFIRMADA→ATENDIDA\|CANCELADA\|DEVUELTA\|INASISTENCIA`; Types: `BASICA\|ESPECIAL` |
| `Abono` | Types: `ANTICIPO\|DEVOLUCION` |
| `Visita` | `visitaFechaHoraFin` NULL = activa |
| `Mesa` | States: `ESPERA→EN_PREPARACION→ATENDIDA→CERRADA`; PK = `visitaId` |
| `Comanda` | Estaciones: `COCINA\|BARRA`; States: `PRE_RESERVA→PENDIENTE→EN_PREPARACION→LISTO→COMPLETADO` |
| `Producto` | Types: `VENTA_DIRECTA\|PREPARACION`; Categories: `PLATO\|BEBIDA\|OTRO` |
| `Venta` | PK=FK a `Visita`; cierra visita; +1 punto lealtad |
| `Notificacion` | Types: `ATENCION\|PLATOS_LISTOS\|BEBIDAS_LISTAS\|CAMBIO`; States: `ACTIVA\|ATENDIDA` |

---

## Critical Rules

### Database — Flyway
**⚠️ NEVER crear migraciones más allá de V5.** Schema → modificar `V1`. Base seed → `V2`. Dev data → `V3`. `V4` y `V5` son seeds de tests de integración.

Aplicar cambios:
```bash
docker compose down -v && docker compose up --build
```

### Security
- Stateless JWT (HMAC-SHA256, access 30 min, refresh 7 días)
- BCrypt strength 10
- `JwtAuthenticationFilter` valida firma, expiración y `sesionActiva=true`
- CORS: `localhost:4200` (Angular), `localhost:80` (Docker)
- Public endpoints: `/api/auth/*`, `/swagger-ui/**`, `/v3/api-docs/**`, `/actuator/health`, `/ws/**`
- Ownership: CLIENTE solo puede acceder a sus propios recursos

### Postman password
- Manual: `Al.Toro2026!` (hardcoded)
- Automated: variable `passwordValida`

### RabbitMQ
Exchange: `altoro.topic` (durable, topic). Routing keys: `comanda.nueva`, `impresion.ticket`, `notificacion.email`, `notificacion.ws`.

### WebSocket — tópicos existentes
`/topic/mesas`, `/topic/mesas/asistencia`, `/topic/visita/{id}/orden`, `/topic/visita/{id}/cuenta`, `/topic/visita/{id}/asistencia`, `/topic/reservas/cambios`, `/topic/produccion/{cocina|barra}`. **NO crear duplicados.** Detalle del patrón en `docs/coding-patterns.md`.

El tópico `/topic/produccion/{cocina|barra}` transporta el contrato unificado `ComandaProduccionEventoWsMessage(tipo, estacion, comandaId, resumen, nuevoEstado)` con `tipo ∈ {CREADA, ACTUALIZADA, ELIMINADA, COMPLETADA}`. Sustituye al legado `/topic/comandas/completado`. El campo `resumen` viaja solo en `CREADA`; `nuevoEstado` viaja solo en `ACTUALIZADA` (transiciones `PENDIENTE→EN_PREPARACION` y `EN_PREPARACION→LISTO`).

### ErrorCode rápido
`ENT-001` (404 not found), `AUTH-001` (401), `AUTH-002` (403), `NEG-001` (400/409 regla violada), `NEG-002` (409 estado inválido), `VAL-001` (400 validación). Tabla completa en `docs/coding-patterns.md`.

---

## Business Rules

- **Horario reservas:** 17:00–22:00
- **Cutoff modificación:** BASICA 13:00, ESPECIAL 23:00 (día previo)
- **WhatsApp flag:** true para BASICA→ESPECIAL o cambio de abono neto
- **Cancelación:** sin restricción horaria; `requiereWhatsApp=true` si requiere reembolso
- **Capacidad zona:** suma de `reservaNumeroPersonas` de reservas activas
- **Decoración:** una por zona por slot horario
- **PRE_RESERVA → PENDIENTE:** al iniciar visita
- **Puntos lealtad:** +1 por Venta cerrada; `clientePuntos`=canjeables, `clientePuntosAcumulados`=lifetime

---

## API

Base URL: `http://localhost:8080/api`. Lista completa en `ENDPOINTS.md`. Convenciones de naming en `docs/api-conventions.md`.

**30 endpoints:** Auth (5), Reservas (9), Clientes (3), Productos (2), Mesas (5), Visitas (4), Ventas (1), Notificaciones (1).

---

## Working Rules

- **NEVER modify the frontend** — backend-only
- **NO logging** en services (`@Slf4j`, `log.debug()` prohibidos)
- **Documentación obligatoria:** Javadoc (clases, métodos) + comentarios inline (`//`) en pasos no obvios
- **Mappers:** toda transformación entity→DTO en mapper dedicado, NO builders inline en services
- **Ordenamiento items:** SIEMPRE por categoría (PLATO→BEBIDA→OTRO) usando comparador estático, ANTES de transformar/agrupar
- **Acceso multi-rol:** seguir patrón `VisitaController` — `@PreAuthorize("hasAnyRole(...)")` + parámetro opcional `emailCliente` + ownership solo para CLIENTE
- **Scope to the HU:** no features/helpers/abstractions no requeridas por HU actual
- **TDD obligatorio** — cobertura mínima en `docs/testing.md`
- **Postman tests al modificar endpoints:** 1 manual + colección automatizada con cobertura completa (no solo happy path)
- **Para WS:** ver patrón en `docs/coding-patterns.md` antes de añadir tópicos o llamadas

---

## Git Workflow

- **Ramas protegidas:** `main` (production), `develop` (integration)
- **Naming:** `PA-{jira}-{descripción-corta}` (ej. `PA-96-estado-asistencia`)
- **Workflow:** branch desde `develop` → implementar → rebase a `develop` → PR → merge. **Nunca commit directo a `main`/`develop`**.
- **Commit format:** `<tipo>(<módulo>): <descripción en español>`. Tipos: `feat`, `fix`, `refactor`, `test`, `docs`, `style`, `chore`. Detalle en `docs/coding-patterns.md`.
- **NO ejecutar commits automáticamente** — reportar mensaje al usuario.
