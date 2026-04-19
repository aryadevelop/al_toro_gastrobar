# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with the **backend** of this repository.

## Project Overview

Al Toro Gastrobar is a restaurant management system with role-based portals for waiters, cashiers, kitchen/bar production staff, admins, and customers. Backend: Spring Boot 3.5 + PostgreSQL 15 + RabbitMQ 3.13.

## Commands

### Backend — run from `backend/`
```bash
./mvnw spring-boot:run     # requires PostgreSQL + RabbitMQ running
./mvnw test                # run unit tests
./mvnw clean package       # build JAR
./mvnw clean compile       # compile only
./mvnw flyway:clean flyway:migrate   # reset DB to seed state (dev only)
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

## Architecture

### Backend (`backend/src/main/java/co/edu/unicauca/backend/`)
Modular Spring Boot app. Each module follows `controller → service → repository → entity` layering. All modules live under `modules/`:

| Module | Responsibility |
|---|---|
| `auth` | JWT issue/refresh, session tracking (`Sesion` table), `CustomUserDetailsService` |
| `usuarios` | `Cliente` (loyalty points), `Empleado`, `UsuarioRol` (roles), point redemption |
| `reservas` | Reservations, zones, decorations, availability blocks |
| `produccion` | Menu products (`Producto`), categories |
| `mesas_comandas` | Physical tables, orders (`Comanda`), line items, visit history |
| `pagos_caja` | Sales transactions (`Venta`), installment payments (`Abono`) |
| `inventario` | Supplies, recipes linking products to ingredients, modification options, inventory movement log |
| `notificaciones` | Real-time alerts via RabbitMQ + WebSocket |
| `reportes` | Analytics and reports |

**Cross-cutting:**
- `security/` inside `auth` module — `SecurityConfig`, JWT filter chain
- RabbitMQ handles async inter-module events (e.g., new order → kitchen notification)
- Flyway migrations: `V1__init_schema.sql` (schema), `V2__seed_data.sql`, `V3__dev_data.sql`
- All tables live in the `restaurante` PostgreSQL schema

**Spring profiles:**
- `dev` — `ddl-auto: update`, SQL logging enabled, Swagger UI accessible
- `prod` — `ddl-auto: validate`, errors silenced, ports not exposed externally

**Roles:** `CLIENTE`, `MESERO`, `CAJERO`, `COCINERO`, `BARTENDER`, `ADM` — a user can hold multiple roles via `UsuarioRol`.

### API Documentation
Swagger UI available at `http://localhost:8080/swagger-ui.html` (dev profile only).
Postman collections are in `backend/postman/`. The base template for creating new Postman tests is in `backend/postman/prompt`.

## Working Rules

- **Never modify the frontend.** All implementation work is backend-only unless explicitly instructed otherwise.
- **Documentation is mandatory:** every implemented feature must update its corresponding Javadoc (classes, methods) and inline comments so that documentation stays consistent with the implementation.
- **Scope to the HU:** do not implement features, helpers, or abstractions that are not required by the current user story, even if they seem useful for future stories, except in refactor issues.
- **Postman tests:** when adding or modifying endpoints, create the corresponding Postman tests using `backend/postman/prompt` as the base template.

## Postman — Convenciones y estructura

### Formato de colecciones
Las colecciones usan el formato YAML por carpetas del plugin **Postman for VS Code**. Cada endpoint es una carpeta con:
- `.resources/definition.yaml` — definición de la colección (hook de colección)
- `XX-NN Nombre – STATUS.request.yaml` — un archivo por caso de prueba

### Patrón de pruebas — login autónomo por request
**Todos los tests** (tanto de denegación como de camino feliz) incluyen un script `beforeRequest` que hace login autónomo vía `pm.sendRequest` al endpoint `/api/auth/login`. Esto garantiza que cada test puede ejecutarse en aislamiento sin depender del hook de colección.

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

Para tests que además necesitan seleccionar datos dinámicamente (probe de IDs seed), el login se encadena: el callback del login llama a la función probe pasando el token recién obtenido.

**Regla:** nunca usar `{{variableDeToken}}` en el `Authorization` header sin que el `beforeRequest` del mismo test haya seteado esa variable.

### Hardcoding de emails seed fijos
Cuando una URL contiene un email de seed fijo (e.g., `sinpuntos@altoro.com`), se escribe **hardcodeado** en la URL y en el script — no como `{{emailSinPuntos}}`. Motivo: las variables de entorno en la URL pueden no sustituirse antes de que Tomcat valide el request, causando 400 por caracteres inválidos (`{{}}`).

### Nomenclatura de tokens
| Rol | Variable de entorno |
|-----|-------------------|
| CLIENTE (principal) | `clienteToken` |
| CLIENTE sin puntos | `clienteSinPuntosToken` |
| MESERO | `meseroToken` |
| CAJERO | `cajeroToken` |
| COCINERO | `cocineroToken` |
| ADMIN | `adminToken` |

### Selección dinámica de cliente con puntos
Los tests que necesitan un cliente con `puntosActuales > 0` (PC-05, PC-06, PC-09) iteran el pool seed (IDs 11..22) usando `GET /api/clientes/{id}/puntos` hasta encontrar uno válido. Cada canje agota un cliente del pool; cuando el pool se vacía se requiere `./mvnw flyway:clean flyway:migrate`.

## Notas de implementación — HE-02 (Dashboard del cliente)

### Colecciones Postman generadas
Cada endpoint tiene su propia carpeta en `backend/postman/postman/collections/`:

