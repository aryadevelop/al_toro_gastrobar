# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with the **backend** of this repository.

## Project Overview

Al Toro Gastrobar is a restaurant management system with role-based portals for waiters, cashiers, kitchen/bar production staff, admins, and customers. Backend: Spring Boot 3.5 + Java 21 + PostgreSQL 15 + RabbitMQ 3.13.

---

 ## Planning Approval Protocol

  **MANDATORY CHECKPOINT** — Before implementing ANY feature, STOP and get explicit user
  approval for:

  ### 1. Resumen Ejecutivo
  Explain in simple, non-technical language WHAT will be built:

  Qué se va a construir:
  - Breve descripción del feature (1-2 oraciones)
  - Qué problema resuelve o qué funcionalidad añade
  - Quién lo va a usar (rol de usuario)

  ### 2. Lógica de Implementación
  Explain HOW it will be built technically:

  Cómo se implementará:
  - Flujo principal: Controller → Service → Repository (paso a paso)
  - Validaciones de negocio que se aplicarán
  - Interacciones con otros módulos (si aplica)
  - Transformaciones entity↔DTO (mappers a usar/crear)
  - Side effects: RabbitMQ messages, WebSocket broadcasts, etc.

  ### 3. Pruebas Propuestas
  List all tests that will be created or modified:

  **Tests unitarios/integración (JUnit):**
  - ServiceTest: [listar métodos a probar]
  - ControllerTest: [listar endpoints a probar]
  - MapperTest: [si se crea/modifica mapper]

  **Pruebas Postman:**
  - Collection: [nombre de colección]
  - Test cases: [listar casos - happy path, validaciones, access control]

  ### 4. DTOs Structure
  List all new/modified DTOs with complete field definitions:

  DTOs to create/modify:
  - FooRequest
  • campo1: String (description)
  • campo2: Long (description)
  • campo3: LocalDateTime (nullable: yes/no)
  - BarResponse
  • campo1: String
  • campo2: List

  ### 5. Controller Access Rules
  Specify `@PreAuthorize` annotation for each endpoint:

  Controller access:
  - POST /api/foo → @PreAuthorize("hasAnyRole('ADMIN', 'MESERO')")
  - GET /api/bar/{id} → @PreAuthorize("hasRole('CLIENTE')")
  └─ Ownership validation: CLIENTE can only access own resources

  ### 6. Functional Clarifications
  List assumptions about business logic that need confirmation:

  Assumptions to confirm:
  - When X happens, should the system automatically Y?
  - Should edge case Z return 404 or 200 with empty list?
  - Is field W required or optional in the request?

  ### 7. Scope Confirmation
  State what WILL and WILL NOT be implemented:

  Implementation scope:
  ✓ INCLUDES: Feature X, Y, Z
  ✗ EXCLUDES: Feature A (out of scope for this HU)

  ---

  **Approval format:**

  📋 Plan Approval Required

  [Present the 7 sections above]

  Type 'approved' to proceed with implementation, or suggest changes.

  **Rule:** Implementation CANNOT start until user types 'approved' or provides specific
  modifications.

  ---

## Commands

### Backend — run from `backend/`
```bash
./mvnw spring-boot:run                   # requires PostgreSQL + RabbitMQ running
./mvnw test                              # run all unit tests
./mvnw clean package                     # build JAR
./mvnw clean compile                     # compile only (fast check)
./mvnw flyway:clean flyway:migrate       # reset DB to seed state (dev only)
```

On Windows with Maven installed globally:
```bash
"C:/Program Files/Apache/Maven/bin/mvn" -f backend/pom.xml clean compile -q
```

### Docker — run from project root
```bash
docker compose up --build                                              # dev (uses override)
docker compose -f docker-compose.yml -f docker-compose.prod.yml \
  --env-file .env.prod up -d                                          # production
docker compose logs -f api                                            # stream backend logs
docker compose down -v                                                # stop + remove volumes
```

### Environment Setup
Copy `.env.prod.example` to `.env.prod` and fill in secrets for production. Dev runs with defaults from `docker-compose.override.yml` (JWT secret, Mailtrap, CORS for localhost:4200).

---

## Architecture

### Backend (`backend/src/main/java/co/edu/unicauca/backend/`)
Modular Spring Boot app. Each module follows `controller → service → repository → entity + DTOs + mapper` layering. All domain modules live under `modules/`:

| Module | Responsibility |
|---|---|
| `auth` | JWT issue/refresh, session tracking (`Sesion` table), `CustomUserDetailsService` |
| `usuarios` | `Cliente` (loyalty points), `Empleado`, `UsuarioRol` (roles), point redemption |
| `reservas` | Reservations, zones, decorations, availability blocks, WhatsApp notifications |
| `produccion` | Menu products (`Producto`), categories (`CategoriaCarta`), special menus |
| `mesas_comandas` | Physical tables (`Mesa`), orders (`Comanda`), line items, visit history (`Visita`), notifications |
| `pagos_caja` | Sales transactions (`Venta`), installment payments (`Abono`) |
| `inventario` | Supplies (`Insumo`), recipes linking products to ingredients, modification options (`OpcionModificacion`), inventory movement log |
| `notificaciones` | Real-time alerts via RabbitMQ + WebSocket |
| `reportes` | Analytics and reports (placeholder — not yet implemented) |

**Cross-cutting (`shared/`):**
- `SecurityConfig`, `RabbitMQConfig`, `WebSocketConfig`
- Global exception handler, shared enums, `ApiResponse<T>` wrapper

**Spring profiles:**
- `dev` — `ddl-auto: validate`, SQL logging enabled, Swagger UI accessible at `http://localhost:8080/swagger-ui.html`
- `prod` — `ddl-auto: none` (Flyway only), errors silenced, Swagger disabled

**Roles (via `UsuarioRol`):** `CLIENTE`, `MESERO`, `CAJERO`, `COCINERO`, `BARTENDER`, `ADMIN` — a user can hold multiple roles.

---

## Domain Model

All entities extend `AuditableEntity` (createdAt, updatedAt). All tables live in the `restaurante` PostgreSQL schema.

| Entity | Table | Key Fields / Notes |
|--------|-------|-------------------|
| `Usuario` | `usuario` | `usuarioEmail` (unique), `usuarioPassword` (BCrypt) |
| `Sesion` | `sesion` | `sesionToken`, `sesionRefreshToken` (unique), `sesionActiva` bool |
| `UsuarioRol` | `usuario_rol` | Composite PK (usuarioId, rolNombre) |
| `Cliente` | `cliente` | PK = FK to usuario; `clientePuntos` (redeemable balance), `clientePuntosAcumulados` (lifetime, never decreases) |
| `Empleado` | `empleado` | PK = FK to usuario |
| `Zona` | `zona` | `zonaCapacidadPersonas`; M:N with `Decoracion` via `DecoracionZona` |
| `Decoracion` | `decoracion` | `decoracionCostoAdicional` (NULL = free, >0 = paid, min=1.00) |
| `BloqueDisponibilidad` | `bloque_disponibilidad` | Blocks time slots for reservations; linked to admin `Empleado` |
| `Reserva` | `reserva` | States: `PENDIENTE→CONFIRMADA→ATENDIDA\|CANCELADA\|DEVUELTA\|INASISTENCIA`; Types: `BASICA\|ESPECIAL` |
| `Abono` | `abono` | Types: `ANTICIPO` or `DEVOLUCION`; linked to Reserva and Cajero |
| `Visita` | `visita` | `visitaFechaHoraFin` NULL = active visit |
| `Mesa` | `mesa` | States: `ESPERA→EN_PREPARACION→ATENDIDA→CERRADA` |
| `Comanda` | `comanda` | Estaciones: `COCINA\|BARRA`; States: `PRE_RESERVA→PENDIENTE→EN_PREPARACION→LISTO→COMPLETADO` |
| `ComandaItem` | `comanda_item` | Item within a comanda; can have `ComandaMenuModificacion` |
| `Producto` | `producto` | Types: `VENTA_DIRECTA\|PREPARACION`; Categories: `PLATO\|BEBIDA\|OTRO`; `menuEspecial` bool |
| `CategoriaCarta` | `categoriacarta` | `categoriaNombre` (unique), `orden` for display sorting |
| `Insumo` | `insumo` | Types: `MATERIA_PRIMA\|SEMIELABORADO`; linked to `Producto` via `Receta` |
| `OpcionModificacion` | `opcion_modificacion` | `tipoComponente` enum: `ARROZ, PROTEINA, SALSA, SALSA_PROTEINA_1, SALSA_PROTEINA_2, ACOMPAÑAMIENTO, BEBIDA, ENSALADA, OTRO` |
| `Venta` | `venta` | PK = FK to Visita; closes a visit; triggers +1 loyalty point for customer |
| `CanjePuntos` | `canje_puntos` | Audit record; resets `clientePuntos` to 0 (accumulated never changes) |
| `Notificacion` | `notificacion` | Types: `ATENCION\|PLATOS_LISTOS\|BEBIDAS_LISTAS\|CAMBIO`; States: `ACTIVA\|ATENDIDA` |

