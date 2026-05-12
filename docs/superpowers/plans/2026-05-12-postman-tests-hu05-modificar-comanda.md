# Plan — Postman tests HU-05 Modificar Comanda (PA-90)

**Fecha:** 2026-05-12
**Rama:** `PA-90-modificar-comanda`
**Endpoints:** 7 (todos `MESERO`, `ADMIN`)

---

## 1. Endpoints + DTOs

| # | Método | Ruta | DTO Request | Notas |
|---|--------|------|-------------|-------|
| E1 | GET    | `/api/comandas/borrador?visitaId={id}` | — | Borrador (incluye vacío válido) |
| E2 | POST   | `/api/comandas/borrador/items` | `AgregarItemRequest` | visitaId, productoId, cantidad[1..250], descripcion?[≤500] |
| E3 | PATCH  | `/api/comandas/borrador/items/{itemId}` | `ModificarItemRequest` | cantidad?[1..250], descripcion?[≤500] |
| E4 | DELETE | `/api/comandas/borrador/items/{itemId}` | — | Elimina par cocina+barra si menú especial |
| E5 | POST   | `/api/comandas/borrador/{comandaId}/enviar` | — | BORRADOR → PENDIENTE; valida stock; eventos RabbitMQ/WS |
| E6 | DELETE | `/api/comandas/borrador?visitaId={id}` | — | Descarta todas las BORRADOR de la visita |
| E7 | PATCH  | `/api/comandas/borrador/{comandaId}/notas` | `NotasRequest` | notas?[≤500] |

---

## 2. Convenciones

- **Carpeta automatizada:** `backend/postman/postman/collections/mesas_comandas/Al Toro – {METHOD} -api-comandas-...`
- **Manual:** numeración **`90-XX Comandas (MESERO)`** (confirmado).
- **Token:** `meseroToken` (automated) / `tmpMeseroToken` (manual). Password `Al.Toro2026!` (manual) / `{{passwordValida}}` (automated).
- **IDs fijos vía SQL seed** — NO variables de entorno (Postman las elimina). Cada test usa IDs hardcoded en URL/body que existen porque el seed SQL los crea.
- **Error codes:** `ENT-001` 404, `AUTH-001` 401, `AUTH-002` 403, `NEG-001` 400/409 regla, `NEG-002` 409 estado, `VAL-001` 400.
- **Importante:** producto menú especial → 400 (`BUSINESS_ERROR + BAD_REQUEST`), NO 409.

---

## 3. Matriz de casos de prueba (automated)

Códigos: `CD-NN` (CD = Comanda Draft, NN secuencial por endpoint).
Ampliada tras revisión del `ComandaBorradorService` (acumulación, modificación libre, cascada de ítems modificados, par menú especial, cantidad=0, menú especial → 400).

### E1 — GET /borrador (7 casos)
| ID | Caso | HTTP |
|----|------|------|
| CD1-01 | Borrador existente con ítems cocina+barra | 200 |
| CD1-02 | Visita sin borrador devuelve estructura vacía válida | 200 |
| CD1-03 | Sin token | 401 |
| CD1-04 | Rol no autorizado (CLIENTE) | 403 |
| CD1-05 | Rol no autorizado (CAJERO) | 403 |
| CD1-06 | visitaId inexistente | 404 |
| CD1-07 | visitaId ausente (param) | 400 |

### E2 — POST /borrador/items (20 casos) — endpoint clave
| ID | Caso | HTTP | Notas |
|----|------|------|-------|
| CD2-01 | Agregar PLATO nuevo (crea comanda COCINA) | 200 | comanda inexistente → crea |
| CD2-02 | Agregar BEBIDA nueva (crea comanda BARRA) | 200 | resuelve estación por categoría |
| CD2-03 | Agregar plato **existente** (mismo productoId, sin descripción) → **acumula cantidad** | 200 | cantidad_total = anterior + nueva |
| CD2-04 | Agregar item con descripción libre `"sin azúcar"` (ítem modificado) | 200 | crea fila separada con descripcion |
| CD2-05 | Agregar mismo item modificado **dos veces** (misma desc) → acumula | 200 | match por descripcion |
| CD2-06 | Agregar mismo productoId con descripción DIFERENTE a una existente | 200 | crea ítem separado, no acumula |
| CD2-07 | Sin token | 401 | |
| CD2-08 | Rol no autorizado (CLIENTE) | 403 | |
| CD2-09 | Rol no autorizado (CAJERO) | 403 | |
| CD2-10 | visitaId ausente | 400 | VAL-001 |
| CD2-11 | productoId ausente | 400 | VAL-001 |
| CD2-12 | cantidad ausente | 400 | VAL-001 |
| CD2-13 | cantidad < 1 | 400 | VAL-001 |
| CD2-14 | cantidad > 250 | 400 | VAL-001 |
| CD2-15 | descripcion > 500 chars | 400 | VAL-001 |
| CD2-16 | productoId inexistente | 404 | ENT-001 |
| CD2-17 | visitaId inexistente | 404 | ENT-001 |
| CD2-18 | Producto **menú especial** rechazado | **400** | BUSINESS_ERROR + BAD_REQUEST |
| CD2-19 | Stock insuficiente al agregar | 409 | NEG-001 |
| CD2-20 | Acumular cantidad supera stock | 409 | NEG-001 |

