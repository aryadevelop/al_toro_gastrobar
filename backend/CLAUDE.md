# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with the **backend** of this repository.

## Project Overview

Al Toro Gastrobar is a restaurant management system with role-based portals for waiters, cashiers, kitchen/bar production staff, admins, and customers. Backend: Spring Boot 3.5 + Java 21 + PostgreSQL 15 + RabbitMQ 3.13.

---

## Planning Approval Protocol

**MANDATORY CHECKPOINT** — Before implementing ANY feature, get explicit user approval for a plan covering:

1. **Resumen Ejecutivo**: Qué se construirá, qué problema resuelve, quién lo usará
2. **Lógica de Implementación**: Flujo Controller→Service→Repository, validaciones, mappers, side effects (RabbitMQ/WebSocket)
3. **Pruebas Propuestas**: Tests unitarios (ServiceTest, ControllerTest, MapperTest) y Postman (collection, test cases)
4. **DTOs Structure**: Todos los DTOs nuevos/modificados con campos completos y tipos
5. **Controller Access Rules**: `@PreAuthorize` para cada endpoint + ownership validation si aplica
6. **Functional Clarifications**: Assumptions de negocio que necesitan confirmación
7. **Scope Confirmation**: Qué INCLUYE y qué EXCLUYE la implementación

**Format**: Presentar las 7 secciones, esperar `'approved'` del usuario antes de implementar.

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
docker compose up --build                # dev (uses override)
docker compose logs -f api               # stream backend logs
docker compose down -v                   # stop + remove volumes
```

**Environment**: Copy `.env.prod.example` to `.env.prod` for production. Dev uses defaults from `docker-compose.override.yml`.

---

## Architecture

**Backend structure**: Modular Spring Boot app. Each module follows `controller → service → repository → entity + DTOs + mapper` layering under `modules/`:

| Module | Responsibility |
|---|---|
| `auth` | JWT issue/refresh, session tracking (`Sesion` table) |
| `usuarios` | `Cliente` (loyalty points), `Empleado`, `UsuarioRol`, point redemption |
| `reservas` | Reservations, zones, decorations, availability blocks, WhatsApp |
| `produccion` | Menu products, categories, special menus |
| `mesas_comandas` | Tables (`Mesa`), orders (`Comanda`), visits (`Visita`), notifications |
| `pagos_caja` | Sales (`Venta`), installment payments (`Abono`) |
| `inventario` | Supplies, recipes, modification options, movement log |
| `notificaciones` | Real-time alerts via RabbitMQ + WebSocket |
| `reportes` | Analytics (placeholder) |

**Cross-cutting** (`shared/`): SecurityConfig, RabbitMQConfig, WebSocketConfig, exception handler, `ApiResponse<T>` wrapper

**Roles**: `CLIENTE`, `MESERO`, `CAJERO`, `COCINERO`, `BARTENDER`, `ADMIN` (a user can hold multiple)

**Spring profiles**: `dev` (validate, SQL logging, Swagger at `/swagger-ui.html`) | `prod` (Flyway only, Swagger disabled)

---

## Domain Model

All entities extend `AuditableEntity` (createdAt, updatedAt). Schema: `restaurante`.

| Entity | Key Fields / Notes |
|--------|-------------------|
| `Usuario` | `usuarioEmail` (unique), `usuarioPassword` (BCrypt) |
| `Sesion` | `sesionToken`, `sesionRefreshToken`, `sesionActiva` bool |
| `UsuarioRol` | Composite PK (usuarioId, rolNombre) |
| `Cliente` | `clientePuntos` (redeemable), `clientePuntosAcumulados` (lifetime) |
| `Empleado` | PK = FK to usuario |
| `Zona` | `zonaCapacidadPersonas`; M:N with `Decoracion` |
| `Decoracion` | `decoracionCostoAdicional` (NULL=free, >0=paid min 1.00) |
| `Reserva` | States: `PENDIENTE→CONFIRMADA→ATENDIDA\|CANCELADA\|DEVUELTA\|INASISTENCIA`; Types: `BASICA\|ESPECIAL` |
| `Abono` | Types: `ANTICIPO\|DEVOLUCION`; linked to Reserva and Cajero |
| `Visita` | `visitaFechaHoraFin` NULL = active visit |
| `Mesa` | States: `ESPERA→EN_PREPARACION→ATENDIDA→CERRADA` |
| `Comanda` | Estaciones: `COCINA\|BARRA`; States: `PRE_RESERVA→PENDIENTE→EN_PREPARACION→LISTO→COMPLETADO` |
| `Producto` | Types: `VENTA_DIRECTA\|PREPARACION`; Categories: `PLATO\|BEBIDA\|OTRO` |
| `Venta` | PK=FK to Visita; closes visit; +1 loyalty point |
| `Notificacion` | Types: `ATENCION\|PLATOS_LISTOS\|BEBIDAS_LISTAS\|CAMBIO`; States: `ACTIVA\|ATENDIDA` |

---

## API Endpoints

Base URL: `http://localhost:8080/api`

