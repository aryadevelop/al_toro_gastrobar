# API Conventions — Naming y Diseño de Endpoints

**Aplica solo a endpoints NUEVOS.** Los endpoints actuales se mantienen sin cambios por compatibilidad con frontend.

## 1. Estructura de URLs

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

## 2. Métodos HTTP

```
✅ GET     - lectura (idempotente, sin side effects)
✅ POST    - crear recurso o acciones complejas
✅ PUT     - actualizar recurso completo
✅ PATCH   - actualizar parcial o acciones específicas
✅ DELETE  - eliminar recurso
```

## 3. GET by ID retorna detalle completo

```
✅ CORRECTO: GET /recursos/{id}
   Siempre retorna el detalle completo. NO añadir sufijo /detalle (redundante).

❌ EVITAR: GET /recursos/{id}/detalle
```

## 4. Filtrado y Búsqueda

```
✅ PREFERIR: Query parameters para filtros
   GET /reservas?estado=PENDIENTE&fecha=2026-01-15
   GET /productos?menuEspecial=true
   GET /visitas?finalizada=true&clienteId=123

❌ EVITAR: Rutas diferentes por filtro
   GET /reservas/pendientes
   GET /productos/especiales

   EXCEPCIÓN: Si la respuesta es estructuralmente MUY diferente,
              puede justificarse un endpoint dedicado.
```

## 5. Acciones Específicas (No-CRUD)

```
✅ CORRECTO: POST/PATCH /recursos/{id}/accion
   POST /reservas/{id}/cancelar
   PATCH /notificaciones/{id}/atender
   POST /visitas/{id}/asistencia

✅ CORRECTO: Sustantivos en URL principal, NO verbos
   GET /reservas (NO /consultarReservas)
   POST /ventas (NO /cerrarCuenta)
```

## 6. Autorización por Rol

```
✅ PREFERIR: Validar rol en backend con @PreAuthorize
   Misma URL para todos los roles, diferenciar lógica en controller.

❌ EVITAR: Prefijos de rol en URLs
   GET /cliente/reservas → GET /reservas (+ ownership validation)
   GET /mesero/consulta → GET /reservas?... (+ role check)

   EXCEPCIÓN: Endpoints "self" o "me" (singleton del usuario actual).
              GET /clientes/me/puntos ✅
```

## 7. Recursos Singleton

```
✅ /me para recursos del usuario actual
   GET /clientes/me/puntos
   GET /auth/me

✅ Singular para singletons semánticos
   GET /visitas/activa (solo 1 visita activa por cliente)

   ALTERNATIVA: Query param con limit=1
   GET /visitas?estado=activa&limit=1
```

## 8. Versionamiento (Futuro)

```
⚠️ Para breaking changes, usar versionamiento:
   /api/v1/recursos (versión actual, legacy)
   /api/v2/recursos (nuevas convenciones)

   NO implementar ahora.
```

## 9. Path Parameters — Consistencia

```
✅ OPCIÓN 1: Específicos con camelCase
   GET /reservas/{reservaId}
   GET /clientes/{clienteId}

✅ OPCIÓN 2: Genéricos con {id}
   GET /reservas/{id}
   GET /clientes/{id}

⚠️ ELEGIR UNA y mantener consistencia.
   Futuro: preferir {id} genérico.
```

## 10. Ejemplos

```java
// CRUD básico
@GetMapping
public ResponseEntity<?> listar() { ... }                                // GET /recursos

@GetMapping("/{id}")
public ResponseEntity<?> obtener(@PathVariable Long id) { ... }          // GET /recursos/{id}

@PostMapping
public ResponseEntity<?> crear(@RequestBody Request req) { ... }         // POST /recursos

@PutMapping("/{id}")
public ResponseEntity<?> actualizar(@PathVariable Long id,
                                     @RequestBody Request req) { ... }

// Endpoint con filtros
@GetMapping
public ResponseEntity<?> listar(
    @RequestParam(required = false) String estado,
    @RequestParam(required = false) LocalDate fecha) { ... }

// Acción específica
@PostMapping("/{id}/cancelar")
@PreAuthorize("hasRole('CLIENTE')")
public ResponseEntity<?> cancelar(@PathVariable Long id) { ... }

// Singleton
@GetMapping("/me/puntos")
@PreAuthorize("hasRole('CLIENTE')")
public ResponseEntity<?> misPuntos() { ... }
```

## 11. Checklist de Code Review

Antes de aprobar un nuevo endpoint, verificar:
- [ ] URL en plural (salvo singletons válidos)
- [ ] kebab-case en URL
- [ ] camelCase en path/query params
- [ ] NO sufijos `/detalle` redundantes
- [ ] NO prefijos de rol innecesarios
- [ ] Filtros como query params, NO rutas separadas
- [ ] Acciones específicas con POST/PATCH, NO verbos en URL
- [ ] Consistencia con path params ({id} vs {recursoId})