---

## API Endpoints

Base URL: `http://localhost:8080/api`

### Auth (`/api/auth`)
| Method | Path | Access | Description |
|--------|------|--------|-------------|
| POST | `/login` | Public | Returns accessToken + refreshToken; supports `forceSessionOverride` |
| POST | `/register` | Public | Creates Usuario + Cliente (role CLIENTE) |
| POST | `/refresh` | Public | Rotates token pair |
| GET | `/me` | Authenticated | Current user profile with roles |
| POST | `/logout` | Authenticated | Invalidates all active sessions |

### Reservas (`/api/reservas`)
| Method | Path | Access | Description |
|--------|------|--------|-------------|
| GET | `/disponibilidad?fechaHora=` | CLIENTE | Available zones & decorations (17:00–22:00 only) |
| POST | `/` | CLIENTE | Create reservation + optional pre-order Comanda (PRE_RESERVA state) |
| PUT | `/{reservaId}` | CLIENTE | Modify future reservation (cutoff: 13:00 BASICA / 23:00 ESPECIAL) |
| PATCH | `/{reservaId}/cancelar` | CLIENTE | Cancel; returns `requiereWhatsApp` flag if refund needed |
| GET | `/cliente/futuras?emailCliente=` | CLIENTE | PENDIENTE/CONFIRMADA reservations ordered ASC |
| GET | `/cliente/canceladas-devueltas?emailCliente=` | CLIENTE | Cancelled/returned history |
| GET | `/{reservaId}/detalle` | CLIENTE/ADMIN | Full detail with pre-order items and payments |
| GET | `/mesero/consulta?fecha=&identificador=` | MESERO/ADMIN | Lista reservas activas del día (o fecha especificada); busca por ID si se proporciona identificador |
| GET | `/mesero/{reservaId}/detalle` | MESERO/ADMIN | Detalle completo de una reserva para meseros (incluye teléfono cliente y modificaciones de pre-orden) |

### Clientes / Puntos (`/api/clientes`)
| Method | Path | Access | Description |
|--------|------|--------|-------------|
| GET | `/me/puntos?emailCliente=` | CLIENTE | Own redeemable + accumulated points |
| GET | `/{clienteId}/puntos` | CAJERO/ADMIN | Any client's points |
| POST | `/{clienteId}/canje-puntos?emailEmpleado=` | CAJERO | Redeem all points (resets balance to 0, accumulated unchanged) |

### Productos (`/api/productos`)
| Method | Path | Access | Description |
|--------|------|--------|-------------|
| GET | `/carta` | Authenticated | Menu grouped by category (non-special items) |
| GET | `/menu-especial` | Authenticated | Special menus with modification option groups |

### Visitas (`/api/visitas`)
| Method | Path | Access | Description |
|--------|------|--------|-------------|
| GET | `/cliente/historial?emailCliente=` | CLIENTE | Past visits sorted DESC by date |
| GET | `/cliente/{visitaId}/detalle` | CLIENTE | Full visit detail: comandas, items, sale |
| GET | `/activa` | CLIENTE / MESERO / CAJERO / ADMIN | Estado de la visita activa: ítems, total, asistencia. CLIENTE usa token; otros roles requieren `?emailCliente=` |
| POST | `/{visitaId}/asistencia` | CLIENTE | Solicita asistencia de mesero; 409 si ya hay solicitud activa para la mesa |

### Ventas (`/api/ventas`)
| Method | Path | Access | Description |
|--------|------|--------|-------------|
| POST | `/` | CAJERO | Close visit account; registers Venta; awards +1 point to client; publishes `CuentaCerradaWsMessage` con `puntosActuales` |

### Notificaciones (`/api/notificaciones`)
| Method | Path | Access | Description |
|--------|------|--------|-------------|
| PATCH | `/{notificacionId}/atender` | MESERO / ADMIN | Marca solicitud de asistencia como atendida; publica WS al cliente |

---

## Security

- **Stateless JWT** — HMAC-SHA256 via JJWT 0.12.5; no HTTP sessions
- **Access token:** 30 min (`jwt.expiration=1800000`)
- **Refresh token:** 7 days (`jwt.refresh-expiration=604800000`)
- **Filter chain:** `JwtAuthenticationFilter` validates signature, expiration, and that `sesionActiva=true` in DB before setting security context
- **BCrypt** strength 10 for passwords
- **CORS** allowed origins: `localhost:4200` (Angular dev), `localhost:80` (Docker prod)
- **Public endpoints:** `/api/auth/*`, `/swagger-ui/**`, `/v3/api-docs/**`, `/actuator/health`, `/ws/**`
- **Ownership validation:** CLIENTE role can only access own reservations, visits, and points

---

## Business Rules

- **Reservation hours:** 17:00–22:00 only; outside hours returns no availability
- **Modification cutoff BASICA:** 13:00 the day before the reservation
- **Modification cutoff ESPECIAL:** 23:00 the day before the reservation
- **WhatsApp flag on modification:** always true for BASICA→ESPECIAL; otherwise true only when net abono amount changes
- **Cancellation:** no time restriction; `requiereWhatsApp=true` if there is a net abono to refund
- **Zone capacity:** computed at query time by summing `reservaNumeroPersonas` for active reservations on the same day; cannot exceed `zonaCapacidadPersonas`
- **Decoration exclusivity:** one decoration per zone per time slot
- **PRE_RESERVA comanda:** created when a reservation includes a pre-order; transitions to PENDIENTE when the visit starts
- **Loyalty points:** +1 per closed Venta; `clientePuntos` = redeemable balance (resets to 0 on redemption); `clientePuntosAcumulados` = lifetime total (never decreases)

---

## Database

**PostgreSQL 15** — container `altoro_postgres`, schema `restaurante`

**Flyway migrations** (`src/main/resources/db/migration/`):

| File | Content |
|------|---------|
| `V1__init_schema.sql` | Full schema: tables, constraints, sequences, 58 indexes |
| `V2__seed_data.sql` | Base seed: admin, employees, clients, zones, decorations, full menu |
| `V3__dev_data.sql` | Dev/test data: additional clients, reservations, visits, comandas, sales |
| `V4__mr_test_seed.sql` | Seed for MR (modificar reserva) integration tests |
| `V5__cr_test_seed.sql` | Seed for CR (cancelar reserva) integration tests |