**Ver**: `ENDPOINTS.md` para lista completa de todos los endpoints con métodos, accesos y descripciones detalladas.

**Resumen por módulo** (30 endpoints total):
- **Auth** (5): login, register, refresh, me, logout
- **Reservas** (9): disponibilidad, crear, modificar, cancelar, futuras, canceladas, detalle, consulta mesero
- **Clientes** (3): me/puntos, puntos by ID, canje-puntos
- **Productos** (2): carta, menu-especial
- **Mesas** (5): mapa, detalle, items-produccion, asignar, zonas-disponibles
- **Visitas** (4): historial, detalle, activa (multi-rol), asistencia
- **Ventas** (1): cerrar cuenta
- **Notificaciones** (1): atender

---

## Naming Conventions for Endpoints (Futuras Implementaciones)

**IMPORTANTE**: Estas reglas aplican SOLO para nuevos endpoints. Los endpoints actuales se mantienen sin cambios por compatibilidad con frontend.

### 1. Estructura de URLs

```
✅ OBLIGATORIO: Recursos en plural
   GET /productos
   GET /reservas
   GET /mesas

✅ OBLIGATORIO: kebab-case para segmentos de URL
   GET /menu-especial
   POST /canje-puntos
   GET /zonas-disponibles

✅ OBLIGATORIO: camelCase para path parameters
   GET /reservas/{reservaId}
   GET /clientes/{clienteId}
   ALTERNATIVA: usar {id} genérico si el contexto es claro

✅ OBLIGATORIO: camelCase para query parameters
   GET /reservas?emailCliente=...
   GET /visitas?estado=activa&fecha=2026-01-15
```

### 2. Métodos HTTP

```
✅ GET - lectura (siempre idempotente, sin side effects)
✅ POST - crear recurso o acciones complejas
✅ PUT - actualizar recurso completo
✅ PATCH - actualizar parcial o acciones específicas
✅ DELETE - eliminar recurso
```

### 3. GET by ID retorna detalle completo

```
✅ CORRECTO: GET /recursos/{id}
   Siempre retorna el detalle completo del recurso.
   NO añadir sufijo /detalle (redundante).

❌ EVITAR: GET /recursos/{id}/detalle
   El sufijo /detalle no agrega valor semántico.
```

### 4. Filtrado y Búsqueda

```
✅ PREFERIR: Query parameters para filtros
   GET /reservas?estado=PENDIENTE&fecha=2026-01-15
   GET /productos?menuEspecial=true
   GET /visitas?finalizada=true&clienteId=123

❌ EVITAR: Rutas diferentes por filtro
   GET /reservas/pendientes
   GET /productos/especiales
   GET /visitas/finalizadas
   
   EXCEPCIÓN: Si la respuesta es estructuralmente MUY diferente,
              puede justificarse un endpoint dedicado.
```

### 5. Acciones Específicas (No-CRUD)

```
✅ CORRECTO: POST/PATCH /recursos/{id}/accion
   POST /reservas/{id}/cancelar
   PATCH /notificaciones/{id}/atender
   POST /visitas/{id}/asistencia

✅ CORRECTO: Usar sustantivos, NO verbos en URL principal
   GET /reservas (NO /consultarReservas)
   POST /ventas (NO /cerrarCuenta)
```

