# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Al Toro Gastrobar is a full-stack restaurant management system with role-based portals for waiters, cashiers, kitchen/bar production staff, admins, and customers. It uses Angular 17 (frontend) + Spring Boot 3.5 (backend) + PostgreSQL 15 + RabbitMQ 3.13.

## Commands

### Frontend (Angular 17) — run from `frontend/`
```bash
npm install
npm start          # dev server at http://localhost:4200
npm run build      # production build → frontend/dist/
```

### Backend (Spring Boot) — run from `backend/`
```bash
./mvnw spring-boot:run     # requires PostgreSQL + RabbitMQ running
./mvnw test                # run unit tests
./mvnw clean package       # build JAR
./mvnw clean compile       # compile only
```

### Docker (Full Stack) — run from project root
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

### Frontend (`frontend/src/app/`)
Standalone Angular 17 components with no NgModules. Routing is in `app.routes.ts`.

```
core/           # Guards, interceptors (JWT), global services, models
features/
  admin/        # Dashboard, staff, inventory, recipes, products, sales
  auth/         # Login, password change
  cajero/       # Cashier operations
  cliente/      # Customer portal (reservations, loyalty points)
  mesero/       # Waiter table/order management
  produccion/   # Kitchen and bar production queues
layouts/        # Shell layout components per role
shared/         # Reusable components, pipes, directives
```

Authentication uses JWT stored client-side; the interceptor in `core/interceptors/` attaches the access token to all requests.

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
- **Scope strictly to the HU:** do not implement features, helpers, or abstractions that are not required by the current user story, even if they seem useful for future stories.
- **Postman tests:** when adding or modifying endpoints, create the corresponding Postman tests using `backend/postman/prompt` as the base template.

## Notas de implementación — HE-02 (Dashboard del cliente)

### Colecciones Postman generadas
Las pruebas están divididas en archivos por endpoint:
- `reservas/reservas_disponibilidad.postman_collection.json`
- `reservas/reservas_crear.postman_collection.json`
- `reservas/reservas_cliente_futuras.postman_collection.json`
- `reservas/reservas_cliente_canceladas_devueltas.postman_collection.json`
- `reservas/reservas_detalle.postman_collection.json`
- `mesas_comandas/visitas_cliente_historial.postman_collection.json`
- `mesas_comandas/visitas_cliente_detalle.postman_collection.json` 
- `usuarios/clientes_me_puntos.postman_collection.json` 
- `usuarios/clientes_puntos.postman_collection.json` 
- `usuarios/clientes_canje_puntos.postman_collection.json` 

### Decisiones de diseño relevantes
- **CA-07 aparece dos veces** en los requisitos originales con semántica distinta: "detalle de reserva futura" (`GET /api/reservas/{id}/detalle`) y "detalle de visita" (`GET /api/visitas/cliente/{id}/detalle`). Ambas se implementaron y prueban por separado.
- **CA-04 (actualización de puntos)** no puede probarse de forma aislada: el incremento ocurre en `VentaService` al cerrar cuenta. El test PA-03 verifica el *estado resultante*; para ejecutarlo de extremo a extremo hay que cerrar una cuenta desde la colección de `pagos_caja` antes de correr PA-03.
- **`puntosAcumulados` nunca disminuye**, ni siquiera tras el canje. El test PC-03 verifica este invariante explícitamente.
- **Walk-ins vs visitas con reserva:** `visitaIdWalkIn` y `visitaIdConReserva` se intentan capturar dinámicamente en VA-01. Si el cliente de prueba no tiene ambos tipos en seed data, deben setearse manualmente en el entorno.

### Variables de entorno añadidas a `AlToro-local.postman_environment.json`
Las siguientes variables se agregaron con valor vacío; deben poblarse antes de ejecutar las colecciones correspondientes:

| Variable | Cómo obtener el valor |
|----------|-----------------------|
| `fechaFueraHorario` | Valor fijo: `2026-12-15T15:00:00` (ya seteado) |
| `fechaDespuesCierre` | Valor fijo: `2026-12-15T23:00:00` (ya seteado) |
| `visitaIdConReserva` | Capturado en VA-01 o: `SELECT visita_id FROM restaurante.visita WHERE reserva_id IS NOT NULL LIMIT 1` |
| `visitaIdWalkIn` | Capturado en VA-01 o: `SELECT visita_id FROM restaurante.visita WHERE reserva_id IS NULL LIMIT 1` |
| `reservaIdConPreOrden` | Capturado en RB-04 o: `SELECT r.reserva_id FROM restaurante.reserva r JOIN restaurante.comanda c ON c.reserva_id = r.reserva_id LIMIT 1` |
| `reservaIdCancelada` | `SELECT reserva_id FROM restaurante.reserva WHERE reserva_estado IN ('CANCELADA','DEVUELTA') LIMIT 1` |
| `clienteIdConPuntos` | `SELECT usuario_id FROM restaurante.cliente WHERE cliente_puntos > 0 LIMIT 1` |
| `clienteIdSinPuntos` | `SELECT usuario_id FROM restaurante.cliente WHERE cliente_puntos = 0 LIMIT 1` |

## Git Workflow
- **Protected branches:** `main` (production), `develop` (integration)
- **Branch naming:** `PA-{jira-number}-{short-description-with-dashes}` (e.g., `PA-66-reserva-orden`)
- **Commit convention:** `<type>(<scope>): <description>` — types: `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`
- **Workflow:** branch from `develop` → commit → rebase onto `develop` → PR → merge; never commit directly to `main` or `develop`
- See `CONTRIBUTING.md` for full details