Reset to seed: `./mvnw flyway:clean flyway:migrate` (dev only)

**Key indexes:** `idx_reserva_activas_hoy` (composite partial), `idx_visita_activas` (WHERE fin IS NULL), `idx_sesion_activa`, `idx_producto_activos`, `idx_comanda_pendientes`

---

## RabbitMQ

Exchange: `altoro.topic` (durable, topic type) — container `altoro_rabbitmq`

| Routing Key | Queue | Consumer |
|-------------|-------|----------|
| `comanda.nueva` | `q.comanda.produccion` | ProduccionService (kitchen/bar queue) |
| `impresion.ticket` | `q.impresion.ticket` | Node.js bridge (receipt printer) |
| `notificacion.email` | `q.notificacion.email` | NotificacionEmailService (Mailtrap in dev) |
| `notificacion.ws` | `q.notificacion.ws` | WebSocket publisher (real-time staff alerts) |

All queues are durable. Messages serialized with Jackson2 JSON.

---

## Tests

**Current state: 152 tests pass, 1 skipped** (`BackendApplicationTests` — requires PostgreSQL + RabbitMQ, marked `@Disabled`)

```bash
./mvnw test   # from backend/
```

Key test files:
- `JwtTokenProviderTest`, `JwtAuthenticationFilterTest`, `CustomUserDetailsServiceTest` — security layer
- `AuthServiceTest` — register, login, refresh, logout, session management
- `ReservaValidadorTest` (19 tests), `ReservaServiceModificarTest` (12 tests)
- `PuntosServiceTest` (8 tests), `MensajeWhatsAppBuilderTest` (5 tests)
- Entity validation tests for all major entities

---

## Postman — Conventions & Structure

### Collection format
Collections use the YAML format of the **Postman for VS Code** plugin. Each endpoint is a folder with:
- `.resources/definition.yaml` — collection-level hook definition
- `XX-NN Nombre – STATUS.request.yaml` — one file per test case

### Autonomous login pattern — critical rule
**All tests** (both denial and happy-path) include a `beforeRequest` script that performs autonomous login via `pm.sendRequest`. This ensures every test can run in isolation without depending on the collection hook.

```yaml
scripts:
  - type: beforeRequest
    code: |-
      pm.sendRequest({
        url: pm.environment.get('baseUrl') + '/api/auth/login',
        method: 'POST',
        header: { 'Content-Type': 'application/json' },
        body: {
          mode: 'raw',
          raw: JSON.stringify({
            email: pm.environment.get('emailCajero'),
            password: pm.environment.get('passwordValida'),
            forceSessionOverride: true
          })
        }
      }, function (err, res) {
        if (!err && res && res.code === 200) {
          pm.environment.set('cajeroToken', res.json().accessToken);
        } else {
          console.warn('XX-NN: login CAJERO falló', err, res && res.code);
        }
      });
    language: text/javascript
```

For tests that also need to probe dynamic IDs (e.g., finding a client with points), the login is chained: the login callback calls the probe function passing the fresh token.

**Rule:** never use `{{tokenVariable}}` in the `Authorization` header without the same test's `beforeRequest` having set it.

### Hardcoded seed emails in URLs
When a URL contains a fixed seed email (e.g., `sinpuntos@altoro.com`), write it **hardcoded** in the URL and script — not as `{{emailSinPuntos}}`. Reason: environment variable substitution in URLs can fail before Tomcat validates the request, causing 400 errors due to `{{}}` characters.

### Token variable naming
| Role | Environment variable |
|------|---------------------|
| CLIENTE (main) | `clienteToken` |
| CLIENTE sin puntos | `clienteSinPuntosToken` |
| MESERO | `meseroToken` |
| CAJERO | `cajeroToken` |
| COCINERO | `cocineroToken` |
| ADMIN | `adminToken` |

### Dynamic client selection for points tests
Tests requiring a client with `puntosActuales > 0` (PC-05, PC-06, PC-09) iterate the seed pool (IDs 11..22) via `GET /api/clientes/{id}/puntos` until a valid one is found. Each redemption exhausts one client from the pool; when empty, run `./mvnw flyway:clean flyway:migrate`.

### Running Postman collections

**Prerequisite:** Clean test state by running the cleanup script before executing tests:
```bash
psql -U postgres -d altoro_db -f postman/cleanup-notificaciones.sql
```

This script marks all active `ATENCION` notifications as `ATENDIDA`, ensuring tests start from a clean state.

### Postman collections by module
Collections live in `backend/postman/postman/collections/`:

| Folder | Endpoint |
|--------|----------|
| `reservas/Al Toro – GET -api-reservas-disponibilidad/` | `GET /api/reservas/disponibilidad` |
| `reservas/Al Toro – POST -api-reservas/` | `POST /api/reservas` |
| `reservas/Al Toro – PUT -api-reservas-{reservaId}/` | `PUT /api/reservas/{reservaId}` |
| `reservas/Al Toro – GET -api-reservas-cliente-futuras/` | `GET /api/reservas/cliente/futuras` |
| `reservas/Al Toro – GET -api-reservas-cliente-canceladas-devueltas/` | `GET /api/reservas/cliente/canceladas-devueltas` |
| `reservas/Al Toro – GET -api-reservas-{id}-detalle/` | `GET /api/reservas/{reservaId}/detalle` |
| `mesas_comandas/Al Toro – GET -api-visitas-cliente-historial/` | `GET /api/visitas/cliente/historial` |
| `mesas_comandas/Al Toro – GET -api-visitas-cliente-{id}-detalle/` | `GET /api/visitas/cliente/{visitaId}/detalle` |
| `mesas_comandas/Al Toro – GET -api-visitas-activa/` | `GET /api/visitas/activa` (8 tests) |
| `mesas_comandas/Al Toro – POST -api-visitas-{visitaId}-asistencia/` | `POST /api/visitas/{visitaId}/asistencia` (6 tests) |
| `notificaciones/Al Toro – PATCH -api-notificaciones-{notificacionId}-atender/` | `PATCH /api/notificaciones/{notificacionId}/atender` (5 tests) |
| `usuarios/Al Toro – GET -api-clientes-me-puntos/` | `GET /api/clientes/me/puntos` |
| `usuarios/Al Toro – GET -api-clientes-{clienteId}-puntos/` | `GET /api/clientes/{clienteId}/puntos` |
| `usuarios/Al Toro – POST -api-clientes-{clienteId}-canje-puntos/` | `POST /api/clientes/{clienteId}/canje-puntos` |

### Environment variables — `Al Toro – Local.environment.yaml`

| Variable | Value / how to obtain |
|----------|-----------------------|
| `baseUrl` | `http://localhost:8080` |
| `passwordValida` | Password used for all seed users |
| `emailSinPuntos` | `sinpuntos@altoro.com` (fixed, seed V3) |
| `clienteIdSinPuntos` | `SELECT u.usuario_id FROM restaurante.usuario u WHERE u.usuario_email = 'sinpuntos@altoro.com'` |
| `visitaIdConReserva` | `SELECT visita_id FROM restaurante.visita WHERE reserva_id IS NOT NULL LIMIT 1` |
| `visitaIdWalkIn` | `SELECT visita_id FROM restaurante.visita WHERE reserva_id IS NULL LIMIT 1` |
| `reservaIdConPreOrden` | `SELECT r.reserva_id FROM restaurante.reserva r JOIN restaurante.comanda c ON c.reserva_id = r.reserva_id LIMIT 1` |
| `reservaIdCancelada` | `SELECT reserva_id FROM restaurante.reserva WHERE reserva_estado IN ('CANCELADA','DEVUELTA') LIMIT 1` |
| `decoracionConCostoId` | `SELECT decoracion_id FROM restaurante.decoracion WHERE decoracion_costo_adicional > 0 AND decoracion_estado = 'ACTIVO' LIMIT 1` |
| `decoracionConCostoId2` | Same query with `OFFSET 1 LIMIT 1` |
| `reservaIdOtroCliente` | `SELECT r.reserva_id FROM restaurante.reserva r JOIN restaurante.usuario u ON u.usuario_id = r.cliente_id WHERE u.usuario_email <> '<emailCliente>' AND r.reserva_estado IN ('PENDIENTE','CONFIRMADA') LIMIT 1` |