### 6. Autorización por Rol

```
✅ PREFERIR: Validar rol en backend con @PreAuthorize
   Usar MISMA URL para todos los roles, diferenciar lógica en controller.

❌ EVITAR: Prefijos de rol en URLs
   GET /cliente/reservas → GET /reservas (+ ownership validation)
   GET /mesero/consulta → GET /reservas?... (+ role check)
   
   EXCEPCIÓN: Endpoints "self" o "me" (singleton del usuario actual)
              GET /clientes/me/puntos ✅ (válido)
```

### 7. Recursos Singleton

```
✅ CORRECTO: Usar /me para recursos del usuario actual
   GET /clientes/me/puntos
   GET /auth/me

✅ CORRECTO: Singular para singletons semánticos
   GET /visitas/activa (solo 1 visita activa por cliente)
   
   ALTERNATIVA: Query param con limit=1
   GET /visitas?estado=activa&limit=1
```

### 8. Versionamiento (Futuro)

```
⚠️ RECOMENDACIÓN: Para breaking changes, usar versionamiento
   /api/v1/recursos (versión actual, legacy)
   /api/v2/recursos (nuevas convenciones)
   
   NO implementar ahora, pero tener en cuenta para futuras migraciones.
```

### 9. Consistencia en Path Parameters

```
✅ OPCIÓN 1: Específicos con camelCase
   GET /reservas/{reservaId}
   GET /clientes/{clienteId}
   GET /mesas/{mesaId}

✅ OPCIÓN 2: Genéricos con {id}
   GET /reservas/{id}
   GET /clientes/{id}
   GET /mesas/{id}

⚠️ ELEGIR UNA y mantener consistencia en todo el proyecto.
   Actual: mezcla de ambos estilos.
   Futuro: preferir {id} genérico para simplicidad.
```

### 10. Ejemplos de Convenciones Correctas

```java
// ✅ Endpoint simple CRUD
@GetMapping
public ResponseEntity<?> listar() { ... }  // GET /recursos

@GetMapping("/{id}")
public ResponseEntity<?> obtener(@PathVariable Long id) { ... }  // GET /recursos/{id}

@PostMapping
public ResponseEntity<?> crear(@RequestBody Request req) { ... }  // POST /recursos

@PutMapping("/{id}")
public ResponseEntity<?> actualizar(@PathVariable Long id, @RequestBody Request req) { ... }

// ✅ Endpoint con filtros
@GetMapping
public ResponseEntity<?> listar(
    @RequestParam(required = false) String estado,
    @RequestParam(required = false) LocalDate fecha
) { ... }  // GET /recursos?estado=...&fecha=...

// ✅ Endpoint de acción específica
@PostMapping("/{id}/cancelar")
@PreAuthorize("hasRole('CLIENTE')")
public ResponseEntity<?> cancelar(@PathVariable Long id) { ... }

// ✅ Endpoint singleton
@GetMapping("/me/puntos")
@PreAuthorize("hasRole('CLIENTE')")
public ResponseEntity<?> misPuntos() { ... }
```

### 11. Validación de Reglas en Code Review

Antes de aprobar un nuevo endpoint, verificar:
- [ ] URL en plural (salvo singletons válidos)
- [ ] kebab-case en URL
- [ ] camelCase en path/query params
- [ ] NO sufijos `/detalle` redundantes
- [ ] NO prefijos de rol innecesarios (`/cliente/`, `/mesero/`)
- [ ] Filtros como query params, NO rutas separadas
- [ ] Acciones específicas con POST/PATCH, NO verbos en URL principal
- [ ] Consistencia con path params ({id} vs {recursoId})

---

## Security

- **Stateless JWT** — HMAC-SHA256, access token 30 min, refresh token 7 days
- **Filter chain**: `JwtAuthenticationFilter` validates signature, expiration, `sesionActiva=true`
- **BCrypt** strength 10
- **CORS**: `localhost:4200` (Angular), `localhost:80` (Docker)
- **Public endpoints**: `/api/auth/*`, `/swagger-ui/**`, `/v3/api-docs/**`, `/actuator/health`, `/ws/**`
- **Ownership validation**: CLIENTE can only access own reservations, visits, points

