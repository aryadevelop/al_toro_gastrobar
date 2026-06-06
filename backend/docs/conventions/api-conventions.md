# Convenciones de API — Naming y diseño de endpoints

Guía de diseño para endpoints nuevos del proyecto Al Toro Gastrobar. Los endpoints existentes se mantienen sin cambios por compatibilidad con el frontend.

---

## Tabla de contenido

- [Estructura de URLs](#estructura-de-urls)
- [Métodos HTTP](#métodos-http)
- [Filtrado y búsqueda](#filtrado-y-búsqueda)
- [Acciones específicas](#acciones-específicas)
- [Autorización por rol](#autorización-por-rol)
- [Consistencia de path parameters](#consistencia-de-path-parameters)
- [Checklist de code review](#checklist-de-code-review)

---

## Estructura de URLs

Los segmentos de URL siguen convenciones distintas según su tipo.

| Elemento | Convención | Ejemplo |
|----------|------------|---------|
| Recursos | Plural | `/productos`, `/reservas`, `/mesas` |
| Segmentos de URL | `kebab-case` | `/menu-especial`, `/canje-puntos` |
| Path parameters | `camelCase` | `{reservaId}`, `{clienteId}` |
| Query parameters | `camelCase` | `?emailCliente=`, `?estado=activa` |

---

## Métodos HTTP

| Método | Uso |
|--------|-----|
| `GET` | Lectura idempotente sin efectos secundarios |
| `POST` | Crear recurso o ejecutar acción compleja |
| `PUT` | Actualizar recurso completo |
| `PATCH` | Actualizar parcialmente o ejecutar acción específica |
| `DELETE` | Eliminar recurso |

### GET por ID

`GET /recursos/{id}` retorna el detalle completo del recurso. **No** añadir sufijo `/detalle` — es redundante.

```
CORRECTO:  GET /recursos/{id}
INCORRECTO: GET /recursos/{id}/detalle
```

---

## Filtrado y búsqueda

Usar query parameters para todos los filtros. Evitar rutas separadas por criterio de filtro, salvo cuando la respuesta sea estructuralmente diferente y justifique un endpoint dedicado.

```
CORRECTO:   GET /reservas?estado=PENDIENTE&fecha=2026-01-15
CORRECTO:   GET /productos?menuEspecial=true
INCORRECTO: GET /reservas/pendientes
INCORRECTO: GET /productos/especiales
```

---

## Acciones específicas

Las acciones no-CRUD se expresan como sub-recursos del recurso afectado, usando `POST` o `PATCH`. Los verbos no van en la URL principal.

```
CORRECTO:   POST  /reservas/{id}/cancelar
CORRECTO:   PATCH /notificaciones/{id}/atender
CORRECTO:   POST  /visitas/{id}/asistencia
INCORRECTO: POST  /cancelarReserva
INCORRECTO: POST  /consultarReservas
```

---

## Autorización por rol

Validar el rol en el backend con `@PreAuthorize`. La misma URL sirve a todos los roles; la lógica de diferenciación reside en el controller o el service.

**No** usar prefijos de rol en la URL.

```
CORRECTO:   GET /reservas               (+ validación de ownership en backend)
INCORRECTO: GET /cliente/reservas
INCORRECTO: GET /mesero/consulta
```

---

## Consistencia de path parameters

Elegir una sola convención y mantenerla en todo el proyecto.

| Opción | Ejemplo | Nota |
|--------|---------|------|
| Específico | `{reservaId}`, `{clienteId}` | Más explícito |
| Genérico | `{id}` | Preferido para trabajo futuro |

**Preferir `{id}` genérico** en endpoints nuevos.

## Checklist de code review

Antes de aprobar un endpoint nuevo, verificar cada punto:

- [ ] URL en plural (salvo singletons válidos)
- [ ] Segmentos de URL en `kebab-case`
- [ ] Path y query parameters en `camelCase`
- [ ] Sin sufijo `/detalle` redundante
- [ ] Sin prefijos de rol en la URL
- [ ] Filtros como query parameters, no como rutas separadas
- [ ] Acciones específicas con `POST`/`PATCH`, sin verbos en la URL
- [ ] Path parameters consistentes (`{id}` o `{recursoId}`, no ambos)

---

## Ejemplos de implementación

```java
// CRUD básico
@GetMapping
public ResponseEntity<?> listar() { ... }

@GetMapping("/{id}")
public ResponseEntity<?> obtener(@PathVariable Long id) { ... }

@PostMapping
public ResponseEntity<?> crear(@RequestBody Request req) { ... }

@PutMapping("/{id}")
public ResponseEntity<?> actualizar(@PathVariable Long id, @RequestBody Request req) { ... }

// Filtros como query parameters
@GetMapping
public ResponseEntity<?> listar(
    @RequestParam(required = false) String estado,
    @RequestParam(required = false) LocalDate fecha) { ... }

// Acción específica
@PostMapping("/{id}/cancelar")
@PreAuthorize("hasRole('CLIENTE')")
public ResponseEntity<?> cancelar(@PathVariable Long id) { ... }

```