### Reglas críticas para pruebas Postman

#### 1. Variables de ambiente en URLs dinámicas

**Problema:** Variables como `{{reservaId}}` pueden estar vacías al momento de construir la URL, causando URLs malformadas (`//`) que Spring Security rechaza con `RequestRejectedException: The request was rejected because the URL contained a potentially malicious String "//"`.

**Solución:** Configurar variables temporales en `beforeRequest`:

```yaml
url: "{{baseUrl}}/api/reservas/mesero/{{tmpReservaId}}/detalle"
scripts:
  - type: beforeRequest
    code: |-
      // Leer del ambiente con fallback a valor seed conocido
      const reservaId = pm.environment.get('reservaIdConPreOrden') || '10';
      pm.environment.set('tmpReservaId', reservaId);
      
      // Continuar con login autónomo...
      pm.sendRequest({ /* login */ });
  
  - type: afterResponse
    code: |-
      // Tests aquí...
      
      // IMPORTANTE: Limpiar variables temporales
      pm.environment.unset('tmpReservaId');
```

**Regla:** Nunca usar `{{variableAmbiente}}` directamente en la URL si el valor puede estar vacío. Siempre configurar una variable temporal prefijada con `tmp` en el `beforeRequest`.

#### 2. Crear datos dinámicos para pruebas

**Problema:** Crear reservas para "hoy" falla si la hora ya pasó (`fechaHoraLlegada` debe ser futura → error `VAL-001: La fecha y hora de llegada deben ser en el futuro`).

**Solución:** Usar fechas relativas que garanticen validez:

```javascript
// Para reservas futuras: SIEMPRE usar mañana, nunca "hoy"
const pad = n => String(n).padStart(2, '0');
const d = new Date();
d.setDate(d.getDate() + 1);  // Mañana
d.setHours(19, 0, 0, 0);
const fechaHora = `${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())}T19:00:00`;
const fechaConsulta = `${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())}`;
pm.environment.set('tmpFechaConsulta', fechaConsulta);
```

**Regla:** 
- Reservas futuras: `d.setDate(d.getDate() + 1)` (mañana)
- Fechas pasadas/historial: `d.setDate(d.getDate() - 30)` (hace 30 días)
- Nunca usar fechas fijas como `2026-12-25` en scripts dinámicos

#### 3. Patrón completo: autonomous login + dynamic data + cleanup

Ejemplo completo para una prueba que crea una reserva, la consulta, y luego limpia:

```javascript
// beforeRequest
const pad = n => String(n).padStart(2, '0');
const d = new Date();
d.setDate(d.getDate() + 1);  // Mañana
const fechaHora = `${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())}T19:00:00`;
pm.environment.set('tmpFechaConsulta', `${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())}`);

// 1. Login cliente
pm.sendRequest({
  url: pm.environment.get('baseUrl') + '/api/auth/login',
  method: 'POST',
  header: { 'Content-Type': 'application/json' },
  body: { mode: 'raw', raw: JSON.stringify({
    email: pm.environment.get('emailCliente'),
    password: pm.environment.get('passwordValida'),
    forceSessionOverride: true
  })}
}, function (err, res) {
  const clienteToken = res.json().accessToken;
  
  // 2. Crear reserva
  pm.sendRequest({
    url: pm.environment.get('baseUrl') + '/api/reservas',
    method: 'POST',
    header: { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + clienteToken },
    body: { mode: 'raw', raw: JSON.stringify({
      emailCliente: pm.environment.get('emailCliente'),
      fechaHoraLlegada: fechaHora,
      numeroPersonas: 2,
      zonaId: null, decoracionId: null, notas: null, preOrden: null
    })}
  }, function (e2, r2) {
    if (!e2 && r2 && r2.code === 201) {
      pm.environment.set('tmpReservaId', r2.json().data.reservaId);
    }
    
    // 3. Login final (mesero/admin) para ejecutar la prueba
    pm.sendRequest({
      url: pm.environment.get('baseUrl') + '/api/auth/login',
      method: 'POST',
      header: { 'Content-Type': 'application/json' },
      body: { mode: 'raw', raw: JSON.stringify({
        email: pm.environment.get('emailMesero'),
        password: pm.environment.get('passwordValida'),
        forceSessionOverride: true
      })}
    }, function (e3, r3) {
      pm.environment.set('meseroToken', r3.json().accessToken);
    });
  });
});

// afterResponse
pm.test('El sistema retorna HTTP 200', function () {
  pm.response.to.have.status(200);
});

// ... más tests ...

// Cleanup: cancelar reserva creada
const reservaId = pm.environment.get('tmpReservaId');
if (reservaId && reservaId !== 'undefined') {
  pm.sendRequest({
    url: pm.environment.get('baseUrl') + '/api/auth/login',
    method: 'POST',
    header: { 'Content-Type': 'application/json' },
    body: { mode: 'raw', raw: JSON.stringify({
      email: pm.environment.get('emailCliente'),
      password: pm.environment.get('passwordValida'),
      forceSessionOverride: true
    })}
  }, function (e, r) {
    const clienteToken = r.json().accessToken;
    pm.sendRequest({
      url: pm.environment.get('baseUrl') + '/api/reservas/' + reservaId + '/cancelar',
      method: 'PATCH',
      header: { 'Authorization': 'Bearer ' + clienteToken }
    }, function (e2, r2) {
      if (!e2 && r2 && r2.code !== 200) {
        console.warn('Cleanup: cancelar reserva falló', r2 && r2.code);
      }
    });
  });
}

pm.environment.unset('tmpReservaId');
pm.environment.unset('tmpFechaConsulta');
```

#### 4. Cuándo eliminar vs. modificar pruebas duplicadas

Cuando el backend cambia de retornar `200 OK + lista vacía` a `404 NOT_FOUND`:

- **Eliminar** pruebas que se vuelvan duplicados exactos  
  Ejemplo: `MC-04 identificador inexistente – 200 OK lista vacía` duplica `MC-12 identificador inexistente – 404 Not Found` → eliminar MC-04

- **Modificar** pruebas con propósito único  
  Ejemplo: `MC-01 día actual` crea datos dinámicos para garantizar 200 OK → modificar para crear reserva de mañana y consultar con parámetro `fecha`

#### 5. Códigos de error: enum Java ≠ JSON serializado

```java
// Backend (ErrorCode.java)
ENTITY_NOT_FOUND("ENT-001", "El recurso solicitado no existe.")

// JSON response
{ "success": false, "code": "ENT-001", "message": "..." }
```

**Regla:** Las pruebas Postman deben verificar el código serializado (`ENT-001`), **no** el nombre del enum (`ENTITY_NOT_FOUND`):

```javascript
pm.test('El código de error es ENT-001', function () {
  pm.expect(body.code).to.equal('ENT-001');  // ✓ Correcto
  // NUNCA: pm.expect(body.code).to.equal('ENTITY_NOT_FOUND');
});
```