---

## Business Rules

- **Reservation hours**: 17:00–22:00 only
- **Modification cutoff**: BASICA 13:00, ESPECIAL 23:00 (day before)
- **WhatsApp flag**: true for BASICA→ESPECIAL or net abono change
- **Cancellation**: no time restriction; `requiereWhatsApp=true` if refund needed
- **Zone capacity**: computed by summing `reservaNumeroPersonas` for active reservations
- **Decoration exclusivity**: one decoration per zone per time slot
- **PRE_RESERVA comanda**: transitions to PENDIENTE when visit starts
- **Loyalty points**: +1 per closed Venta; `clientePuntos`=redeemable (resets on redemption), `clientePuntosAcumulados`=lifetime

---

## Database

**PostgreSQL 15** — container `altoro_postgres`, schema `restaurante`

**Flyway migrations** (`src/main/resources/db/migration/`):
- `V1__init_schema.sql` — Full schema (tables, constraints, indexes)
- `V2__seed_data.sql` — Base seed (admin, employees, clients, zones, menu)
- `V3__dev_data.sql` — Dev/test data
- `V4__mr_test_seed.sql`, `V5__cr_test_seed.sql` — Integration test seeds

**CRITICAL MIGRATION RULE:**

⚠️ **NEVER create new migrations beyond V5.** Schema changes: modify `V1`. Base seed: modify `V2`. Dev data: modify `V3`.

**When modifying**:
1. Update V1/V2/V3 file
2. `docker compose down -v`
3. `docker compose up --build`

Reset to seed: `./mvnw flyway:clean flyway:migrate` (dev only)

---

## RabbitMQ

Exchange: `altoro.topic` (durable, topic type) — container `altoro_rabbitmq`

| Routing Key | Queue | Consumer |
|-------------|-------|----------|
| `comanda.nueva` | `q.comanda.produccion` | ProduccionService |
| `impresion.ticket` | `q.impresion.ticket` | Node.js bridge |
| `notificacion.email` | `q.notificacion.email` | NotificacionEmailService |
| `notificacion.ws` | `q.notificacion.ws` | WebSocket publisher |

---

## Tests

**Current state**: 152 tests pass, 1 skipped

```bash
./mvnw test   # from backend/
```

Key test files: `JwtTokenProviderTest`, `AuthServiceTest`, `ReservaValidadorTest` (19 tests), `ReservaServiceModificarTest` (12 tests), `PuntosServiceTest` (8 tests)

---

## Postman — Conventions

### Collection format
- YAML format (Postman for VS Code plugin)
- `.resources/definition.yaml` — collection-level hooks
- `XX-NN Nombre – STATUS.request.yaml` — test cases

### Autonomous login pattern — CRITICAL
**ALL tests** include `beforeRequest` script with autonomous login via `pm.sendRequest`. Never use `{{tokenVariable}}` without `beforeRequest` setting it.

### Token naming
| Role | Variable |
|------|----------|
| CLIENTE | `clienteToken` |
| MESERO | `meseroToken` |
| CAJERO | `cajeroToken` |
| ADMIN | `adminToken` |

### Running collections
**Prerequisite**: Clean state with `psql -U postgres -d altoro_db -f postman/cleanup-notificaciones.sql`

### Critical rules

**1. Variables de ambiente en URLs dinámicas**

**Problema**: `{{reservaId}}` vacía causa URLs malformadas (`//`) → `RequestRejectedException`

**Solución**: Configurar variable temporal en `beforeRequest`:
```javascript
const reservaId = pm.environment.get('reservaIdConPreOrden') || '10';
pm.environment.set('tmpReservaId', reservaId);
// ... login ...
// afterResponse: pm.environment.unset('tmpReservaId');
```

**Regla**: Nunca usar `{{var}}` directo en URL si puede estar vacío. Usar `tmp` prefix.

**2. Fechas dinámicas**