### E3 — PATCH /borrador/items/{itemId} (13 casos)
| ID | Caso | HTTP |
|----|------|------|
| CD3-01 | Cambiar solo cantidad | 200 |
| CD3-02 | Cambiar solo descripción | 200 |
| CD3-03 | Cambiar cantidad y descripción | 200 |
| CD3-04 | Body vacío (no-op) | 200 |
| CD3-05 | cantidad = 0 → debe usar DELETE | 409 (NEG-002) |
| CD3-06 | Sin token | 401 |
| CD3-07 | Rol no autorizado (CLIENTE) | 403 |
| CD3-08 | cantidad < 1 (bean validation) | 400 |
| CD3-09 | cantidad > 250 | 400 |
| CD3-10 | descripcion > 500 | 400 |
| CD3-11 | itemId inexistente | 404 |
| CD3-12 | Ítem en comanda PENDIENTE (no-BORRADOR) | 409 (NEG-002) |
| CD3-13 | Nueva cantidad supera stock | 409 (NEG-001) |

### E4 — DELETE /borrador/items/{itemId} (9 casos)
| ID | Caso | HTTP | Notas |
|----|------|------|-------|
| CD4-01 | Eliminar ítem base **sin** modificados | 200 | borra solo ese |
| CD4-02 | Eliminar ítem base **con** modificados → **cascada** | 200 | borra base + todos los modificados del mismo productoId |
| CD4-03 | Eliminar SOLO ítem modificado (base permanece) | 200 | borra esa fila |
| CD4-04 | Eliminar último ítem → comanda se elimina | 200 | comanda BORRADOR queda sin ítems → DELETE |
| CD4-05 | Eliminar **par menú especial** → borra cocina+barra | 200 | match por `menuGrupo` |
| CD4-06 | Sin token | 401 | |
| CD4-07 | Rol no autorizado (CLIENTE) | 403 | |
| CD4-08 | itemId inexistente | 404 | |
| CD4-09 | Ítem en comanda no-BORRADOR | 409 (NEG-002) | |

### E5 — POST /borrador/{comandaId}/enviar (6 casos)
| ID | Caso | HTTP |
|----|------|------|
| CD5-01 | Enviar borrador válido (transición BORRADOR → PENDIENTE, mesa ESPERA → EN_PREPARACION) | 200 |
| CD5-02 | Sin token | 401 |
| CD5-03 | Rol no autorizado (CLIENTE) | 403 |
| CD5-04 | comandaId inexistente | 404 |
| CD5-05 | Comanda ya en PENDIENTE | 409 (NEG-002) |
| CD5-06 | Comanda BORRADOR sin ítems | 409 (NEG-001) |

> **CD5-07 (stock insuficiente)** — omitido del primer ciclo: tras el envío, el stock no se decrementa (se decrementa en PENDIENTE→EN_PREPARACION), por lo que `validarStock(cantidad, cantidad)` solo falla si el producto bajó de stock entre el agregar y el enviar. Requiere ajuste manual del stock vía SQL. Documentar como caso pendiente.

### E6 — DELETE /borrador?visitaId={} (6 casos)
| ID | Caso | HTTP |
|----|------|------|
| CD6-01 | Cancelar formulario existente | 200 |
| CD6-02 | Cancelar sin borrador (idempotente) | 200 |
| CD6-03 | Sin token | 401 |
| CD6-04 | Rol no autorizado (CLIENTE) | 403 |
| CD6-05 | visitaId inexistente | 404 |
| CD6-06 | visitaId ausente | 400 |

### E7 — PATCH /borrador/{comandaId}/notas (7 casos)
| ID | Caso | HTTP |
|----|------|------|
| CD7-01 | Actualizar notas con texto | 200 |
| CD7-02 | notas = null borra | 200 |
| CD7-03 | Sin token | 401 |
| CD7-04 | Rol no autorizado (CLIENTE) | 403 |
| CD7-05 | notas > 500 chars | 400 |
| CD7-06 | comandaId inexistente | 404 |
| CD7-07 | Comanda no BORRADOR | 409 (NEG-002) |

**Total automated:** 7 + 20 + 13 + 9 + 6 + 6 + 7 = **68 requests**.

---

## 4. Manual collection (1 entrada por endpoint)

| # | Archivo |
|---|---------|
| 90-01 | Obtener Borrador – MESERO |
| 90-02 | Agregar Ítem (plato) – MESERO |
| 90-03 | Modificar Ítem – MESERO |
| 90-04 | Eliminar Ítem – MESERO |
| 90-05 | Enviar a Producción – MESERO |
| 90-06 | Cancelar Formulario – MESERO |
| 90-07 | Actualizar Notas – MESERO |