**Mapeo de códigos comunes:**
- `ENTITY_NOT_FOUND` → `"ENT-001"`
- `VALIDATION_ERROR` → `"VAL-001"`
- `UNAUTHORIZED` → `"AUTH-001"`
- `ACCESS_DENIED` → `"AUTH-002"`
- `BUSINESS_RULE_VIOLATION` → `"BUS-001"`

---

## Coding Patterns & Conventions

Esta sección documenta los patrones de código establecidos en el proyecto. **IMPORTANTE:** Seguir estos patrones es obligatorio para mantener consistencia en el codebase.

### Services

**Patrón canónico:** `VisitaEstadoService.obtenerEstadoVisitaActiva()`

**Reglas obligatorias:**
- ❌ **NO usar logging** (`@Slf4j`, `log.debug()`, `log.info()`) — el proyecto no los usa en services
- ✅ **SÍ usar** Javadoc detallado con flujo paso a paso en comentarios de método
- ✅ **SÍ usar** `@Transactional(readOnly = true)` en operaciones de consulta
- ✅ **SÍ usar** `@RequiredArgsConstructor` para inyección de dependencias
- ✅ **SÍ usar** `Optional.orElseThrow()` con `ErrorCode` específico para entidades no encontradas

**Estructura de método estándar:**

```java
@Service
@RequiredArgsConstructor
public class MiService {
    
    private final MiRepository miRepository;
    private final MiMapper miMapper;
    
    /**
     * Descripción del método.
     * 
     * <p>Flujo:
     * <ol>
     *   <li>Paso 1: Buscar entidad principal</li>
     *   <li>Paso 2: Consultas auxiliares</li>
     *   <li>Paso 3: Delegar mapeo al mapper</li>
     * </ol>
     * 
     * @param param parámetro de entrada
     * @return DTO de respuesta
     * @throws BusinessException con HTTP 404 si no se encuentra la entidad
     */
    @Transactional(readOnly = true)
    public MiResponse obtenerMiDato(Long id) {
        
        // 1. Buscar entidad con orElseThrow
        MiEntidad entidad = miRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.ENTITY_NOT_FOUND,
                        "Mensaje descriptivo del error",
                        HttpStatus.NOT_FOUND));
        
        // 2. Consultas auxiliares
        Optional<RelatedEntity> relatedOpt = relatedRepository.findByXxx(id);
        
        // 3. Delegar mapeo al mapper (NO construir DTOs en el service)
        return miMapper.toResponse(entidad, relatedOpt);
    }
}
```

---

### Mappers

**Patrón canónico:** `VisitaEstadoMapper.mapearItemsOrdenados()`, `VisitaMapper.agruparItems()`

**Reglas obligatorias:**
- ✅ **SIEMPRE ordenar items por categoría ANTES de agrupar/mapear**
- ✅ **Reutilizar comparador estático** para ordenamiento de items de comanda
- ✅ **Enums → String en DTOs** usando `.name()` (NO pasar enum directamente)
- ✅ **Nombres de empleados:** usar `empleadoNombre` (campo completo) NO hay separación nombre/apellido
- ✅ **Nombres de usuarios:** usar `usuarioNombre` (campo único) NO existe `usuarioApellido`

**Comparador estándar para items de comanda:**

```java
@Component
@RequiredArgsConstructor
public class MiMapper {
    
    /**
     * Comparador para ordenar items por categoría de producto.
     * Orden: PLATO (0) → BEBIDA (1) → OTRO (2)
     */
    private static final Comparator<ComandaItem> COMPARATOR_POR_CATEGORIA =
            Comparator.comparing(item -> item.getProducto().getProductoCategoria().ordinal());
    
    /**
     * Mapea y ordena items de comanda por categoría.
     */
    public List<ItemResponse> mapearItemsOrdenados(List<ComandaItem> items) {
        return items.stream()
                .sorted(COMPARATOR_POR_CATEGORIA)  // ORDENAR PRIMERO
                .map(this::toItemResponse)          // LUEGO MAPEAR
                .collect(Collectors.toList());
    }
    
    /**
     * Mapea un item de comanda a DTO.
     */
    private ItemResponse toItemResponse(ComandaItem item) {
        return ItemResponse.builder()
                .nombreProducto(item.getProducto().getProductoNombre())
                .categoriaProducto(item.getProducto().getProductoCategoria().name())  // Enum → String
                .cantidad(item.getComandaItemCantidad())
                .build();
    }
}
```

**Agrupación de items (patrón establecido):**

```java
/**
 * Agrupa items de comanda por (nombreProducto + descripcion).
 * Suma las cantidades de items con mismo nombre y descripción.
 */
private List<ItemResponse> agruparItems(List<ComandaItem> items) {
    // Clave de agrupación: nombreProducto + "|" + descripcion (null-safe)
    Map<String, List<ComandaItem>> agrupados = items.stream()
        .sorted(COMPARATOR_POR_CATEGORIA)  // ORDENAR ANTES DE AGRUPAR
        .collect(Collectors.groupingBy(item ->
            item.getProducto().getProductoNombre() + "|" +
            (item.getComandaItemDescripcion() != null ? item.getComandaItemDescripcion() : "")
        ));
    
    return agrupados.values().stream()
        .map(grupo -> {
            ComandaItem primero = grupo.get(0);
            int cantidadTotal = grupo.stream()
                    .mapToInt(ComandaItem::getComandaItemCantidad)
                    .sum();
            
            return ItemResponse.builder()
                    .nombreProducto(primero.getProducto().getProductoNombre())
                    .descripcion(primero.getComandaItemDescripcion())
                    .cantidad(cantidadTotal)
                    .categoriaProducto(primero.getProducto().getProductoCategoria().name())
                    .build();
        })
        .collect(Collectors.toList());
}
```

---

### DTOs

**Reglas obligatorias:**
- ✅ **Enums → String** en DTOs de respuesta (usar `.name()` en mappers)
- ✅ **Inmutabilidad:** usar `@Getter` + `@Builder` + campos `final` en DTOs de respuesta
- ✅ **Javadoc:** documentar cada campo con propósito y valores posibles
- ✅ **Categoría de producto:** incluir como `String categoriaProducto` cuando se muestran items

**Ejemplo de DTO de respuesta:**

```java
/**
 * DTO para representar un item de comanda.
 */
@Getter
@Builder
public class ItemResponse {
    
    /** Nombre del producto */
    private final String nombreProducto;
    
    /** Categoría: "PLATO", "BEBIDA", "OTRO" */
    private final String categoriaProducto;
    
    /** Estado de la comanda: "PENDIENTE", "EN_PREPARACION", "LISTO", "COMPLETADO" */
    private final String estadoComanda;
    
    /** Cantidad de unidades */
    private final Integer cantidad;
}
```

---

### ErrorCode Usage

**Códigos disponibles en `ErrorCode.java`:**

| Código | Enum | HTTP Status | Uso |
|--------|------|-------------|-----|
| `ENT-001` | `ENTITY_NOT_FOUND` | 404 | Entidad no encontrada (mesa, visita, zona, etc.) |
| `ENT-002` | `ENTITY_ALREADY_EXISTS` | 409 | Entidad duplicada (email, identificador, etc.) |
| `AUTH-001` | `INVALID_CREDENTIALS` | 401 | Credenciales inválidas |
| `AUTH-002` | `ACCESS_DENIED` | 403 | Sin permisos para la acción |
| `NEG-001` | `BUSINESS_ERROR` | 400/409 | Regla de negocio violada (genérico) |
| `NEG-002` | `INVALID_STATE` | 409 | Estado inválido para la operación |
| `NEG-003` | `CAPACITY_EXCEEDED` | 400 | Capacidad máxima superada |
| `VAL-001` | `VALIDATION_ERROR` | 400 | Error de validación de entrada |
| `SRV-001` | `INTERNAL_ERROR` | 500 | Error interno del servidor |