```javascript
// Reservas futuras: SIEMPRE mañana
const d = new Date();
d.setDate(d.getDate() + 1);  // Mañana
d.setHours(19, 0, 0, 0);
const fechaHora = `${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())}T19:00:00`;
```

**Regla**: Nunca fechas fijas (`2026-12-25`). Usar `d.setDate(d.getDate() + 1)` para futuras.

**3. Códigos de error**

**Regla**: Pruebas verifican código serializado (`ENT-001`), **no** enum (`ENTITY_NOT_FOUND`):
```javascript
pm.expect(body.code).to.equal('ENT-001');  // ✓ Correcto
// NUNCA: pm.expect(body.code).to.equal('ENTITY_NOT_FOUND');
```

**Mapeo**: `ENTITY_NOT_FOUND`→`ENT-001`, `VALIDATION_ERROR`→`VAL-001`, `UNAUTHORIZED`→`AUTH-001`, `ACCESS_DENIED`→`AUTH-002`

---

## Coding Patterns & Conventions

### Services

**Reglas obligatorias:**
- ❌ NO logging (`@Slf4j`, `log.debug()`)
- ✅ Javadoc detallado con flujo paso a paso
- ✅ `@Transactional(readOnly = true)` en consultas
- ✅ `@RequiredArgsConstructor` para inyección
- ✅ `Optional.orElseThrow()` con `ErrorCode` específico

**Estructura**:
```java
@Service
@RequiredArgsConstructor
public class MiService {
    private final MiRepository miRepository;
    private final MiMapper miMapper;
    
    @Transactional(readOnly = true)
    public MiResponse obtener(Long id) {
        // 1. Buscar entidad con orElseThrow
        MiEntidad e = miRepository.findById(id).orElseThrow(() -> 
            new BusinessException(ErrorCode.ENTITY_NOT_FOUND, "...", HttpStatus.NOT_FOUND));
        // 2. Consultas auxiliares
        Optional<Related> related = relatedRepo.findByXxx(id);
        // 3. Delegar mapeo (NO construir DTOs aquí)
        return miMapper.toResponse(e, related);
    }
}
```

---

### Mappers

**Reglas obligatorias:**
- ✅ **SIEMPRE ordenar items por categoría ANTES de agrupar/mapear**
- ✅ Reutilizar comparador estático
- ✅ Enums → String usando `.name()`
- ✅ `empleadoNombre` (campo completo), NO separación nombre/apellido
- ✅ `usuarioNombre` (campo único), NO existe `usuarioApellido`

**Comparador estándar**:
```java
private static final Comparator<ComandaItem> COMPARATOR_POR_CATEGORIA =
    Comparator.comparing(item -> item.getProducto().getProductoCategoria().ordinal());

public List<ItemResponse> mapearOrdenados(List<ComandaItem> items) {
    return items.stream()
        .sorted(COMPARATOR_POR_CATEGORIA)  // ORDENAR PRIMERO
        .map(this::toItemResponse)          // LUEGO MAPEAR
        .collect(Collectors.toList());
}
```

**Agrupación** (nombreProducto + descripcion):
```java
Map<String, List<ComandaItem>> agrupados = items.stream()
    .sorted(COMPARATOR_POR_CATEGORIA)  // ORDENAR ANTES
    .collect(Collectors.groupingBy(item ->
        item.getProducto().getProductoNombre() + "|" +
        (item.getComandaItemDescripcion() != null ? item.getComandaItemDescripcion() : "")
    ));
```

---

### DTOs

**Reglas:**
- ✅ Enums → String en DTOs (`.name()` en mappers)
- ✅ Inmutabilidad: `@Getter` + `@Builder` + campos `final`
- ✅ Javadoc: documentar cada campo con valores posibles
- ✅ Incluir `String categoriaProducto` cuando se muestran items

```java
@Getter @Builder
public class ItemResponse {
    /** Nombre del producto */
    private final String nombreProducto;
    /** Categoría: "PLATO", "BEBIDA", "OTRO" */
    private final String categoriaProducto;
    private final Integer cantidad;
}
```

---

### ErrorCode Usage