**Total manual:** 7 requests.

---

## 5. Orden de ejecución / commits

Un commit por endpoint (8 commits totales contando manual). Patrón:
```
test(postman): cobertura automated GET /api/comandas/borrador (E1)
```
Final:
```
test(postman): casos manuales 90-XX para HU-05 modificar comanda
```

### Fases
1. Crear `definition.yaml` por endpoint (7 archivos) — establece colección + login script.
2. Implementar requests automated por endpoint en orden E1→E7.
3. Implementar manual (90-01 a 90-07).
4. Verificación final: estructura completa, naming consistente, todas las URLs usan `tmp` prefix.

---

## 6. SQL seed — IDs fijos para los tests

Archivo nuevo: `backend/postman/seed-hu05-comandas.sql`.

**Estrategia:** crear visitas, comandas e ítems con IDs altos (rango `9000+`) que no colisionan con `V2/V3` migration, e idempotentes (se pueden re-ejecutar). El usuario corre este SQL antes de ejecutar la colección.

### IDs fijos a crear

| ID | Recurso | Propósito |
|----|---------|-----------|
| `9001` | **Visita activa** con mesa en ESPERA + borrador con ítems cocina+barra | E1 happy path, E3, E4, E6, E7 |
| `9002` | **Visita activa sin borrador** | E1 vacío (CD1-02), E6 idempotente (CD6-02) |
| `9099` | **Visita inexistente** (no se crea) | 404 |
| `9101` | **Producto PLATO** activo, stock=100 | agregar plato |
| `9102` | **Producto BEBIDA** activo, stock=100 | agregar bebida |
| `9103` | **Producto menú especial** (`menu_especial=true`) | CD2-18 → 400 |
| `9104` | **Producto PLATO stock=2** | CD2-19, CD2-20, CD3-13 |
| `9201` | **Comanda BORRADOR cocina** (visita 9001) | E3, E4, E5, E7 |
| `9202` | **Comanda BORRADOR barra** (visita 9001) | E4-05 par menú especial |
| `9203` | **Comanda PENDIENTE** (visita 9001) | CD3-12, CD5-05, CD7-07 |
| `9204` | **Comanda BORRADOR sin ítems** (visita 9002 vacía no aplica; usar visita 9001) | CD5-06 |
| `9299` | comandaId inexistente | 404 |
| `9301` | **Item base plato** sin modificados, en 9201 | CD3-01, CD4-01 |
| `9302` | **Item base plato** con modificados (mismo productoId que 9303/9304) | CD4-02 cascada |
| `9303–9304` | Items modificados del 9302 | CD4-02, CD4-03 |
| `9305` | **Item bebida único** en 9201 | CD4-04 último ítem |
| `9306` | **Item par menú especial cocina** (`menu_grupo=GRP-9001`) | CD4-05 |
| `9307` | **Item par menú especial barra** (`menu_grupo=GRP-9001`) | CD4-05 |
| `9308` | Item en comanda PENDIENTE 9203 | CD3-12, CD4-09 |
| `9399` | itemId inexistente | 404 |

### Usuarios reutilizados
- MESERO (existe en `V3`): `mesero1@altoro.com` / `Al.Toro2026!` → token automated `meseroToken` o manual `tmpMeseroToken`.
- CLIENTE para 403: usuario CLIENTE existente.
- CAJERO para 403: usuario CAJERO existente.

### Reglas del SQL
1. `DELETE` previo de IDs 9000+ para idempotencia (cascada de FK desde `comanda_item` → `comanda` → `visita`).
2. `INSERT INTO visita ...` con IDs forzados (`OVERRIDING SYSTEM VALUE` si secuencias).
3. Linkear mesa → visita 9001 con `mesa_estado='ESPERA'` (para validar transición a EN_PREPARACION en CD5-01).
4. Mesa asignada al mesero del seed para que `mesaValidador.validarOwnership` no falle.

> **Caveat:** CD5-01 (envío válido) consume el seed: tras correrlo, la comanda 9201 pasa a PENDIENTE. Solución: re-ejecutar `seed-hu05-comandas.sql` antes de cada corrida completa de la colección. Documentar esto en `backend/postman/postman/collections/mesas_comandas/README.md`.

---

## 7. Pendientes antes de implementar

1. Confirmar que los IDs altos (`9000+`) no chocan con secuencias PostgreSQL existentes — verificar con `SELECT last_value FROM <tabla>_id_seq`.
2. Confirmar nombre exacto del campo: el plan asume `mesa_estado`, `comanda_estado`, `producto_estado`, `menu_especial` — validar contra `V1__init_schema.sql`.
3. Confirmar password de seed activo en V3 (`Al.Toro2026!` BCrypt).

---

**Listo para implementar:** matriz ampliada a 68 casos automated + 7 manual + 1 SQL seed file.