**Ejemplo de uso:**

```java
// Entidad no encontrada
Mesa mesa = mesaRepository.findById(visitaId)
        .orElseThrow(() -> new BusinessException(
                ErrorCode.ENTITY_NOT_FOUND,
                "Mesa no encontrada",
                HttpStatus.NOT_FOUND));

// Regla de negocio violada
if (visita.getVisitaFechaHoraFin() == null) {
    throw new BusinessException(
            ErrorCode.INVALID_STATE,
            "La visita aún está activa",
            HttpStatus.CONFLICT);
}
```

---

### Controller Tests

**Patrón canónico:** `ClienteControllerTest`

**Reglas obligatorias:**
- ✅ Usar `@WebMvcTest(controllers = MiController.class)` para tests de controller
- ✅ Importar `TestSecurityConfig` personalizada con seguridad permisiva
- ✅ Usar `@MockitoBean` para services y dependencias de seguridad
- ✅ Usar `@Nested` + `@DisplayName` para agrupar tests por endpoint
- ✅ Nombrar tests descriptivamente: `condicion_resultadoEsperado()`

**Estructura estándar:**

```java
@WebMvcTest(controllers = MiController.class)
@Import(MiControllerTest.PermissiveSecurityConfig.class)
class MiControllerTest {
    
    static class PermissiveSecurityConfig {
        @Bean
        @Order(1)
        SecurityFilterChain testFilterChain(HttpSecurity http) throws Exception {
            return http
                    .securityMatcher("/**")
                    .csrf(AbstractHttpConfigurer::disable)
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                    .build();
        }
    }
    
    @Autowired MockMvc mockMvc;
    
    @MockitoBean MiService miService;
    @MockitoBean JwtTokenProvider jwtTokenProvider;
    @MockitoBean UserDetailsService userDetailsService;
    @MockitoBean SesionRepository sesionRepository;
    
    @Nested
    @DisplayName("GET /api/mi-endpoint")
    class ObtenerMiDato {
        
        @Test
        @WithMockUser(username = "mesero@altoro.com", roles = "MESERO")
        @DisplayName("Mesero con ID válido → 200 OK")
        void idValido_retorna200() throws Exception {
            // Arrange
            MiResponse response = MiResponse.builder().dato("valor").build();
            when(miService.obtenerMiDato(anyLong())).thenReturn(response);
            
            // Act & Assert
            mockMvc.perform(get("/api/mi-endpoint/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.dato").value("valor"));
            
            verify(miService).obtenerMiDato(1L);
        }
        
        @Test
        @WithMockUser(username = "mesero@altoro.com", roles = "MESERO")
        @DisplayName("ID inexistente → 404 Not Found")
        void idInexistente_retorna404() throws Exception {
            // Arrange
            when(miService.obtenerMiDato(999L))
                    .thenThrow(new BusinessException(
                            ErrorCode.ENTITY_NOT_FOUND,
                            "No encontrado",
                            HttpStatus.NOT_FOUND));
            
            // Act & Assert
            mockMvc.perform(get("/api/mi-endpoint/999"))
                    .andExpect(status().isNotFound());
        }
    }
}
```

---

### Postman Testing — DOS TIPOS

**⚠️ IMPORTANTE:** Hay DOS tipos de requests en Postman con patrones diferentes.

---

#### 1. Manual Testing (`manual-testing/`)

**Propósito:** Pruebas manuales rápidas sin validaciones automáticas.

**Convenciones:**
- ✅ **Solo `{{baseUrl}}`** como variable de entorno
- ✅ **Credenciales hardcoded** en `beforeRequest`
- ✅ **Password estándar:** `Al.Toro2026!`
- ✅ **Tokens temporales** con prefijo `tmp` (e.g., `tmpMeseroToken`)
- ✅ **Cleanup en `afterResponse`** (solo unset de tokens)
- ❌ **NO tests** en `afterResponse` (solo cleanup)
- ✅ **Formato:** `XX-YY Descripción – ROL.request.yaml`

**Numeración:**
- `00-XX`: Auth
- `10-XX`: Productos
- `20-XX`: Reservas (CLIENTE)
- `30-XX`: Reservas (MESERO)
- `40-XX`: Visitas
- `50-XX`: Puntos
- `60-XX`: Ventas
- `70-XX`: Notificaciones
- `80-XX`: Mesas

**Template:**

```yaml
name: 80-01 Obtener mapa mesas – MESERO
url: "{{baseUrl}}/api/mesas"
headers:
  Authorization: Bearer {{tmpMeseroToken}}
scripts:
  - type: beforeRequest
    code: |-
      pm.sendRequest({
        url: '{{baseUrl}}/api/auth/login',
        method: 'POST',
        header: { 'Content-Type': 'application/json' },
        body: {
          mode: 'raw',
          raw: JSON.stringify({
            email: 'mesero1@altoro.com',
            password: 'Al.Toro2026!',
            forceSessionOverride: true
          })
        }
      }, (err, res) => {
        if (!err && res && res.code === 200) {
          pm.environment.set('tmpMeseroToken', res.json().accessToken);
        }
      });
  - type: afterResponse
    code: pm.environment.unset('tmpMeseroToken');  // Solo cleanup, NO tests
```

---

#### 2. Automated Collections (`collections/`)

**Propósito:** Pruebas automatizadas con validaciones y flujos independientes.

**Convenciones:**
- ✅ **Variables de entorno del proyecto** (`emailMesero`, `passwordValida`, `baseUrl`)
- ✅ **Login autónomo** en `beforeRequest` usando variables de entorno
- ✅ **Limpieza de estado previo** en `beforeRequest` (datos seed, notificaciones activas, etc.)
- ✅ **Tests obligatorios** en `afterResponse` con `pm.test()`
- ✅ **Variables temporales** (e.g., `tmpNotificacionCreada`) SOLO si se necesitan para tests posteriores
- ❌ **NO cleanup en `afterResponse`** (cada test se prepara su propio estado en beforeRequest)
- ✅ **Formato:** `XX-YY Descripción – Código HTTP.request.yaml`
- ✅ **INDEPENDENCIA:** Cada test debe ser ejecutable solo, sin depender del orden

**Template:**

```yaml
$kind: http-request
name: MC-01 MESERO obtiene mapa todas zonas – 200 OK
description: |-
  **Criterio de Aceptación:** CA-01 — Mesero visualiza mapa completo.
  **Objetivo:** Verificar que se retornan todas las zonas con mesas activas.
  **Pre-condición:** `meseroToken` disponible. BD tiene zonas seed.
  **Resultado esperado:** 200 OK con array de zonas.
url: "{{baseUrl}}/api/mesas"
method: GET
headers:
  Authorization: Bearer {{meseroToken}}
scripts:
  - type: beforeRequest
    code: |-
      // Login autónomo usando variables de entorno del proyecto
      pm.sendRequest({
        url: pm.environment.get('baseUrl') + '/api/auth/login',
        method: 'POST',
        header: { 'Content-Type': 'application/json' },
        body: {
          mode: 'raw',
          raw: JSON.stringify({
            email: pm.environment.get('emailMesero'),
            password: pm.environment.get('passwordValida'),
            forceSessionOverride: true
          })
        }
      }, (err, res) => {
        if (!err && res && res.code === 200) {
          pm.environment.set('meseroToken', res.json().accessToken);
        } else {
          console.warn('MC-01: login MESERO falló', err, res && res.code);
        }
      });
    language: text/javascript
  - type: afterResponse
    code: |-
      pm.test('El sistema retorna HTTP 200', function () {
        pm.response.to.have.status(200);
      });
      
      pm.test('La respuesta contiene zonas', function () {
        const body = pm.response.json();
        pm.expect(body).to.have.property('data');
        pm.expect(body.data).to.have.property('zonas');
        pm.expect(body.data.zonas).to.be.an('array');
      });
      
      pm.test('Cada zona tiene estructura correcta', function () {
        const body = pm.response.json();
        if (body.data.zonas.length > 0) {
          const zona = body.data.zonas[0];
          pm.expect(zona).to.have.property('zonaId');
          pm.expect(zona).to.have.property('zonaNombre');
          pm.expect(zona).to.have.property('cantidadMesasActivas');
          pm.expect(zona).to.have.property('mesas');
        }
      });
      
      // NO cleanup aquí - cada test prepara su propio estado
    language: text/javascript
order: 1000
```