| Código | Enum | HTTP | Uso |
|--------|------|------|-----|
| `ENT-001` | `ENTITY_NOT_FOUND` | 404 | Entidad no encontrada |
| `ENT-002` | `ENTITY_ALREADY_EXISTS` | 409 | Duplicada |
| `AUTH-001` | `INVALID_CREDENTIALS` | 401 | Credenciales inválidas |
| `AUTH-002` | `ACCESS_DENIED` | 403 | Sin permisos |
| `NEG-001` | `BUSINESS_ERROR` | 400/409 | Regla violada |
| `NEG-002` | `INVALID_STATE` | 409 | Estado inválido |
| `VAL-001` | `VALIDATION_ERROR` | 400 | Validación |

---

### Controller Tests

**Reglas:**
- ✅ `@WebMvcTest(controllers = MiController.class)`
- ✅ Importar `TestSecurityConfig` permisiva
- ✅ `@MockitoBean` para services y dependencias seguridad
- ✅ `@Nested` + `@DisplayName` agrupar por endpoint
- ✅ Nombres: `condicion_resultadoEsperado()`

```java
@WebMvcTest(controllers = MiController.class)
@Import(MiControllerTest.PermissiveSecurityConfig.class)
class MiControllerTest {
    static class PermissiveSecurityConfig {
        @Bean @Order(1)
        SecurityFilterChain testFilterChain(HttpSecurity http) throws Exception {
            return http.securityMatcher("/**").csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll()).build();
        }
    }
    @Autowired MockMvc mockMvc;
    @MockitoBean MiService miService;
    @MockitoBean JwtTokenProvider jwtTokenProvider;
    @MockitoBean UserDetailsService userDetailsService;
    @MockitoBean SesionRepository sesionRepository;
    
    @Nested @DisplayName("GET /api/mi-endpoint")
    class ObtenerDato {
        @Test @WithMockUser(username = "mesero@altoro.com", roles = "MESERO")
        @DisplayName("ID válido → 200 OK")
        void idValido_retorna200() throws Exception {
            when(miService.obtener(1L)).thenReturn(response);
            mockMvc.perform(get("/api/mi-endpoint/1"))
                .andExpect(status().isOk());
            verify(miService).obtener(1L);
        }
    }
}
```

---

### Postman Testing — DOS TIPOS

**⚠️ IMPORTANTE**: Hay DOS tipos con patrones diferentes.

#### 1. Manual Testing (`manual-testing/`)

**Propósito**: Pruebas manuales rápidas sin validaciones automáticas.

**Convenciones**:
- ✅ Solo `{{baseUrl}}`
- ✅ Credenciales hardcoded en `beforeRequest`
- ✅ Password: `Al.Toro2026!`
- ✅ Tokens temporales con `tmp` prefix
- ✅ Cleanup en `afterResponse` (solo unset)
- ❌ NO tests en `afterResponse`
- ✅ Formato: `XX-YY Descripción – ROL.request.yaml`

**Numeración**: `00-XX` Auth, `10-XX` Productos, `20-XX` Reservas (CLIENTE), `30-XX` Reservas (MESERO), `40-XX` Visitas, `50-XX` Puntos, `60-XX` Ventas, `70-XX` Notificaciones, `80-XX` Mesas

#### 2. Automated Collections (`collections/`)

**Propósito**: Pruebas automatizadas con validaciones.

**Convenciones**:
- ✅ Variables de entorno (`emailMesero`, `passwordValida`)
- ✅ Login autónomo en `beforeRequest`
- ✅ Limpieza estado previo en `beforeRequest`
- ✅ Tests obligatorios en `afterResponse`
- ✅ Variables temporales SOLO si necesarias
- ❌ NO cleanup en `afterResponse`
- ✅ Formato: `XX-YY Descripción – Código HTTP.request.yaml`
- ✅ **INDEPENDENCIA**: ejecutable solo, sin depender del orden

**Diferencias clave**:

| Aspecto | Manual | Automated |
|---------|--------|-----------|
| Credenciales | Hardcoded | Variables entorno |
| Password | `Al.Toro2026!` | `passwordValida` |
| Tokens | `tmpMeseroToken` | `meseroToken` |
| afterResponse | Solo cleanup | Tests + guardar IDs |
| Cleanup | En `afterResponse` | En `beforeRequest` siguiente |
| Independencia | No requerida | OBLIGATORIA |

