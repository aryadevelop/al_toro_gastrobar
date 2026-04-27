# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with the **backend** of this repository.

## Project Overview

Al Toro Gastrobar is a restaurant management system with role-based portals for waiters, cashiers, kitchen/bar production staff, admins, and customers. Backend: Spring Boot 3.5 + Java 21 + PostgreSQL 15 + RabbitMQ 3.13.

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

## Working Rules

- **Never modify the frontend.** All implementation work is backend-only unless explicitly instructed otherwise in the session.
- **Documentation is mandatory:** every implemented feature must update su Javadoc correspondiente (clases, métodos) y los comentarios en línea (`//`) en pasos no-obvios de servicios y métodos importantes (ver `VentaService.cerrarCuenta` y `VisitaEstadoService.obtenerEstadoVisitaActiva` como ejemplos canónicos).
- **Mappers:** Toda transformación entity→DTO debe implementarse en una clase mapper dedicada en `mapper/` dentro del módulo (`VisitaMapper`, `ReservaMapper`, `ProductoMapper`, `VisitaEstadoMapper`, etc.). Los servicios no deben construir DTOs con builders inline en streams; deben delegar al mapper. Si un servicio existente tiene esa lógica embebida, refactorizarla al añadir nuevas funcionalidades.
- **Ordenamiento de items de comanda:** Siempre que se implemente funcionalidad relacionada a mapeo o presentación de items de comanda (`ComandaItem`), se debe implementar ordenamiento por categoría de producto (PLATO → BEBIDA → OTRO) usando `Comparator.comparing(item -> item.getProducto().getProductoCategoria().ordinal())`. Esto aplica en todos los flujos: pre-orden de reservas, estado de visita activa, detalle de visita. El ordenamiento debe aplicarse **antes** de cualquier transformación o agrupación de items.
- **Acceso multi-rol:** Al diseñar endpoints que sirven tanto a `CLIENTE` como a empleados, seguir el patrón de `VisitaController`: `@PreAuthorize("hasAnyRole(...)")` + parámetro opcional `emailCliente` + validación de ownership solo cuando el solicitante es `CLIENTE`. Otros roles acceden sin restricción de propiedad.
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