**Limpieza de estado previo (ejemplo OB-01):**

```javascript
// beforeRequest
pm.sendRequest({...login cliente...}, (err, res) => {
  const token = res.json().accessToken;
  pm.environment.set('clienteToken', token);
  
  // Verificar si hay notificación activa y limpiarla
  pm.sendRequest({
    url: pm.environment.get('baseUrl') + '/api/visitas/activa',
    method: 'GET',
    header: { 'Authorization': 'Bearer ' + token }
  }, (err2, res2) => {
    if (!err2 && res2 && res2.code === 200) {
      const estado = res2.json().data;
      if (estado.asistenciaSolicitada && estado.notificacionAsistenciaId) {
        // Limpiar con login de mesero
        pm.sendRequest({...login mesero...}, (err3, res3) => {
          pm.sendRequest({
            url: pm.environment.get('baseUrl') + '/api/notificaciones/' + estado.notificacionAsistenciaId + '/atender',
            method: 'PATCH',
            header: { 'Authorization': 'Bearer ' + res3.json().accessToken }
          }, ...);
        });
      }
    }
  });
});
```

**Guardar IDs para tests posteriores:**

```javascript
// afterResponse
pm.test('La respuesta contiene notificacionId', function () {
  const body = pm.response.json();
  pm.expect(body.data).to.have.property('notificacionId');
  // Guardar para tests posteriores
  pm.environment.set('tmpNotificacionCreada', body.data.notificacionId);
});
```

---

### Diferencias Clave

| Aspecto | Manual Testing | Automated Collections |
|---------|---------------|----------------------|
| **Credenciales** | Hardcoded (`mesero1@altoro.com`) | Variables de entorno (`emailMesero`) |
| **Password** | Hardcoded (`Al.Toro2026!`) | Variable (`passwordValida`) |
| **Tokens** | Temporales (`tmpMeseroToken`) | Del proyecto (`meseroToken`) |
| **afterResponse** | Solo cleanup de tokens | Tests + guardar IDs temporales |
| **Cleanup** | En `afterResponse` | En `beforeRequest` del siguiente test |
| **Independencia** | No requerida | OBLIGATORIA - cada test se auto-prepara |
| **Propósito** | Pruebas manuales rápidas | Suite automatizada de validación |

---

### Git Commits

**Reglas obligatorias:**
- ❌ **NO ejecutar commits automáticamente** — reportar mensaje en español para que el usuario lo ejecute
- ✅ **Formato:** `<tipo>(<módulo>): <descripción en español>`
- ✅ **Co-author obligatorio:** `Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>`

**Tipos de commit:**
- `feat`: Nueva funcionalidad
- `fix`: Corrección de bug
- `refactor`: Refactorización sin cambio funcional
- `test`: Añadir/modificar tests
- `docs`: Cambios en documentación
- `style`: Formato, espacios (sin cambio de lógica)
- `chore`: Tareas de mantenimiento

**Ejemplos:**

```bash
# ✅ CORRECTO (español, descriptivo)
git commit -m "feat(mesas): añadir endpoint GET /api/mesas para consultar mapa"

# ❌ INCORRECTO (inglés, genérico)
git commit -m "add new endpoint"
```

**Instrucción para el usuario:**

Cuando Claude Code proponga un commit, debe presentarlo así:

```
📝 Mensaje de commit propuesto (ejecutar manualmente):

git add <archivos>
git commit -m "feat(mesas): añadir endpoint GET /api/mesas"
```

---

### WebSocket Integration

**REGLA CRÍTICA:** Siempre que se implemente una funcionalidad que modifique el estado del sistema, **analizar si debe enviar un mensaje por WebSocket** para actualización en tiempo real.

**Patrón establecido:** REST + Publisher (NO usar `@MessageMapping` para operaciones con persistencia)

**Arquitectura:**
```
Cliente → REST endpoint → Service → Publisher → /topic/* → Suscriptores
```

**Cuándo enviar mensaje WS:**
- ✅ Crear/modificar/eliminar mesas
- ✅ Cambiar estado de mesa
- ✅ Crear/atender notificaciones
- ✅ Cerrar cuenta
- ✅ Actualizar items de comanda
- ✅ Cambios en reservas activas

**Cuándo NO enviar mensaje WS:**
- ❌ Consultas de solo lectura (GET)
- ❌ Operaciones que no afectan a otros usuarios

**Publisher Pattern:**

```java
@Service
@RequiredArgsConstructor
public class MiWsPublisher {
    
    private final SimpMessagingTemplate messagingTemplate;
    
    /**
     * Publica evento a un tópico específico.
     * 
     * @param id identificador del recurso
     * @param mensaje DTO del mensaje WebSocket
     */
    public void publicarEvento(Long id, MiWsMessage mensaje) {
        messagingTemplate.convertAndSend("/topic/mi-recurso/" + id, mensaje);
    }
}
```

**Inyección en Service:**

```java
@Service
@RequiredArgsConstructor
public class MiService {
    
    private final MiRepository miRepository;
    private final MiWsPublisher wsPublisher;  // ← SIEMPRE inyectar publisher
    
    @Transactional
    public MiResponse crearRecurso(MiRequest request) {
        // 1. Validar y persistir
        MiEntidad entidad = miRepository.save(...);
        
        // 2. Construir respuesta
        MiResponse response = miMapper.toResponse(entidad);
        
        // 3. Publicar evento WS DESPUÉS de persistir
        MiWsMessage wsMessage = MiWsMessage.builder()
                .id(entidad.getId())
                .tipoEvento(TipoEvento.CREAR)
                .timestamp(System.currentTimeMillis())
                .build();
        wsPublisher.publicarEvento(entidad.getId(), wsMessage);
        
        // 4. Retornar respuesta
        return response;
    }
}
```

**Tests de WebSocket:**

```java
@ExtendWith(MockitoExtension.class)
class MiServiceTest {
    
    @Mock MiRepository miRepository;
    @Mock MiWsPublisher wsPublisher;  // ← Mock del publisher
    @Mock MiMapper miMapper;
    
    @InjectMocks MiService miService;
    
    @Test
    @DisplayName("al crear recurso publica evento WebSocket")
    void alCrearRecurso_publicaEventoWs() {
        // Arrange
        when(miRepository.save(any())).thenReturn(entidad);
        when(miMapper.toResponse(any())).thenReturn(response);
        
        // Act
        miService.crearRecurso(request);
        
        // Assert - Verifica que se publicó WS
        ArgumentCaptor<MiWsMessage> captor = ArgumentCaptor.forClass(MiWsMessage.class);
        verify(wsPublisher).publicarEvento(eq(1L), captor.capture());
        
        // Verificar contenido del mensaje
        assertThat(captor.getValue().getId()).isEqualTo(1L);
        assertThat(captor.getValue().getTipoEvento()).isEqualTo(TipoEvento.CREAR);
        assertThat(captor.getValue().getTimestamp()).isNotNull();
    }
}
```