---

### Git Commits

**Reglas:**
- ❌ NO ejecutar commits automáticamente — reportar mensaje en español
- ✅ Formato: `<tipo>(<módulo>): <descripción en español>`
- ✅ Co-author obligatorio: `Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>`

**Tipos**: `feat`, `fix`, `refactor`, `test`, `docs`, `style`, `chore`

**Ejemplo**:
```bash
git commit -m "feat(mesas): añadir endpoint GET /api/mesas para consultar mapa"
```

---

### WebSocket Integration

**REGLA CRÍTICA**: Siempre analizar si debe enviar mensaje WS para actualización en tiempo real.

**Patrón**: REST + Publisher (NO `@MessageMapping` para operaciones con persistencia)

**Cuándo enviar WS**: crear/modificar/eliminar mesas, cambiar estado, crear/atender notificaciones, cerrar cuenta, actualizar items, cambios reservas

**Cuándo NO**: consultas GET, operaciones que no afectan otros usuarios

**Publisher Pattern**:
```java
@Service @RequiredArgsConstructor
public class MiWsPublisher {
    private final SimpMessagingTemplate messagingTemplate;
    public void publicarEvento(Long id, MiWsMessage msg) {
        messagingTemplate.convertAndSend("/topic/mi-recurso/" + id, msg);
    }
}

// En Service:
@Transactional
public MiResponse crear(MiRequest req) {
    MiEntidad e = miRepository.save(...);
    MiResponse res = miMapper.toResponse(e);
    wsPublisher.publicarEvento(e.getId(), buildWsMessage(e));  // WS DESPUÉS de persistir
    return res;
}
```

**Tópicos**: `/topic/visita/{visitaId}/orden`, `/topic/visita/{visitaId}/cuenta`, `/topic/visita/{visitaId}/asistencia`, `/topic/mesas/asistencia`, `/topic/mesas`, `/topic/reservas/cambios`

---

## Working Rules

- **Never modify the frontend** — backend-only
- **Documentation mandatory**: Javadoc (clases, métodos) + inline comments (`//`) en pasos no-obvios
- **Mappers**: Toda transformación entity→DTO en mapper dedicado, NO builders inline en services
- **Ordenamiento items**: SIEMPRE ordenar por categoría (PLATO→BEBIDA→OTRO) usando `Comparator.comparing(item -> item.getProducto().getProductoCategoria().ordinal())` ANTES de transformar/agrupar
- **Acceso multi-rol**: Seguir patrón `VisitaController`: `@PreAuthorize("hasAnyRole(...)")` + parámetro opcional `emailCliente` + ownership solo para CLIENTE

### Code Coverage (JaCoCo)

**Objetivos mínimos**: Services 90-95%, Controllers 85-90%, Mappers 90-95%, Validators 95%+, Repositories 70-80% (solo custom queries)

**Reglas**:
1. NUNCA implementar feature sin tests — TDD
2. Tests ANTES del commit — `./mvnw clean test jacoco:report`
3. Cubrir edge cases — validaciones, errores, casos límite, nulls, listas vacías
4. Branches coverage — cada `if/else`, `switch`, `try/catch` con tests para TODAS las ramas

**Exclusiones válidas**: DTOs/Entities sin lógica, `*Application.java`, Exception classes con solo constructores, Config classes

- **Scope to the HU**: no features/helpers/abstractions no requeridas por HU actual
- **Postman tests**: crear al añadir/modificar endpoints
- **Manual testing requests**: OBLIGATORIO para cada nuevo endpoint — seguir template en `00-01 Login CLIENTE.request.yaml`

---

## Git Workflow

- **Protected branches**: `main` (production), `develop` (integration)
- **Branch naming**: `PA-{jira-number}-{short-description}` (e.g., `PA-96-estado-asistencia`)
- **Commit convention**: `<type>(<scope>): <description>`
- **Workflow**: branch from `develop` → implement → rebase onto `develop` → PR → merge; never commit to `main`/`develop` directly