| Carpeta | Endpoint |
|---------|----------|
| `reservas/Al Toro – GET -api-reservas-disponibilidad/` | `GET /api/reservas/disponibilidad` |
| `reservas/Al Toro – POST -api-reservas/` | `POST /api/reservas` |
| `reservas/Al Toro – GET -api-reservas-cliente-futuras/` | `GET /api/reservas/cliente/futuras` |
| `reservas/Al Toro – GET -api-reservas-cliente-canceladas/` | `GET /api/reservas/cliente/canceladas` |
| `reservas/Al Toro – GET -api-reservas-{id}-detalle/` | `GET /api/reservas/{id}/detalle` |
| `reservas/Al Toro – PUT -api-reservas-{reservaId}/` | `PUT /api/reservas/{reservaId}` |
| `mesas_comandas/Al Toro – GET -api-visitas-cliente-historial/` | `GET /api/visitas/cliente/historial` |
| `mesas_comandas/Al Toro – GET -api-visitas-cliente-{id}-detalle/` | `GET /api/visitas/cliente/{id}/detalle` |
| `usuarios/Al Toro – GET -api-clientes-me-puntos/` | `GET /api/clientes/me/puntos` |
| `usuarios/Al Toro – GET -api-clientes-{clienteId}-puntos/` | `GET /api/clientes/{clienteId}/puntos` |
| `usuarios/Al Toro – POST -api-clientes-{clienteId}-canje-puntos/` | `POST /api/clientes/{clienteId}/canje-puntos` |

### Decisiones de diseño relevantes
- **CA-07 aparece dos veces** en los requisitos originales con semántica distinta: "detalle de reserva futura" (`GET /api/reservas/{id}/detalle`) y "detalle de visita" (`GET /api/visitas/cliente/{id}/detalle`). Ambas se implementaron y prueban por separado.
- **CA-04 (actualización de puntos)** no se prueba con dependencia secuencial entre colecciones: el incremento ocurre en `VentaService` al cerrar cuenta (fuera del alcance de `/api/clientes`). Las colecciones de `/api/clientes` validan únicamente estructura, invariantes y reglas del endpoint propio.
- **`puntosAcumulados` nunca disminuye**, ni siquiera tras el canje. PC-05/PC-06 verifican explícitamente el invariante capturando el acumulado antes del canje y comparándolo con el de la respuesta.
- **Walk-ins vs visitas con reserva:** `visitaIdWalkIn` y `visitaIdConReserva` se capturan dinámicamente en VA-01. Si el cliente de prueba no tiene ambos tipos en seed data, deben setearse manualmente en el entorno.

### Cobertura de pruebas por colección
- `me/puntos` — PA-01..PA-09: matriz de autorización (401 + 4×403), 200 con/sin puntos, 403 ownership, 400 param faltante.
- `{clienteId}/puntos` — PB-01..PB-07: matriz (401 + 3×403), 200 CAJERO/ADMIN, 404.
- `{clienteId}/canje-puntos` — PC-01..PC-09: matriz (401 + 3×403), 200 CAJERO/ADMIN con invariante de acumulados, 422 saldo cero, 404 cliente, 404 empleado.

### Variables de entorno — `Al Toro – Local.environment.yaml`

| Variable | Valor / cómo obtener |
|----------|----------------------|
| `emailSinPuntos` | `sinpuntos@altoro.com` (fijo, seed V3) |
| `clienteIdSinPuntos` | `SELECT u.usuario_id FROM restaurante.usuario u WHERE u.usuario_email = 'sinpuntos@altoro.com'` |
| `visitaIdConReserva` | `SELECT visita_id FROM restaurante.visita WHERE reserva_id IS NOT NULL LIMIT 1` |
| `visitaIdWalkIn` | `SELECT visita_id FROM restaurante.visita WHERE reserva_id IS NULL LIMIT 1` |
| `reservaIdConPreOrden` | `SELECT r.reserva_id FROM restaurante.reserva r JOIN restaurante.comanda c ON c.reserva_id = r.reserva_id LIMIT 1` |
| `reservaIdCancelada` | `SELECT reserva_id FROM restaurante.reserva WHERE reserva_estado IN ('CANCELADA','DEVUELTA') LIMIT 1` |
| `decoracionConCostoId` | `SELECT decoracion_id FROM restaurante.decoracion WHERE decoracion_costo_adicional > 0 AND decoracion_estado = 'ACTIVO' LIMIT 1` |
| `decoracionConCostoId2` | Segunda decoración con costo para MR-13; misma query con `OFFSET 1 LIMIT 1` |
| `reservaIdOtroCliente` | `SELECT r.reserva_id FROM restaurante.reserva r JOIN restaurante.usuario u ON u.usuario_id = r.cliente_id WHERE u.usuario_email <> '<emailCliente>' AND r.reserva_estado IN ('PENDIENTE','CONFIRMADA') LIMIT 1` |
| `clienteIdConPuntos` | Ya no requerido para `/api/clientes` — los tests lo resuelven dinámicamente del pool 11..22 |

## Git Workflow
- **Protected branches:** `main` (production), `develop` (integration)
- **Branch naming:** `PA-{jira-number}-{short-description-with-dashes}` (e.g., `PA-66-reserva-orden`)
- **Commit convention:** `<type>(<scope>): <description>` — types: `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`
- **Workflow:** branch from `develop` → commit → rebase onto `develop` → PR → merge; never commit directly to `main` or `develop`
- See `CONTRIBUTING.md` for full details