**Tópicos establecidos:**

| Tópico | Uso | Suscriptores |
|--------|-----|--------------|
| `/topic/visita/{visitaId}/orden` | Items de comanda actualizados | Cliente de esa visita |
| `/topic/visita/{visitaId}/cuenta` | Cuenta cerrada | Cliente de esa visita |
| `/topic/visita/{visitaId}/asistencia` | Asistencia atendida | Cliente de esa visita |
| `/topic/mesas/asistencia` | Nueva solicitud de asistencia | Todos los meseros |
| `/topic/mesas` | Cambios en el mapa de mesas | Todos los meseros |
| `/topic/reservas/cambios` | Cambios en reservas activas | Todos los meseros |

**Configuración (`WebSocketConfig.java`):**

```java
@Override
public void configureMessageBroker(MessageBrokerRegistry registry) {
    registry.enableSimpleBroker("/topic");
    registry.setApplicationDestinationPrefixes("/app");
}

@Override
public void registerStompEndpoints(StompEndpointRegistry registry) {
    registry.addEndpoint("/ws")
            .setAllowedOriginPatterns(allowedOrigins.toArray(String[]::new));
}
```

**Cuándo usar `@MessageMapping` vs REST + Publisher:**

| Escenario | Usar |
|-----------|------|
| Operación requiere persistencia en DB | REST + Publisher |
| Operación tiene validaciones de negocio | REST + Publisher |
| Operación requiere transacción | REST + Publisher |
| Chat en tiempo real (sin persistencia) | `@MessageMapping` |
| Eventos efímeros (no guardar en DB) | `@MessageMapping` |

**IMPORTANTE:** El proyecto usa **REST + Publisher** como estándar. NO introducir `@MessageMapping` sin justificación válida.

---

## Working Rules

- **Never modify the frontend.** All implementation work is backend-only unless explicitly instructed otherwise in the session.
- **Documentation is mandatory:** every implemented feature must update su Javadoc correspondiente (clases, métodos) y los comentarios en línea (`//`) en pasos no-obvios de servicios y métodos importantes (ver `VentaService.cerrarCuenta` y `VisitaEstadoService.obtenerEstadoVisitaActiva` como ejemplos canónicos).
- **Mappers:** Toda transformación entity→DTO debe implementarse en una clase mapper dedicada en `mapper/` dentro del módulo (`VisitaMapper`, `ReservaMapper`, `ProductoMapper`, `VisitaEstadoMapper`, etc.). Los servicios no deben construir DTOs con builders inline en streams; deben delegar al mapper. Si un servicio existente tiene esa lógica embebida, refactorizarla al añadir nuevas funcionalidades.
- **Ordenamiento de items de comanda:** Siempre que se implemente funcionalidad relacionada a mapeo o presentación de items de comanda (`ComandaItem`), se debe implementar ordenamiento por categoría de producto (PLATO → BEBIDA → OTRO) usando `Comparator.comparing(item -> item.getProducto().getProductoCategoria().ordinal())`. Esto aplica en todos los flujos: pre-orden de reservas, estado de visita activa, detalle de visita. El ordenamiento debe aplicarse **antes** de cualquier transformación o agrupación de items.
- **Acceso multi-rol:** Al diseñar endpoints que sirven tanto a `CLIENTE` como a empleados, seguir el patrón de `VisitaController`: `@PreAuthorize("hasAnyRole(...)")` + parámetro opcional `emailCliente` + validación de ownership solo cuando el solicitante es `CLIENTE`. Otros roles acceden sin restricción de propiedad.

- **Code Coverage (JaCoCo):** Mantener cobertura alta en todas las implementaciones nuevas. Objetivos mínimos por tipo:
  - **Services:** 90-95% — toda lógica de negocio debe estar cubierta, incluyendo branches (if/else), loops, y manejo de excepciones
  - **Controllers:** 85-90% — cubrir todos los endpoints con @WebMvcTest, incluyendo validaciones de seguridad (@PreAuthorize), ownership, y códigos de error
  - **Mappers:** 90-95% — probar todas las transformaciones entity→DTO y DTO→entity, validando cada campo
  - **Validators:** 95%+ — cubrir todas las reglas de validación custom y edge cases
  - **Repositories:** 70-80% — solo custom queries (@Query), no generar tests para métodos CRUD estándar
  - **DTOs/Entities:** No requieren tests unitarios si son POJOs sin lógica (solo getters/setters/constructores)
  - **Config classes:** No requieren tests (Spring beans, @Configuration)
  
  **Reglas de implementación:**
  1. **NUNCA implementar una feature sin sus tests** — seguir TDD: escribir test → implementar → refactorizar
  2. **Tests ANTES del commit** — ejecutar `./mvnw clean test jacoco:report` y verificar cobertura antes de cada commit
  3. **Priorizar calidad sobre cantidad** — un test que valida lógica crítica > 10 tests de getters/setters
  4. **Cubrir edge cases** — no solo happy path: validaciones, errores, casos límite, nulls, listas vacías
  5. **Branches coverage** — cada `if/else`, `switch`, `try/catch` debe tener tests para TODAS las ramas
  6. **Integration tests para flujos complejos** — usar @SpringBootTest cuando el flujo involucra múltiples capas o transacciones
  
  **Verificación de cobertura:**
  ```bash
  ./mvnw clean test jacoco:report
  # Ver: backend/target/site/jacoco/index.html
  # Buscar clases con coverage < umbral mínimo
  ```
  
  **Exclusiones válidas de cobertura:**
  - DTOs sin lógica (solo campos + getters/setters)
  - Entities sin métodos custom
  - Classes `*Application.java` (Spring Boot main)
  - Exception classes con solo constructores
  - Config classes (@Configuration, @Bean methods)
- **Scope to the HU:** do not implement features, helpers, or abstractions not required by the current user story, except in refactor issues.
- **Postman tests:** when adding or modifying endpoints, create corresponding Postman tests using `backend/postman/prompt` as the base template.
- **Manual testing requests:** For EVERY new endpoint implemented, create a corresponding request in the `manual-testing` collection (`backend/postman/postman/collections/manual-testing/Al Toro - Manual Testing/`). These requests MUST follow this pattern:
  - **Only use `{{baseUrl}}`** as environment variable (no other variables)
  - **Hardcoded credentials** in `beforeRequest` script for autonomous login (e.g., `cliente1@altoro.com`, `mesero1@altoro.com`, `cajero1@altoro.com`)
  - **Always use `forceSessionOverride: true`** in login
  - **Temporary token variables** prefixed with `tmp` (e.g., `{{tmpClienteToken}}`, `{{tmpMeseroToken}}`)
  - **Cleanup in `afterResponse`** script to unset all temporary variables
  - **Complete DTO examples** with ALL fields for POST/PUT requests (no null/missing fields unless explicitly optional)
  - **File naming:** `XX-YY Descripción – ROL.request.yaml` where XX is module number (00=auth, 10=productos, 20=reservas, 30=reservas-mesero, 40=visitas, 50=puntos, 60=ventas, 70=notificaciones)
  - Example template in `backend/postman/postman/collections/manual-testing/Al Toro - Manual Testing/00-01 Login CLIENTE.request.yaml`

---

## Git Workflow

- **Protected branches:** `main` (production), `develop` (integration)
- **Branch naming:** `PA-{jira-number}-{short-description-with-dashes}` (e.g., `PA-96-estado-asistencia-orden`)
- **Commit convention:** `<type>(<scope>): <description>` — types: `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`
- **Workflow:** branch from `develop` → implement → rebase onto `develop` → PR → merge to develop; never commit directly to `main` or `develop`
- See `CONTRIBUTING.md` for full details
