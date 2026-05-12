# HU-05 Modificar Comanda — Implementation Plan (Backend, sin pruebas)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recomendado) o superpowers:executing-plans para implementar este plan tarea por tarea. Steps usan checkbox (`- [ ]`) para tracking.
>
> **Alcance del plan:** este documento cubre la **implementación funcional backend** de HE-03-HU-05-CA-01 a CA-17. **NO incluye** pruebas unitarias JUnit, Postman manual ni Postman automatizado — esos quedan en planes posteriores tal como acordó la usuaria. Frontend está fuera de alcance.

**Goal:** Implementar el flujo "Modificar comanda" para meseros: cargar borradores existentes (precargados desde pre-orden o vacíos), buscar productos de la carta, agregar/modificar/eliminar ítems con validación de stock en tiempo real, capturar modificaciones libres a productos a la carta (como `ComandaItem` hijos), enviar comanda a cocina y/o a barra, y cancelar formulario con limpieza de borrador. (CA-13 "Guardar como espera" se resuelve sin endpoint backend: el frontend cierra el formulario y la persistencia incremental ya queda en BORRADOR.)

**Architecture:** Se reutiliza la división "1 comanda = 1 estación" ya implementada en el plan PA-87 (split por estación). Por mesa pueden coexistir hasta dos comandas en estado `BORRADOR`: una `COCINA` (platos) y otra `BARRA` (bebidas). Cada operación de "Enviar a cocina" / "Enviar a barra" es una transición pura `BORRADOR → PENDIENTE` por estación. Las **modificaciones libres** (CA-04) se modelan como un `ComandaItem` adicional con el **mismo `producto_id`** que el ítem base, cantidad propia, precio propio (ajustable por el cajero) y `descripcion != null`. Un ítem se considera "padre" cuando `descripcion IS NULL`; "modificado" cuando `descripcion IS NOT NULL` con el mismo `producto_id` en la misma comanda. **No se introduce ninguna columna nueva** en `comanda_item`: `producto_id` y `comanda_item_precio` siguen siendo NOT NULL. La relación padre-hijo es lógica (mismo `productoId`, mismo `comanda` context, `descripcion IS NULL` vs `IS NOT NULL`) y se resuelve en el mapper y en la lógica de eliminación por query. El menú especial conserva la regla "no descuenta inventario" (ítems con `comanda_item_menu_grupo IS NOT NULL`). El response del borrador agrupa los ítems modificados en una sub-lista anidada bajo cada ítem padre (`ItemBorradorResponse.modificaciones`); el agrupamiento por `productoId` + `descripcionIsNull/NotNull` lo hace el mapper. Los WebSocket reutilizan `/topic/mesas` (refresca `tieneBorrador` y `estado`) y `/topic/visita/{visitaId}/orden` (refresca orden del cliente). Al enviar a producción se publican además: `comanda.nueva` en RabbitMQ (consumido por el bridge de impresión de tickets) y un **nuevo tópico** `/topic/estacion/{estacion}` (COCINA o BARRA) para actualizar el dashboard de la estación correspondiente. **El stock se descuenta cuando producción cambia PENDIENTE → EN_PREPARACION** (no al enviar desde el borrador); las comandas en PENDIENTE SÍ cuentan para el stock comprometido de validación.

**Tech Stack:** Spring Boot 3.5 · Java 21 · PostgreSQL 15 (Flyway V1 — modificación in-place) · RabbitMQ 3.13 · Spring WebSocket (STOMP) · MapStruct-style mappers manuales.

---

## Planning Approval Protocol (7 Secciones)

### 1. Resumen Ejecutivo

**Qué se construye:** los endpoints REST y la lógica de servicio para que un MESERO modifique la comanda asociada a una mesa con visita activa. Esto cubre desde acceder al formulario (precargando o no datos de pre-orden) hasta enviar la comanda a producción o descartar el borrador. Toda la persistencia se realiza sobre comandas con estado `BORRADOR`; el envío a producción transiciona la comanda a `PENDIENTE` y dispara el flujo HE-04 (pantallas de cocina/barra) ya existente.

**Qué problema resuelve:** hoy una mesa con pre-orden tiene comandas `BORRADOR` creadas por `MesaAsignarService` (PA-87) pero **no existe forma de leerlas, editarlas, agregar productos a la carta, ni enviarlas a producción**. Tampoco se permite construir una comanda "desde cero" para mesas walk-in. Sin esta HU, la cadena `reserva → mesa → producción` queda interrumpida.

**Quién lo usará:** rol `MESERO` (autenticado y dueño de la mesa) y `ADMIN` (sin restricción de ownership). Indirectamente habilita HE-04 (pantallas de cocina/barra) que ya consume `comanda.nueva` en RabbitMQ y `/topic/comandas/completado` por WebSocket.

**Excluido de este plan:**
- Pruebas unitarias (`*ServiceTest`, `*MapperTest`, `*ControllerTest`, `*RepositoryTest`, `*EntityTest`).
- Pruebas Postman manuales (1 request por endpoint) y colección automatizada.
- Frontend (Angular): formulario de comanda, búsqueda en buscador, modal de confirmación, redirecciones, animaciones.
- Asignación de **precio diferencial** a ítems modificados — en HU-05 los ítems modificados se persisten con `precio = producto.precio` (igual que el ítem base). Cobro diferencial por cajero es HU futura.
- Tareas de migración de la BD productiva (Flyway V1 se modifica in-place según regla del repo).
- HE-03-HU-04-CA-09 (resumen de items enviados a producción en el mapa) — no se altera; ya se actualiza vía `/topic/mesas`.

### 2. Lógica de Implementación

#### 2.1. Endpoints (8 nuevos: 7 en `ComandaController` + 1 en `ProductoController`)

| # | Método | Ruta | CA | Descripción |
|---|--------|------|----|-------------|
| 1 | GET | `/api/comandas/borrador?visitaId=` | CA-01 | Devuelve el formulario de comanda: comandas BORRADOR (cocina+barra) con sus items, modificaciones de menú y modificaciones libres (anidadas como sub-lista por padre), total acumulado. Si no hay borradores, devuelve estructura vacía. |
| 2 | GET | `/api/productos/buscar?q=` | CA-02, CA-03 | Búsqueda parcial case-insensitive sobre productos NO menú especial, NO inactivos. Devuelve `productoId, nombre, precio, categoria, stockActual`. **Vive en el módulo `inventario`** porque expande `ProductoController`. |
| 3 | POST | `/api/comandas/borrador/items` | CA-03, CA-04 | Agrega un ítem a la comanda borrador correspondiente (PLATO→COCINA, BEBIDA→BARRA). Si la comanda BORRADOR de esa estación no existe aún, la crea. Acepta campo opcional `descripcion` (CA-04). Si `descripcion != null`, persiste un ítem modificado con el **mismo** `productoId`, `precio = producto.precio` y la descripción indicada — no se añade ninguna columna nueva. Si ya existe un ítem con ese `productoId` + esa `descripcion`, aumenta su cantidad. Valida stock. |
| 4 | PATCH | `/api/comandas/borrador/items/{itemId}` | CA-05, CA-06, CA-17 | Modifica `cantidad` y/o `descripcion` del ítem (sirve tanto para items padre como para hijos modificación libre). Valida stock y techo 250. |
| 5 | DELETE | `/api/comandas/borrador/items/{itemId}` | CA-07 | Elimina un ítem. Si el ítem **base** (`descripcion=null`) se elimina, también se borran todos los ítems con el mismo `productoId` y `descripcion != null` en la misma comanda. Si el ítem modificado (`descripcion!=null`) se elimina, solo se borra ese ítem. Si la comanda queda sin ítems, se elimina. |
| 6 | POST | `/api/comandas/borrador/{comandaId}/enviar` | CA-09 | Transiciona la comanda BORRADOR (de la estación que lleva) a PENDIENTE. Asigna `comandaFechaHoraInicio = now()`, descuenta stock (excepto ítems con `menu_grupo IS NOT NULL`), publica `comanda.nueva` en RabbitMQ y eventos WS. Las notas de la comanda se envían a producción con ella (ya persistidas en el BORRADOR vía endpoint 8). |
| 7 | DELETE | `/api/comandas/borrador?visitaId=` | implícito CA-01 | Botón "Cancelar formulario con información" — elimina **todas** las comandas BORRADOR de la visita descartando los cambios. Es la contraparte de CA-10 ("Cerrar" guarda; "Cancelar" descarta). WS `/topic/mesas` se publica para retirar `tieneBorrador`. |
| 8 | PATCH | `/api/comandas/borrador/{comandaId}/notas` | CA-01 | Actualiza el campo `notas` de la comanda BORRADOR indicada (COCINA o BARRA). Se persiste en tiempo real, igual que los ítems, para que CA-10 ("Cerrar") conserve las notas. Devuelve `BorradorComandaResponse`. |

> **Nota CA-08 (habilitación de botones):** frontend usa los flags `puedeEnviarCocina` / `puedeEnviarBarra` del `GET /borrador` para habilitar/deshabilitar el botón "Enviar a producción". No requiere endpoint dedicado.

> **Nota CA-10 ("Guardar sin enviar"):** el mesero pulsa "Cerrar" → el frontend regresa al mapa sin llamar al backend. Las notas y los ítems ya están persistidos en BORRADOR (persistencia incremental). El ícono `tieneBorrador` del mapa permanece visible.

> **Nota CA-11 (recarga de página):** el frontend redirige al mapa al recargar. El backend no hace nada especial; el BORRADOR sigue existiendo y el mesero puede volver a editarlo.

#### 2.2. Flujo de cada endpoint

##### 2.2.1. `GET /api/comandas/borrador?visitaId=` (CA-01)

`ComandaController.obtenerBorrador` → `ComandaBorradorService.obtenerBorrador`:

1. Validar visita existe y `visitaFechaHoraFin = null`. Lanzar `BusinessException(ENTITY_NOT_FOUND, ...)` si no.
2. Validar ownership: el mesero autenticado debe ser `mesa.getMesero().usuario.email` o ser `ADMIN`. (Patrón ya usado en `MesaService.obtenerItemsProduccion`.)
3. Cargar `List<Comanda>` con `findByVisita_VisitaIdAndComandaEstado(visitaId, BORRADOR)`.
4. Por cada comanda, cargar todos los `ComandaItem` con `findByComanda_ComandaIdOrderByProductoNombreAsc` (nuevo método repo). La consulta devuelve tanto ítems base (`descripcion=null`) como ítems modificados (`descripcion!=null`). Cargar lazy:
   - `comanda_menu_modificacion` (si menú especial).
5. `ComandaBorradorMapper.toBorradorResponse(mesa, comandasBorrador, totalAcumulado)`:
   - Separar ítems base (`descripcion=null`) de ítems modificados (`descripcion!=null`, mismo `productoId`).
   - Agrupar por categoría (PLATOS → COCINA, BEBIDAS → BARRA).
   - Para cada ítem base, buscar en la misma comanda los ítems con mismo `productoId` y `descripcion!=null` y anidarlos en `ItemBorradorResponse.modificaciones`.
   - Para ítems con `comandaItemMenuGrupo`, fusionar pares COCINA+BARRA en un solo `ItemBorradorResponse` con campo `bebida` (mismo patrón de `PreOrdenMapper` ya existente).
   - Calcular `subtotal = precioUnitario * cantidad` por ítem (base y modificado por separado).
   - Usar `totalAcumulado` recibido como parámetro (calculado en el servicio).
6. Devolver `BorradorComandaResponse`.

##### 2.2.8. `PATCH /api/comandas/borrador/{comandaId}/notas` (CA-01)

`ComandaBorradorService.actualizarNotas(comandaId, NotasRequest, auth)`:

1. Cargar `Comanda` por `comandaId`. Lanzar 404 si no existe.
2. Validar `comanda.estado == BORRADOR`; lanzar `BusinessException(INVALID_STATE, ...)` si no.
3. `Long visitaId = comanda.getVisita().getVisitaId()`. Validar ownership.
4. Aplicar: `comanda.setComandaNotas(req.getNotas())`. Persistir.
5. No publicar WS `/topic/mesas` — las notas no cambian el ícono ni el estado de la mesa.
6. Devolver `obtenerBorradorInterno(mesa)`.

> `Comanda.comandaNotas` ya existe en la entidad como `TEXT` nullable. No se requiere migración.

##### 2.2.2. `GET /api/productos/buscar?q=` (CA-02, CA-03)

`ProductoController.buscarProductos` → `ProductoService.buscarProductos`:

1. Validar `q.length() >= 1` (frontend ya lo hace; backend permisivo).
2. Llamar `productoRepository.findByNombreContainingIgnoreCaseAndEstadoAndNoEsMenu(q, ACTIVO)` (nueva query JPQL).
3. Mapear con `ProductoMapper.toBusquedaResponse` a `List<ProductoBusquedaResponse>` ordenado alfabéticamente por nombre (la query ya ordena).

##### 2.2.3. `POST /api/comandas/borrador/items` (CA-03, CA-04, CA-06, CA-17)

`ComandaBorradorService.agregarItem(visitaId, AgregarItemRequest, emailMesero)`:

1. Validar visita activa + ownership.
2. Validar `cantidad ∈ [1, 250]`.
3. Cargar `Producto` por `productoId`. Lanzar 404 si no existe o está INACTIVO.
4. Rechazar si `producto.menuEspecial = true` (los menús se cargan vía pre-orden, no se agregan manualmente — confirmado en brainstorming).
5. **Validación de stock (CA-06):** si `producto.stockActual` no es null y la suma de unidades comprometidas + nueva cantidad excederían el stock, lanzar `BusinessException(INSUFFICIENT_STOCK, "Solo hay X unidades disponibles de este producto", 400)`.
   - Cálculo: `stockComprometido = sum(comanda_item.cantidad WHERE producto_id = X AND comanda.comanda_estado IN (BORRADOR, PENDIENTE))`. Solo BORRADOR y PENDIENTE porque el stock se descuenta al pasar de PENDIENTE → EN_PREPARACION (en HE-04); EN_PREPARACION, LISTO y COMPLETADO ya descontaron `stockActual` y estarían doble-contados. La fórmula: `disponible = stockActual - (comprometido - cantidadAnteriorDelItemActual)`.
6. **Resolver estación:**
   - `categoria=PLATO` → `EstacionComanda.COCINA`.
   - `categoria=BEBIDA` → `EstacionComanda.BARRA`.
   - `categoria=OTRO` → rechazar (PA-87 ya documenta su deprecación; backend no debe aceptar nuevos OTRO).
7. **Obtener-o-crear comanda BORRADOR de esa estación:**
   - Buscar `findByVisita_VisitaIdAndComandaEstadoAndComandaEstacion(visitaId, BORRADOR, estacion)` (nuevo método). Si existe, usar.
   - Si no existe, crear `Comanda(visita=v, reserva=v.reserva, estado=BORRADOR, estacion, fechaHoraInicio=null, notas=null)` y persistir.
8. **Si ya existe un `ComandaItem` del mismo `productoId` y misma `descripcion` en la comanda**, aumentar su cantidad (CA-03). Si la descripción difiere, crear ítem nuevo. Un ítem con `descripcion = null` es el "base"; un ítem con `descripcion != null` es el "modificado" (aparece anidado bajo el base en el response).
9. Persistir `ComandaItem(producto, cantidad, precio = producto.precio, descripcion, menu_grupo=null)`.
10. **WebSocket:** publicar `MesaWsPublisher.publicarActualizacionMesa(visitaId, ACTUALIZAR)` para que el mapa refresque `tieneBorrador`.
12. Devolver `BorradorComandaResponse` (mismo DTO que GET) — el frontend reusa el render.

##### 2.2.4. `PATCH /api/comandas/borrador/items/{itemId}` (CA-05, CA-06, CA-07, CA-17)

`ComandaBorradorService.modificarItem(itemId, ModificarItemRequest, emailMesero)`:

1. Cargar `ComandaItem` y validar:
   - existe, su comanda está en BORRADOR, su comanda pertenece a la visita del mesero (ownership).
2. Si `cantidad == 0` → **rechazar con código `NEG-002`** y mensaje "Use DELETE para eliminar items" (la confirmación de CA-07 la maneja el frontend; el backend no admite cantidad 0).
3. Si `cantidad > 250` → `VAL-001` "La cantidad máxima por producto/bebida es de 250" (CA-17).
4. Si `cantidad < 1` → `VAL-001`.
5. Re-validar stock (CA-05/CA-06) restando la cantidad anterior del cómputo de comprometido.
6. Aplicar cambios y persistir.
7. WS `/topic/mesas` (`ACTUALIZAR`).
8. Devolver `BorradorComandaResponse`.

##### 2.2.5. `DELETE /api/comandas/borrador/items/{itemId}` (CA-07, CA-08)

`ComandaBorradorService.eliminarItem(itemId, emailMesero)`:

1. Cargar `ComandaItem` + validar (igual a §2.2.4).
2. Si el item es **parte de un par menú especial** (`comanda_item_menu_grupo != null`), eliminar el par completo (cocina + barra) — patrón ya existe en `PreOrdenGestor.eliminarPreOrdenExistente`.
3. Si el item **no tiene `descripcion` (es el base)**: eliminar también todos los `ComandaItem` de la misma comanda con el mismo `productoId` que SÍ tienen `descripcion` (los modificados). Luego eliminar el item base. Cascade de `comanda_menu_modificacion` se mantiene (orphanRemoval existente).
4. Si el item **tiene `descripcion` (es un modificado)**: eliminar solo ese item. No afecta ni al base ni a otros modificados del mismo producto.
5. Si la comanda quedó sin ítems base (`descripcion IS NULL`), eliminar la `Comanda`.
5. Si la visita queda sin comandas BORRADOR, publicar WS `/topic/mesas` con `ACTUALIZAR` (para retirar el ícono de borrador).
6. Devolver `BorradorComandaResponse` actualizado.

##### 2.2.6. `POST /api/comandas/borrador/{comandaId}/enviar` (CA-10, CA-11, CA-12)

`ComandaBorradorService.enviarAProduccion(comandaId, emailMesero)`:

1. Cargar `Comanda` + validar:
   - estado = BORRADOR,
   - tiene al menos 1 item (`!items.isEmpty()`) — refuerza CA-09 backend-side.
   - ownership (mesero asignado a la mesa).
2. **Validación final de stock (CA-10/CA-11)** sobre los items `menu_grupo IS NULL`: si algún producto del lote excede stock disponible (`stockActual - sum(PENDIENTE)`), rechazar con `INSUFFICIENT_STOCK` y detallar el producto. **No se descuenta stock aquí** — el descuento ocurre en HE-04 cuando producción transiciona PENDIENTE → EN_PREPARACION.
3. Setear `comanda.estado = PENDIENTE`, `comanda.fechaHoraInicio = LocalDateTime.now()`. Persistir. **No se decrementa `stockActual` aquí** — el descuento real ocurre en HE-04 cuando producción transiciona PENDIENTE → EN_PREPARACION.
4. **Mesa:** si `mesa.estado == ESPERA`, transicionar a `EN_PREPARACION` y persistir.
5. **Side effects:**
   - Publicar a RabbitMQ: `rabbitTemplate.convertAndSend(EXCHANGE, RK_COMANDA_NUEVA, ComandaNuevaMessage(comandaId, estacion, visitaId))` — consumido por el **bridge de impresión de tickets** (cola `q.comanda.produccion`). **No** actualiza pantallas de producción.
   - **WS `/topic/estacion/{estacion}`** (nuevo): publicar `ComandaEstacionWsMessage(comandaId, visitaId, estacion, items)` al tópico correspondiente a la estación de la comanda enviada (COCINA o BARRA). Las pantallas de producción se suscriben a su propio tópico para recibir el listado actualizado.
   - WS `/topic/mesas` con `ACTUALIZAR` (refresca estado de mesa y `tieneBorrador`).
   - WS `/topic/visita/{visitaId}/orden` con la lista actualizada de items y `total` — el cliente ve su orden activa.
7. Devolver `BorradorComandaResponse` con la comanda enviada ya removida (queda solo la otra estación si aplica). El frontend recibe `redirigir=true` cuando ambas comandas borrador quedan vacías.

##### 2.2.7. `DELETE /api/comandas/borrador?visitaId=` (CA-15)

`ComandaBorradorService.cancelarFormulario(visitaId, emailMesero)`:

1. Validar visita activa + ownership.
2. Cargar `List<Comanda>` BORRADOR.
3. Eliminar todas — el cascade existente `Comanda → ComandaItem` limpia ítems base e ítems modificados por igual (ambos son filas normales en `comanda_item`, sin FK padre-hijo). `ComandaMenuModificacion` se limpia por `orphanRemoval` ya existente. Las notas de cada `Comanda` se descartan junto con la entidad.
4. WS `/topic/mesas` con `ACTUALIZAR` (retira ícono de borrador).
5. Devolver mensaje "Cambios descartados".

> **CA-15 vs CA-16:** son el mismo endpoint con comportamientos:
> - Si no hay items → idempotente (DELETE devuelve OK aunque no haya nada que borrar).
> - Si hay items → confirmar en frontend antes de invocar este endpoint.
> El backend no distingue: el frontend decide si pre-confirmar.

#### 2.3. Dependencia externa: `NotificacionService.atenderCambio`

HU-05 asume que cuando el mesero llega al formulario de borrador tras atender una notificación de cambio, la comanda ya está en el estado correcto. Para que esto funcione, `NotificacionService.atenderCambio` debe implementar la siguiente lógica de merge **antes** de integrar HU-05:

1. Localizar la `Comanda` PENDIENTE de la notificación.
2. Determinar su estación (`COCINA` o `BARRA`).
3. Buscar si ya existe una `Comanda` BORRADOR para esa misma estación y visita.
4. **Si existe BORRADOR:** para cada `ComandaItem` de la PENDIENTE, buscar en el BORRADOR un ítem con el mismo `productoId` + `descripcion`. Si existe, sumar cantidades. Si no, crear ítem nuevo en el BORRADOR. Eliminar la PENDIENTE.
5. **Si no existe BORRADOR:** transicionar la PENDIENTE a BORRADOR (cambio de estado puro).
6. Marcar la `Notificacion` como `ATENDIDA` y devolver respuesta (no devuelve `BorradorComandaResponse` — el mesero re-entra al formulario con `GET /api/comandas/borrador`).

> **Scope:** esta modificación vive en `NotificacionService` (módulo `notificaciones`) y **no es parte de HU-05**. HU-05 debe documentarla como pre-condición antes de su despliegue.

#### 2.4. WebSocket: estrategia consolidada

| Tópico | Cuándo se publica | Payload | Suscriptores |
|--------|-------------------|---------|--------------|
| `/topic/mesas` (existente) | Al **agregar/modificar/eliminar** ítem, **enviar a producción**, **cancelar borrador** | `MesaWsMessage{visitaId, tipoEvento=ACTUALIZAR, nuevoEstado, timestamp}` | Mapa de mesas de todos los meseros conectados. El cliente del mapa al recibir el evento llama `GET /api/mesas` para repintar (patrón ya existente). |
| `/topic/visita/{visitaId}/orden` (existente) | Al **enviar a producción** únicamente | `VisitaActualizadaWsMessage{visitaId, items, total}` | El cliente dueño de la visita (vista "orden actual"). |
| `/topic/estacion/{estacion}` (**nuevo**) | Al **enviar a producción** — COCINA cuando la comanda es de platos; BARRA cuando es de bebidas | `ComandaEstacionWsMessage{comandaId, visitaId, estacion, items}` (DTO nuevo, en `mesas_comandas/dto/messaging/`) | Dashboard de la estación de producción correspondiente (cocina o barra). |
| `comanda.nueva` en `altoro.topic` (RabbitMQ existente) | Al **enviar a producción** | `ComandaNuevaMessage{comandaId, estacion, visitaId, fechaHoraInicio}` (DTO nuevo, en `mesas_comandas/dto/messaging/`) | Cola `q.comanda.produccion` consumida por el **bridge de impresión de tickets** — **no** por el dashboard de producción. |

**Se introduce un tópico nuevo:** `/topic/estacion/{estacion}`. El tópico se materializa en dos suscripciones distintas: `/topic/estacion/COCINA` y `/topic/estacion/BARRA`. Al enviar una comanda de platos se publica solo en COCINA; al enviar de bebidas, solo en BARRA. Razones de los demás:
- "Validación de stock en tiempo real" (CA-03/CA-05/CA-06) **no requiere WS**: la validación se ejecuta en cada llamada del backend (al agregar, modificar y enviar). El frontend re-llama tras cada acción.
- El "ícono de borrador" del mapa ya se infiere de `tieneBorrador` en `MesaMapaResponse`; basta con que el evento `/topic/mesas` dispare un re-fetch del mapa.

#### 2.4. Side effects (resumen)

- **DB:** **No se altera `comanda_item`**. `producto_id` y `comanda_item_precio` permanecen NOT NULL. La relación padre-hijo entre ítems es puramente lógica (mismo `productoId` + `descripcion IS NULL` vs `IS NOT NULL`), sin columna nueva.
- **RabbitMQ:** publicación en `comanda.nueva` al enviar a producción (RK ya existente) — consumido por bridge de impresión.
- **Stock:** el descuento de `producto.stockActual` ocurre en HE-04 (producción transiciona PENDIENTE → EN_PREPARACION), **no** en este endpoint. Las comandas PENDIENTE SÍ cuentan para el cálculo de stock comprometido en la validación.
- **Mesa:** transición `ESPERA → EN_PREPARACION` al primer envío a producción (no se toca si ya está en EN_PREPARACION).
- **WebSocket:** nuevo tópico `/topic/estacion/{estacion}` publicado al enviar, dirigido a la estación de la comanda (COCINA o BARRA).
- **Mappers:** nuevo `ComandaBorradorMapper` reusa `COMPARATOR_POR_CATEGORIA` y la lógica de fusión menu_grupo cocina+barra de `PreOrdenMapper`.

### 3. Pruebas Propuestas

**EXCLUIDAS de este plan** (acuerdo explícito de la usuaria 2026-05-08).

Para referencia, en plan posterior se cubrirán:
- Unit tests Mockito sobre `ComandaBorradorService` (todos los CA), `ComandaBorradorMapper` (fusión menu_grupo), `ComandaBorradorValidador` (stock/250), `ComandaController` (auth + ownership), `ProductoService.buscarProductos`.
- Repository test sobre `ComandaItemRepository.findByComanda_ComandaIdOrderByProductoNombreAsc` y nuevo método de stock comprometido.
- Postman manual: 1 request por endpoint (8).
- Postman automatizado: cobertura de happy path + casos de error (404, 403, 400 validación, 409 stock, 409 estado).

### 4. DTOs Structure

#### 4.1. Request DTOs (`mesas_comandas/dto/request/`)

```java
public record AgregarItemRequest(
    @NotNull Long visitaId,
    @NotNull Long productoId,
    @NotNull @Min(1) @Max(250) Integer cantidad,
    @Size(max = 500) String descripcion   // null = ítem base; valor = ítem modificado libre
) {}

public record ModificarItemRequest(
    @Min(1) @Max(250) Integer cantidad,                   // null = no cambia
    @Size(max = 500) String descripcion                   // null = no cambia
) {}

/**
 * Payload para actualizar las notas de una comanda BORRADOR. Persistencia
 * incremental: el frontend invoca cada vez que el mesero deja de escribir.
 */
@Getter @Setter
public class NotasRequest {
    /** Texto libre; {@code null} elimina las notas existentes. */
    @Size(max = 500, message = "Las notas no deben exceder 500 caracteres")
    private String notas;
}
```

#### 4.2. Response DTOs (`mesas_comandas/dto/response/`)

```java
@Getter @Builder
public class BorradorComandaResponse {
    private final Long visitaId;
    private final String mesaIdentificador;
    private final Long comandaCocinaId;        // null si no existe BORRADOR cocina
    private final Long comandaBarraId;         // null si no existe BORRADOR barra
    private final List<ItemBorradorResponse> platos;     // ordenados alfabéticamente
    private final List<ItemBorradorResponse> bebidas;    // ordenados alfabéticamente
    private final BigDecimal total;            // suma de subtotales de la visita
    private final Boolean puedeEnviarCocina;   // true si hay al menos un plato
    private final Boolean puedeEnviarBarra;    // true si hay al menos una bebida
}

@Getter @Builder
public class ItemBorradorResponse {
    private final Long comandaItemId;
    private final Long productoId;
    private final String productoNombre;
    private final String categoriaProducto;    // "PLATO" | "BEBIDA"
    private final BigDecimal precioUnitario;
    private final Integer cantidad;
    private final BigDecimal subtotal;         // precioUnitario * cantidad
    private final String descripcion;          // null = ítem base; valor = ítem modificado libre
    private final String menuGrupo;            // UUID si es menú especial; null si carta
    private final List<OpcionMenuSeleccionadaResponse> modificacionesMenu;  // si menú
    private final ItemBebidaMenuResponse bebida;        // si menú: bebida fusionada del par
    /**
     * Ítems modificados anidados — mismos ComandaItem con el mismo productoId y descripcion != null.
     * El mapper agrupa: ítem con descripcion=null es padre; ítems con descripcion!=null son hijos.
     * Vacía si no hay modificaciones asociadas a este ítem.
     */
    private final List<ItemBorradorResponse> modificaciones;
    private final Integer stockActual;         // stock disponible actual del producto (null si no se gestiona)
}
```

#### 4.3. Productos: nuevo response (`inventario/dto/response/`)

```java
@Getter @Builder
public class ProductoBusquedaResponse {
    private final Long productoId;
    private final String productoNombre;
    private final BigDecimal productoPrecio;
    private final String productoCategoria;    // "PLATO" | "BEBIDA"
    private final BigDecimal stockActual;      // null si no se gestiona
}
```

#### 4.4. Messaging RabbitMQ (`mesas_comandas/dto/messaging/`)

```java
/** Payload para RabbitMQ; lo consume el bridge de impresión de tickets. */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ComandaNuevaMessage {
    private Long comandaId;
    private Long visitaId;
    private String estacion;                   // "COCINA" | "BARRA"
    private LocalDateTime fechaHoraInicio;
}
```

#### 4.5. WebSocket (`notificaciones/dto/ws/`)

> Convive con los demás `*WsMessage` ya existentes (`AsistenciaAtendidaWsMessage`, `ComandaCompletadaWsMessage`, `VisitaActualizadaWsMessage`, etc.). No se introduce un paquete `messaging/` para WS dentro de `mesas_comandas` para preservar la convención.

```java
/**
 * Payload para WebSocket /topic/estacion/{estacion}; actualiza el dashboard
 * de la estación de producción cuando una comanda pasa a PENDIENTE.
 */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ComandaEstacionWsMessage {
    private Long comandaId;
    private Long visitaId;
    private String estacion;                   // "COCINA" | "BARRA"
    private List<ItemBorradorResponse> items;
}
```

### 5. Controller Access Rules

Todos los endpoints están en `ComandaController` (nuevo) bajo `/api/comandas/**` salvo `GET /api/productos/buscar` que vive en `ProductoController` existente.

| Endpoint | `@PreAuthorize` | Ownership |
|----------|-----------------|-----------|
| GET `/api/comandas/borrador?visitaId=` | `hasAnyRole('MESERO','ADMIN')` | MESERO debe ser dueño de la mesa; ADMIN omite |
| POST `/api/comandas/borrador/items` | `hasAnyRole('MESERO','ADMIN')` | MESERO dueño |
| PATCH `/api/comandas/borrador/items/{itemId}` | `hasAnyRole('MESERO','ADMIN')` | MESERO dueño (resuelto por `comandaItem.comanda.visita.mesa.mesero`) |
| DELETE `/api/comandas/borrador/items/{itemId}` | `hasAnyRole('MESERO','ADMIN')` | MESERO dueño |
| POST `/api/comandas/borrador/{comandaId}/enviar` | `hasAnyRole('MESERO','ADMIN')` | MESERO dueño |
| DELETE `/api/comandas/borrador?visitaId=` | `hasAnyRole('MESERO','ADMIN')` | MESERO dueño |
| PATCH `/api/comandas/borrador/{comandaId}/notas` | `hasAnyRole('MESERO','ADMIN')` | MESERO dueño (resuelto por `comanda.visita.mesa.mesero`) |
| GET `/api/productos/buscar?q=` | `isAuthenticated()` | n/a (lectura de catálogo) |

**Patrón de ownership:** se agrega un método `validarOwnership(Long visitaId, Authentication auth)` al `MesaValidador` existente (en `mesas_comandas/service/`). Recibe el contexto de seguridad y la visita, replica la lógica embebida hoy en `MesaService.obtenerItemsProduccion`, lanza `BusinessException(ACCESS_DENIED, ...)` con HTTP 403 cuando el principal no es ADMIN ni el mesero asignado, y devuelve la `Mesa` cargada para que los servicios consumidores no la vuelvan a buscar. La lógica inline original de `MesaService.obtenerItemsProduccion` se refactoriza para llamar al método nuevo.

### 6. Functional Clarifications

> **Pendientes de confirmación explícita por la usuaria antes de comenzar implementación.**

1. **CA-04 precio:** la modificación libre se persiste con `precio = producto.precio` (mismo que el ítem base). El **total acumulado** SÍ incluye las modificaciones libres. El cobro diferencial (cajero ajusta precio en cierre de cuenta) es HU futura, fuera de este plan. ¿Confirmar?

2. **CA-09 / CA-13 "Guardar como espera":** se elimina como endpoint backend. Los cambios de items se persisten incrementalmente (cada agregar/modificar/eliminar es un endpoint independiente). El botón "Guardar como espera" del frontend simplemente cierra el formulario y vuelve al mapa; no llama al backend. ¿Aceptar?

3. **CA-15 alcance del descarte:** "Cancelar" elimina **todas** las comandas BORRADOR de la visita (cocina + barra). Esto incluye los items que vinieron precargados desde la pre-orden (porque PA-87 ya las transicionó de PRE_RESERVA a BORRADOR al asignar mesa). Es decir, **una vez se asigna mesa, la pre-orden original ya no es recuperable** por este endpoint. ¿Es la intención?

4. **CA-10/CA-11 enviar parcial:** si una comanda COCINA tiene N platos pero solo M tienen stock suficiente, el endpoint **rechaza el envío completo** (transacción atómica). No envía un subconjunto. ¿Confirmar?

5. **Stock comprometido:** la query suma cantidades de items con `comanda_estado IN (BORRADOR, PENDIENTE)` únicamente. EN_PREPARACION, LISTO y COMPLETADO ya decrementaron `stockActual` (descuento ocurre en PENDIENTE → EN_PREPARACION en HE-04), por lo que incluirlos sería doble conteo. La fórmula: `disponible = stockActual - sum(BORRADOR + PENDIENTE) + cantidadAnteriorDelItemActual`. ¿Confirmar?

6. **Búsqueda CA-02:** retorna **todos** los productos cuyo nombre contenga `q` (case-insensitive), sin paginar y sin límite máximo. Productos del menú especial **se excluyen** del buscador (los menús se cargan vía pre-orden, no se agregan manualmente al borrador). ¿Confirmar?

7. **CA-17 techo 250:** el límite aplica tanto a la `cantidad` del producto padre como a la `cantidad` del hijo modificación libre (mismo Bean Validation `@Max(250)`). ¿Confirmar?

8. **Mesa estado tras enviar a producción:** se transiciona `ESPERA → EN_PREPARACION` al primer envío. Se **mantiene** `EN_PREPARACION` aunque el envío sea parcial (solo cocina, queda barra en BORRADOR). ¿Aceptar?

9. **ADMIN ownership:** ADMIN puede operar sobre cualquier mesa sin ser su mesero asignado, igual que en `MesaService.obtenerItemsProduccion`. ¿Confirmar?

### 7. Scope Confirmation

#### Incluye
- 8 endpoints REST nuevos (7 en `ComandaController` + 1 en `ProductoController`).
- **Sin cambio de esquema en `comanda_item`**: `producto_id` y `comanda_item_precio` permanecen NOT NULL; no se agrega ninguna columna nueva.
- Nuevo tópico WebSocket `/topic/estacion/{estacion}` (COCINA o BARRA) para dashboards de producción.
- 3 DTOs request, 4 DTOs response (incluye `OpcionMenuSeleccionadaResponse` local), 1 DTO RabbitMQ, 1 DTO WebSocket en `notificaciones/dto/ws/`.
- 1 servicio nuevo (`ComandaBorradorService`) + 1 validador nuevo (`ComandaBorradorValidador`) + 1 publisher nuevo (`EstacionWsPublisher`). Ownership se delega al `MesaValidador` existente (método nuevo `validarOwnership`).
- 1 controller nuevo (`ComandaController`) + 1 endpoint en `ProductoController` existente.
- 1 mapper nuevo (`ComandaBorradorMapper`) — agrupa ítems modificados (mismo `productoId` + `descripcion != null`) bajo su ítem base; recibe `totalAcumulado` como parámetro para el total acumulado de la visita.
- 5 nuevos métodos repositorio: `ComandaRepository` (+1), `ComandaItemRepository` (+3: `findByComanda...DescripcionIsNull`, `findByComanda...DescripcionIsNotNull`, `sumTotalActivosByVisita`), `ProductoRepository` (+1).
- Publicación a RabbitMQ `comanda.nueva` y a tópicos WS `/topic/mesas` y `/topic/visita/{visitaId}/orden` (todos existentes).
- Javadoc completo + comentarios inline en pasos no obvios.

#### NO incluye (planes posteriores)
- Pruebas unitarias (JUnit + Mockito).
- Pruebas Postman (manual + automatizada).
- Frontend Angular (formulario, buscador, modales, redirecciones, animaciones).
- Precio diferencial de ítems modificados (se persisten con `precio = producto.precio`; cobro diferencial por cajero es HU futura).
- Limpieza de `CategoriaProducto.OTRO` (PA-87 ya documenta deprecación).
- Endpoint admin `obtenerTodasLasComandas`.

---

## File Structure

### Archivos a CREAR

```
backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/
├── controller/
│   └── ComandaController.java                                     # 7 endpoints (sin GET productos)
├── dto/
│   ├── request/
│   │   ├── AgregarItemRequest.java
│   │   ├── ModificarItemRequest.java
│   │   └── NotasRequest.java
│   ├── response/
│   │   ├── BorradorComandaResponse.java
│   │   ├── ItemBorradorResponse.java                              # auto-referenciado en campo modificaciones
│   │   ├── ItemBebidaMenuResponse.java                            # bebida fusionada del par menú
│   │   └── OpcionMenuSeleccionadaResponse.java                    # local en mesas_comandas (no se importa de reservas)
│   └── messaging/
│       └── ComandaNuevaMessage.java                               # RabbitMQ payload (bridge impresión)
├── mapper/
│   └── ComandaBorradorMapper.java                                 # incluye fusión menu_grupo + agrupación base/modificados
└── service/
    ├── ComandaBorradorService.java                                # lógica principal del flujo HU-05
    ├── ComandaBorradorValidador.java                              # stock, techo 250, resolución de estación
    └── EstacionWsPublisher.java                                   # publica en /topic/estacion/{estacion}

backend/src/main/java/co/edu/unicauca/backend/modules/notificaciones/
└── dto/ws/
    └── ComandaEstacionWsMessage.java                              # WS payload /topic/estacion/{estacion} (junto a los demás *WsMessage)
```

### Archivos a MODIFICAR

```
backend/src/main/java/co/edu/unicauca/backend/
├── modules/mesas_comandas/
│   ├── entity/ComandaItem.java                                    # sin cambios de esquema; modelo lógico: base=descripcion IS NULL, modificado=mismoProductoId+descripcion IS NOT NULL
│   ├── service/
│   │   ├── MesaValidador.java                                     # +validarOwnership(visitaId, auth)
│   │   └── MesaService.java                                       # obtenerItemsProduccion delega a mesaValidador.validarOwnership
│   └── repository/
│       ├── ComandaRepository.java                                 # +findByVisita_VisitaIdAndComandaEstadoAndComandaEstacion
│       └── ComandaItemRepository.java                             # +sumCantidadComprometidaByProducto; +findByComanda...OrderBy; +findByComanda...DescripcionIsNotNull; +findByComanda...Descripcion; +sumTotalActivosByVisita
└── modules/inventario/
    ├── controller/ProductoController.java                         # +GET /buscar
    ├── service/ProductoService.java                               # +buscarProductos
    ├── repository/ProductoRepository.java                         # +findByNombreContainingIgnoreCaseAndEstadoAndNoEsMenu
    ├── mapper/ProductoMapper.java                                 # +toBusquedaResponse
    └── dto/response/ProductoBusquedaResponse.java                 # nuevo
```

### Migración Flyway

```
backend/src/main/resources/db/migration/
└── V1__init_schema.sql                                            # se modifica in-place (regla del repo)
```

**No se altera `comanda_item`.**

El modelo de ítems modificados (CA-04) es puramente lógico: `producto_id` y `comanda_item_precio` siguen NOT NULL. Un ítem base tiene `comanda_item_descripcion = NULL`; un ítem modificado tiene el mismo `producto_id` en la misma comanda con `comanda_item_descripcion != NULL`. No se añade ninguna columna ni FK adicional.

La única modificación en `V1__init_schema.sql` es agregar `'BORRADOR'` al CHECK constraint de `comanda.comanda_estado` (cubierto en Task 1).

```sql
-- Único cambio requerido en comanda_item: ninguno.
-- Agregar BORRADOR al CHECK de comanda.comanda_estado (ver Task 1).
```

---

## Tasks (implementación, sin TDD)

> Cada tarea es self-contained. Tras cada bloque, ejecutar `mvn -f backend/pom.xml clean compile -q` y verificar que compila. **No se incluyen pasos de test** porque la usuaria los excluyó del plan.

### Task 1: Verificar enum EstadoComanda incluye BORRADOR

**Files:**
- Verify: `backend/src/main/java/co/edu/unicauca/backend/shared/enums/EstadoComanda.java`
- Verify: `backend/src/main/resources/db/migration/V1__init_schema.sql`

> **No se requiere ningún cambio de esquema en `comanda_item`.** El modelo de modificaciones usa el campo `comanda_item_descripcion` existente y el mismo `producto_id`: un ítem base tiene `descripcion = null`; un ítem modificado tiene `descripcion != null` con el mismo `producto_id` en la misma comanda. Ambos son `NOT NULL` en DB y permanecen así.

- [ ] **Step 1: Verificar EstadoComanda.BORRADOR y columna en V1**

`EstadoComanda.BORRADOR` ya existe en el enum. Verificar que el CHECK constraint de `comanda.comanda_estado` en `V1__init_schema.sql` incluya `'BORRADOR'`. Si no, agregarlo:

```sql
-- CHECK (comanda_estado IN ('PRE_RESERVA','BORRADOR','PENDIENTE','EN_PREPARACION','LISTO','COMPLETADO'))
```

- [ ] **Step 2: Crear EstacionWsPublisher**

```java
package co.edu.unicauca.backend.modules.mesas_comandas.service;

import co.edu.unicauca.backend.modules.notificaciones.dto.ws.ComandaEstacionWsMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Publicador WebSocket para los dashboards de producción por estación.
 *
 * <p>Publica en {@code /topic/estacion/COCINA} o {@code /topic/estacion/BARRA}
 * cuando una comanda BORRADOR transiciona a PENDIENTE. Cada pantalla de cocina
 * o barra suscribe únicamente el tópico de su estación.
 */
@Service
@RequiredArgsConstructor
public class EstacionWsPublisher {

    /** Cliente STOMP que envía el payload al broker WebSocket. */
    private final SimpMessagingTemplate messagingTemplate;

    /** Prefijo común de los tópicos por estación; concatena con {@code COCINA} o {@code BARRA}. */
    private static final String TOPIC_ESTACION = "/topic/estacion/";

    /**
     * Publica la comanda enviada a producción en el tópico de su estación.
     *
     * @param mensaje payload con items y metadata de la comanda enviada
     */
    public void publicarComandaEnviada(ComandaEstacionWsMessage mensaje) {
        messagingTemplate.convertAndSend(TOPIC_ESTACION + mensaje.getEstacion(), mensaje);
    }
}
```

- [ ] **Step 3: Compilar**

```bash
"C:/Program Files/Apache/Maven/bin/mvn" -f backend/pom.xml clean compile -q
```

Expected: BUILD SUCCESS sin warnings nuevos.

---

### Task 2: Repositorio + búsqueda de productos

**Files:**
- Modify: `backend/src/main/java/co/edu/unicauca/backend/modules/inventario/repository/ProductoRepository.java`
- Modify: `backend/src/main/java/co/edu/unicauca/backend/modules/inventario/service/ProductoService.java`
- Modify: `backend/src/main/java/co/edu/unicauca/backend/modules/inventario/mapper/ProductoMapper.java`
- Modify: `backend/src/main/java/co/edu/unicauca/backend/modules/inventario/controller/ProductoController.java`
- Create: `backend/src/main/java/co/edu/unicauca/backend/modules/inventario/dto/response/ProductoBusquedaResponse.java`

- [ ] **Step 1: Crear DTO ProductoBusquedaResponse**

```java
package co.edu.unicauca.backend.modules.inventario.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * Resultado de búsqueda de productos para el formulario de modificar comanda.
 *
 * <p>El catálogo de búsqueda excluye productos con {@code menuEspecial=true}
 * porque los menús especiales se cargan vía pre-orden, no manualmente.
 */
@Getter @Builder
public class ProductoBusquedaResponse {
    /** Identificador del producto. */
    private final Long productoId;
    /** Nombre del producto. */
    private final String productoNombre;
    /** Precio unitario vigente en el catálogo. */
    private final BigDecimal productoPrecio;
    /** Categoría del producto: {@code "PLATO"} o {@code "BEBIDA"}. */
    private final String productoCategoria;
    /**
     * Stock disponible actual del producto. {@code null} cuando el producto no
     * gestiona stock (servicios, preparaciones ad-hoc).
     */
    private final BigDecimal stockActual;
}
```

- [ ] **Step 2: Agregar query al repositorio**

Insertar en `ProductoRepository.java`:

```java
/**
 * Búsqueda parcial case-insensitive por nombre, excluyendo menús especiales
 * y productos inactivos. Ordena alfabéticamente por nombre.
 *
 * @param nombre fragmento de nombre a buscar (no debe ser null ni vacío)
 * @param estado estado a exigir (típicamente {@code ACTIVO})
 * @return productos coincidentes ordenados por nombre asc
 */
@Query("SELECT p FROM Producto p " +
       "WHERE p.productoEstado = :estado " +
       "AND (p.menuEspecial IS NULL OR p.menuEspecial = false) " +
       "AND LOWER(p.productoNombre) LIKE LOWER(CONCAT('%', :nombre, '%')) " +
       "ORDER BY p.productoNombre ASC")
List<Producto> buscarPorNombreSinMenu(@Param("nombre") String nombre,
                                      @Param("estado") EstadoGenerico estado);
```

- [ ] **Step 3: Mapper toBusquedaResponse**

En `ProductoMapper.java` agregar:

```java
/**
 * Convierte un {@link Producto} a {@link ProductoBusquedaResponse} para la
 * respuesta del buscador del formulario de modificar comanda.
 *
 * @param p producto persistido a transformar
 * @return DTO con los campos de búsqueda
 */
public ProductoBusquedaResponse toBusquedaResponse(Producto p) {
    return ProductoBusquedaResponse.builder()
            .productoId(p.getProductoId())
            .productoNombre(p.getProductoNombre())
            .productoPrecio(p.getProductoPrecio())
            .productoCategoria(p.getProductoCategoria().name())
            .stockActual(p.getStockActual())
            .build();
}
```

- [ ] **Step 4: Service buscarProductos**

En `ProductoService.java` agregar método:

```java
/**
 * Busca productos del catálogo por coincidencia parcial en el nombre,
 * excluyendo menús especiales y productos inactivos.
 *
 * @param q fragmento de nombre; si es nulo o blanco devuelve lista vacía
 * @return lista ordenada alfabéticamente; vacía si no hay coincidencias
 */
@Transactional(readOnly = true)
public List<ProductoBusquedaResponse> buscarProductos(String q) {
    if (q == null || q.isBlank()) {
        return List.of();
    }
    return productoRepository.buscarPorNombreSinMenu(q.trim(), EstadoGenerico.ACTIVO)
            .stream()
            .map(productoMapper::toBusquedaResponse)
            .collect(Collectors.toList());
}
```

- [ ] **Step 5: Endpoint en ProductoController**

En `ProductoController.java` agregar:

```java
/**
 * Endpoint del buscador de productos: coincidencia parcial case-insensitive
 * por nombre, excluyendo menús especiales y productos inactivos.
 *
 * @param q fragmento de nombre a buscar
 * @return productos coincidentes ordenados alfabéticamente
 */
@GetMapping("/buscar")
@PreAuthorize("isAuthenticated()")
@Operation(summary = "Buscar productos por nombre",
           description = "Búsqueda parcial case-insensitive del catálogo, excluye menús especiales")
public ResponseEntity<ApiResponse<List<ProductoBusquedaResponse>>> buscarProductos(
        @Parameter(description = "Fragmento de nombre a buscar")
        @RequestParam("q") String q) {

    List<ProductoBusquedaResponse> resultados = productoService.buscarProductos(q);
    return ResponseEntity.ok(ApiResponse.ok("Productos encontrados", resultados));
}
```

- [ ] **Step 6: Compilar**

```bash
"C:/Program Files/Apache/Maven/bin/mvn" -f backend/pom.xml clean compile -q
```

---

### Task 3: Repositorios de comanda — métodos auxiliares

**Files:**
- Modify: `backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/repository/ComandaRepository.java`
- Modify: `backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/repository/ComandaItemRepository.java`

- [ ] **Step 1: Agregar a ComandaRepository**

```java
/**
 * Localiza la comanda BORRADOR de una visita para una estación específica.
 *
 * <p>Por invariante (PA-87) puede haber a lo sumo una BORRADOR por estación.
 *
 * @param visitaId identificador de la visita
 * @param estado estado a buscar (típicamente {@code BORRADOR})
 * @param estacion estación destino (COCINA o BARRA)
 * @return Optional con la comanda; vacío si no existe
 */
Optional<Comanda> findByVisita_VisitaIdAndComandaEstadoAndComandaEstacion(
        Long visitaId, EstadoComanda estado, EstacionComanda estacion);

/**
 * Devuelve todas las comandas de una visita en un estado específico.
 * Hasta dos por visita en BORRADOR (una por estación).
 */
List<Comanda> findByVisita_VisitaIdAndComandaEstado(Long visitaId, EstadoComanda estado);
```

> Importar `EstacionComanda` y `Optional` si faltan.

- [ ] **Step 2: Agregar a ComandaItemRepository**

```java
/**
 * Suma las cantidades comprometidas de un producto que aún no se descontaron de
 * {@code stockActual}.
 *
 * <p>"Comprometido" = ítems en comandas en estado {@code BORRADOR} o
 * {@code PENDIENTE} que NO sean parte de un menú especial. Se excluyen
 * EN_PREPARACION, LISTO y COMPLETADO porque ya decrementaron {@code stockActual}
 * al transicionar PENDIENTE → EN_PREPARACION (doble conteo si se incluyen). Se
 * excluyen también los ítems con {@code comandaItemMenuGrupo IS NOT NULL}: los
 * menús especiales nunca decrementan inventario, por lo que sumarlos haría que
 * {@code disponible} se vuelva permanentemente negativo.
 *
 * <p>Fórmula de disponibilidad: {@code disponible = stockActual - comprometido + cantidadAnterior}.
 *
 * @param productoId producto a evaluar
 * @return suma de cantidades; 0 si no hay ítems comprometidos
 */
@Query("""
    SELECT COALESCE(SUM(ci.comandaItemCantidad), 0)
    FROM ComandaItem ci
    WHERE ci.producto.productoId = :productoId
    AND ci.comandaItemMenuGrupo IS NULL
    AND ci.comanda.comandaEstado IN (
        co.edu.unicauca.backend.shared.enums.EstadoComanda.BORRADOR,
        co.edu.unicauca.backend.shared.enums.EstadoComanda.PENDIENTE
    )
    """)
Long sumCantidadComprometidaByProducto(@Param("productoId") Long productoId);

/**
 * Devuelve todos los items de una comanda ordenados alfabéticamente por nombre de producto.
 * El mapper agrupa los ítems base (descripcion=null) con sus ítems modificados
 * (mismo productoId, descripcion!=null) en la sub-lista {@code modificaciones}.
 */
@Query("""
    SELECT ci FROM ComandaItem ci
    JOIN FETCH ci.producto p
    WHERE ci.comanda.comandaId = :comandaId
    ORDER BY p.productoNombre ASC, ci.comandaItemDescripcion ASC NULLS FIRST
    """)
List<ComandaItem> findByComanda_ComandaIdOrderByProductoNombreAsc(@Param("comandaId") Long comandaId);

/**
 * Busca el ítem con un {@code productoId} y descripción exactos en una comanda.
 * Lo usa el flujo de "agregar ítem" para acumular cantidad cuando ya existe un
 * ítem con la misma descripción, en vez de crear duplicados.
 *
 * @param comandaId   comanda BORRADOR destino
 * @param productoId  producto a buscar
 * @param descripcion descripción exacta (null = ítem base sin modificación)
 */
Optional<ComandaItem> findByComanda_ComandaIdAndProducto_ProductoIdAndComandaItemDescripcion(
        Long comandaId, Long productoId, String descripcion);

/**
 * Devuelve todos los ítems modificados ({@code descripcion != null}) de un producto
 * en una comanda. Lo usa el flujo de eliminación del ítem base para arrastrar a
 * todos sus modificados.
 *
 * @param comandaId  comanda BORRADOR
 * @param productoId producto cuyo ítem base se está eliminando
 */
List<ComandaItem> findByComanda_ComandaIdAndProducto_ProductoIdAndComandaItemDescripcionIsNotNull(
        Long comandaId, Long productoId);

/**
 * Suma el total monetario acumulado de todos los ítems de la visita que aún no están
 * en estado COMPLETADO. Incluye ítems BORRADOR, PENDIENTE, EN_PREPARACION y LISTO.
 *
 * <p>Los ítems con precio nulo (bebidas del menú especial) se ignoran y aportan
 * cero al total.
 *
 * @param visitaId   identificador de la visita
 * @param excluido   estado a excluir del cómputo (típicamente {@code COMPLETADO})
 * @return suma total; {@link BigDecimal#ZERO} si no hay ítems activos
 */
@Query("""
    SELECT COALESCE(SUM(ci.comandaItemPrecio * ci.comandaItemCantidad), 0)
    FROM ComandaItem ci
    WHERE ci.comanda.visita.visitaId = :visitaId
    AND ci.comanda.comandaEstado <> :excluido
    AND ci.comandaItemPrecio IS NOT NULL
    """)
BigDecimal sumTotalActivosByVisita(@Param("visitaId") Long visitaId,
                                   @Param("excluido") EstadoComanda excluido);
```

- [ ] **Step 3: Compilar**

```bash
"C:/Program Files/Apache/Maven/bin/mvn" -f backend/pom.xml clean compile -q
```

---

### Task 4: DTOs de request/response/messaging

**Files:**
- Create: `backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/dto/request/AgregarItemRequest.java`
- Create: `backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/dto/request/ModificarItemRequest.java`
- Create: `backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/dto/response/BorradorComandaResponse.java`
- Create: `backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/dto/response/ItemBorradorResponse.java`
- Create: `backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/dto/response/ItemBebidaMenuResponse.java`
- Create: `backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/dto/request/NotasRequest.java`
- Create: `backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/dto/messaging/ComandaNuevaMessage.java`
- Create: `backend/src/main/java/co/edu/unicauca/backend/modules/notificaciones/dto/ws/ComandaEstacionWsMessage.java`

- [ ] **Step 1: AgregarItemRequest**

```java
package co.edu.unicauca.backend.modules.mesas_comandas.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Payload para agregar un ítem al borrador de comanda. La estación destino
 * (cocina o barra) la deduce el servicio a partir de la categoría del producto.
 */
@Getter @Setter
public class AgregarItemRequest {

    /** Identificador de la visita dueña del borrador. */
    @NotNull(message = "visitaId es obligatorio")
    private Long visitaId;

    /** Producto a agregar; no se aceptan productos marcados como menú especial. */
    @NotNull(message = "productoId es obligatorio")
    private Long productoId;

    /** Cantidad solicitada; rango {@code [1, 250]}. */
    @NotNull(message = "cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad mínima es 1")
    @Max(value = 250, message = "La cantidad máxima por producto/bebida es de 250")
    private Integer cantidad;

    /**
     * Texto de modificación libre. {@code null} indica un ítem base sin
     * modificación; un valor presente persiste un ítem modificado con el mismo
     * {@code productoId} y descripción propia. El mismo producto puede aparecer
     * en la comanda múltiples veces con distintas descripciones.
     */
    @Size(max = 500, message = "La descripción no debe exceder 500 caracteres")
    private String descripcion;
}
```

- [ ] **Step 2: ModificarItemRequest**

```java
package co.edu.unicauca.backend.modules.mesas_comandas.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Payload para modificar la cantidad y/o la descripción de un ítem existente
 * del borrador. Cualquier campo {@code null} se interpreta como "no cambiar".
 */
@Getter @Setter
public class ModificarItemRequest {

    /** Nueva cantidad del ítem; rango {@code [1, 250]}. {@code null} no la altera. */
    @Min(value = 1, message = "La cantidad mínima es 1")
    @Max(value = 250, message = "La cantidad máxima por producto/bebida es de 250")
    private Integer cantidad;

    /** Nueva descripción del ítem; {@code null} no la altera. */
    @Size(max = 500, message = "La descripción no debe exceder 500 caracteres")
    private String descripcion;
}
```

- [ ] **Step 3: NotasRequest**

Archivo: `backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/dto/request/NotasRequest.java`. Estructura ya mostrada en la sección 4.1.

- [ ] **Step 4: ItemBebidaMenuResponse**

```java
package co.edu.unicauca.backend.modules.mesas_comandas.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * Bebida del menú especial fusionada con el plato del menú dentro del ítem
 * de borrador. La bebida no aporta precio: el cobro completo del menú vive
 * en el ítem del plato. Solo se expone identidad para que el frontend pinte
 * la bebida elegida.
 */
@Getter @Builder
public class ItemBebidaMenuResponse {
    /** Identificador del producto bebida. */
    private final Long productoId;
    /** Nombre del producto bebida. */
    private final String productoNombre;
}
```


- [ ] **Step 5: ItemBorradorResponse**

```java
package co.edu.unicauca.backend.modules.mesas_comandas.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

/**
 * Ítem del formulario de modificar comanda. Se autoreferencia: cuando el ítem
 * es base ({@code descripcion == null}) puede llevar en {@code modificaciones}
 * la sub-lista de ítems con el mismo {@code productoId} y descripción no nula.
 *
 * <p>Cuando el ítem proviene de un menú especial se fusiona el par COCINA+BARRA
 * del mismo {@code menuGrupo} en una sola fila: {@code bebida} contiene la
 * bebida del menú y {@code modificacionesMenu} las opciones seleccionadas.
 */
@Getter @Builder
public class ItemBorradorResponse {
    /** Identificador del {@code comanda_item} en BD; clave para PATCH/DELETE. */
    private final Long comandaItemId;
    /** Identificador del producto del catálogo. */
    private final Long productoId;
    /** Nombre del producto al momento de cargar el borrador. */
    private final String productoNombre;
    /** Categoría del producto: {@code "PLATO"} o {@code "BEBIDA"}. */
    private final String categoriaProducto;
    /** Precio unitario congelado en el ítem; puede diferir del catálogo si hubo cambio posterior. */
    private final BigDecimal precioUnitario;
    /** Cantidad solicitada. */
    private final Integer cantidad;
    /** Resultado de {@code precioUnitario * cantidad}; cero si el precio es nulo (menú). */
    private final BigDecimal subtotal;
    /** Texto de modificación libre; {@code null} indica ítem base sin modificación. */
    private final String descripcion;
    /** UUID del par COCINA+BARRA del mismo menú especial; {@code null} para ítems de carta. */
    private final String menuGrupo;
    /** Opciones del menú especial seleccionadas (arroz, salsa, etc.); vacía para ítems de carta. */
    private final List<OpcionMenuSeleccionadaResponse> modificacionesMenu;
    /** Bebida fusionada del menú especial; {@code null} si el ítem es de carta. */
    private final ItemBebidaMenuResponse bebida;
    /**
     * Ítems modificados anidados bajo este ítem base. Comparten {@code productoId}
     * con el padre pero tienen su propia {@code descripcion}, cantidad y precio.
     * Lista vacía cuando el ítem no tiene modificaciones o cuando este mismo DTO
     * representa ya un hijo modificado.
     */
    private final List<ItemBorradorResponse> modificaciones;
    /** Stock disponible del producto en catálogo; {@code null} si no se gestiona stock. */
    private final BigDecimal stockActual;
}
```

> El DTO local {@code OpcionMenuSeleccionadaResponse} se define en Task 7 Step 1; se evita el import cruzado al módulo {@code reservas} para no introducir un ciclo de paquetes.

- [ ] **Step 6: BorradorComandaResponse**

```java
package co.edu.unicauca.backend.modules.mesas_comandas.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

/**
 * Vista completa del formulario de modificar comanda. Agrupa los ítems por
 * estación, expone los identificadores de las dos comandas BORRADOR posibles
 * y los flags que habilitan los botones de envío a producción.
 */
@Getter @Builder
public class BorradorComandaResponse {
    /** Identificador de la visita dueña del borrador. */
    private final Long visitaId;
    /** Identificador legible de la mesa (ej. {@code "Mesa 5"}). */
    private final String mesaIdentificador;
    /** Identificador de la comanda BORRADOR de cocina; {@code null} si no existe. */
    private final Long comandaCocinaId;
    /** Identificador de la comanda BORRADOR de barra; {@code null} si no existe. */
    private final Long comandaBarraId;
    /** Ítems con categoría {@code PLATO}, ordenados alfabéticamente. */
    private final List<ItemBorradorResponse> platos;
    /** Ítems con categoría {@code BEBIDA}, ordenados alfabéticamente. */
    private final List<ItemBorradorResponse> bebidas;
    /** Total acumulado de todas las comandas de la visita no-COMPLETADO. */
    private final BigDecimal total;
    /** Notas para la cocina; {@code null} si la comanda BORRADOR de cocina no existe o no tiene notas. */
    private final String notasCocina;
    /** Notas para la barra; {@code null} si la comanda BORRADOR de barra no existe o no tiene notas. */
    private final String notasBarra;
    /** {@code true} cuando {@code platos} no está vacío; habilita el envío a cocina. */
    private final Boolean puedeEnviarCocina;
    /** {@code true} cuando {@code bebidas} no está vacío; habilita el envío a barra. */
    private final Boolean puedeEnviarBarra;
}
```

- [ ] **Step 7: ComandaNuevaMessage (RabbitMQ)**

Archivo: `backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/dto/messaging/ComandaNuevaMessage.java`

```java
package co.edu.unicauca.backend.modules.mesas_comandas.dto.messaging;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Payload publicado en RabbitMQ (routing key {@code RK_COMANDA_NUEVA}) cuando
 * una comanda transiciona de BORRADOR a PENDIENTE. Lo consume el bridge de
 * impresión de tickets desde la cola {@code q.comanda.produccion}. No actualiza
 * dashboards de producción — eso se hace por WebSocket.
 */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ComandaNuevaMessage {
    /** Identificador de la comanda enviada a producción. */
    private Long comandaId;
    /** Identificador de la visita dueña de la comanda. */
    private Long visitaId;
    /** Estación destino: {@code "COCINA"} o {@code "BARRA"}. */
    private String estacion;
    /** Marca temporal asignada al transicionar a PENDIENTE. */
    private LocalDateTime fechaHoraInicio;
}
```

- [ ] **Step 8: ComandaEstacionWsMessage (WebSocket — paquete `notificaciones`)**

Archivo: `backend/src/main/java/co/edu/unicauca/backend/modules/notificaciones/dto/ws/ComandaEstacionWsMessage.java`

> **Ubicación:** convive con los demás `*WsMessage` ya existentes (`AsistenciaAtendidaWsMessage`, `ComandaCompletadaWsMessage`, `VisitaActualizadaWsMessage`, etc.) en `notificaciones/dto/ws/`. Mantener este patrón evita fragmentar los DTOs WebSocket.

```java
package co.edu.unicauca.backend.modules.notificaciones.dto.ws;

import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.ItemBorradorResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Payload publicado en WebSocket {@code /topic/estacion/{estacion}} cuando una
 * comanda pasa a PENDIENTE. Lo consume el dashboard de la estación
 * correspondiente: COCINA recibe platos, BARRA recibe bebidas.
 */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ComandaEstacionWsMessage {
    /** Identificador de la comanda recién enviada. */
    private Long comandaId;
    /** Identificador de la visita dueña de la comanda. */
    private Long visitaId;
    /** Estación destino: {@code "COCINA"} o {@code "BARRA"}. */
    private String estacion;
    /** Ítems de la comanda, ordenados por nombre de producto. */
    private List<ItemBorradorResponse> items;
}
```

- [ ] **Step 9: Compilar**

```bash
"C:/Program Files/Apache/Maven/bin/mvn" -f backend/pom.xml clean compile -q
```

---

### Task 5: Agregar `validarOwnership` al `MesaValidador` existente

**Files:**
- Modify: `backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/service/MesaValidador.java`
- Modify: `backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/service/MesaService.java`

> **Decisión:** no se crea una clase nueva. Se concentra la responsabilidad de validación de mesa en el componente ya existente `MesaValidador` para no fragmentar el módulo. El método se reutiliza desde `ComandaBorradorService` y, como limpieza adicional, desde `MesaService.obtenerItemsProduccion` (donde la lógica vive inline hoy).

- [ ] **Step 1: Agregar el método en `MesaValidador`**

Insertar el siguiente método (manteniendo los imports existentes y agregando `Authentication`, `GrantedAuthority`, `Mesa` y `MesaRepository` si no están). `MesaValidador` debe pasar a inyectar `MesaRepository` (constructor final + `@RequiredArgsConstructor`).

```java
/**
 * Verifica que el principal autenticado sea el mesero asignado a la mesa
 * de la visita indicada, o tenga rol ADMIN. Carga y devuelve la entidad
 * para evitar un refetch en el caller.
 *
 * @param visitaId       identificador de la mesa (PK = {@code visitaId})
 * @param authentication contexto de seguridad del request
 * @return la entidad {@link Mesa} cargada
 * @throws BusinessException con {@code ENTITY_NOT_FOUND} si la mesa no
 *         existe o la visita ya está cerrada
 * @throws BusinessException con {@code ACCESS_DENIED} si el principal no
 *         es ADMIN ni el mesero asignado
 */
@Transactional(readOnly = true)
public Mesa validarOwnership(Long visitaId, Authentication authentication) {

    Mesa mesa = mesaRepository.findById(visitaId)
            .orElseThrow(() -> new BusinessException(
                    ErrorCode.ENTITY_NOT_FOUND, "Mesa no encontrada", HttpStatus.NOT_FOUND));

    if (mesa.getVisita().getVisitaFechaHoraFin() != null) {
        throw new BusinessException(
                ErrorCode.ENTITY_NOT_FOUND, "La visita ya está cerrada", HttpStatus.NOT_FOUND);
    }

    boolean esAdmin = authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch("ROLE_ADMIN"::equals);

    if (!esAdmin) {
        String emailAsignado = mesa.getMesero().getUsuario().getUsuarioEmail();
        String emailUsuario = authentication.getName();
        if (!emailAsignado.equals(emailUsuario)) {
            throw new BusinessException(
                    ErrorCode.ACCESS_DENIED,
                    "Solo el mesero asignado puede operar sobre esta mesa",
                    HttpStatus.FORBIDDEN);
        }
    }
    return mesa;
}
```

- [ ] **Step 2: Refactorizar `MesaService.obtenerItemsProduccion`**

Reemplazar las validaciones inline (mesa cargada, visita cerrada, ownership) por una sola línea:

```java
Mesa mesa = mesaValidador.validarOwnership(visitaId, authentication);
```

Esto elimina duplicación con HU-05 y mantiene un único lugar donde se chequea ownership de mesa.

- [ ] **Step 3: Compilar**

```bash
"C:/Program Files/Apache/Maven/bin/mvn" -f backend/pom.xml clean compile -q
```

---

### Task 6: Validador de stock y reglas de borrador

**Files:**
- Create: `backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/service/ComandaBorradorValidador.java`

- [ ] **Step 1: Crear el validador**

```java
package co.edu.unicauca.backend.modules.mesas_comandas.service;

import co.edu.unicauca.backend.modules.inventario.entity.Producto;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.ComandaItem;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ComandaItemRepository;
import co.edu.unicauca.backend.shared.enums.CategoriaProducto;
import co.edu.unicauca.backend.shared.enums.EstacionComanda;
import co.edu.unicauca.backend.shared.exception.BusinessException;
import co.edu.unicauca.backend.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Validaciones de negocio aplicables al borrador de comanda. Concentra:
 * <ul>
 *   <li>Validación de stock disponible vs cantidad propuesta.</li>
 *   <li>Resolución de estación destino a partir de la categoría del producto.</li>
 *   <li>Verificación de que la comanda tenga al menos un ítem antes de enviarse
 *       a producción.</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class ComandaBorradorValidador {

    /** Acceso al cómputo de stock comprometido para la fórmula de disponibilidad. */
    private final ComandaItemRepository comandaItemRepository;

    /**
     * Valida que la cantidad propuesta no exceda el stock disponible. La fórmula
     * de disponibilidad es {@code stockActual - (sumaComprometida - cantidadAnterior)}:
     * la {@code cantidadAnterior} se resta del comprometido para no doble-contar
     * el ítem en cuestión cuando ya estaba persistido.
     *
     * @param producto         producto del ítem; si {@code stockActual} es {@code null}
     *                         no se gestiona stock y se omite la validación
     * @param nuevaCantidad    cantidad propuesta tras aplicar la operación
     * @param cantidadAnterior cantidad ya contabilizada del mismo ítem; {@code 0}
     *                         cuando el ítem es nuevo
     * @throws BusinessException con {@code INSUFFICIENT_STOCK} si la operación
     *         excedería el stock disponible
     */
    public void validarStock(Producto producto, int nuevaCantidad, int cantidadAnterior) {
        if (producto.getStockActual() == null) {
            return;
        }
        long comprometido = comandaItemRepository
                .sumCantidadComprometidaByProducto(producto.getProductoId());
        long disponible = producto.getStockActual().longValue() - (comprometido - cantidadAnterior);
        if (nuevaCantidad > disponible) {
            throw new BusinessException(
                    ErrorCode.INSUFFICIENT_STOCK,
                    "Solo hay " + Math.max(disponible, 0) + " unidades disponibles de este producto",
                    HttpStatus.CONFLICT);
        }
    }

    /**
     * Resuelve la estación destino según la categoría del producto.
     * {@code PLATO} va a COCINA, {@code BEBIDA} a BARRA. Cualquier otra
     * categoría (incluida {@code OTRO}, deprecada) se rechaza.
     *
     * @param producto producto a clasificar
     * @return estación destino
     * @throws BusinessException si la categoría no es soportada
     */
    public EstacionComanda resolverEstacion(Producto producto) {
        CategoriaProducto cat = producto.getProductoCategoria();
        return switch (cat) {
            case PLATO  -> EstacionComanda.COCINA;
            case BEBIDA -> EstacionComanda.BARRA;
            default -> throw new BusinessException(
                    ErrorCode.BUSINESS_ERROR,
                    "La categoría de producto '" + cat + "' no se admite en este flujo",
                    HttpStatus.BAD_REQUEST);
        };
    }

    /**
     * Verifica que la comanda tenga al menos un ítem antes de enviarse a producción.
     *
     * @param items ítems persistidos de la comanda
     * @throws BusinessException si la lista es nula o vacía
     */
    public void validarTieneItems(List<ComandaItem> items) {
        if (items == null || items.isEmpty()) {
            throw new BusinessException(
                    ErrorCode.BUSINESS_ERROR,
                    "La comanda debe tener al menos un producto antes de enviarse a producción",
                    HttpStatus.BAD_REQUEST);
        }
    }
}
```

> **NOTA:** `ErrorCode.INSUFFICIENT_STOCK` no existe en el enum. `NEG-003` ya está ocupado por `CAPACITY_EXCEEDED`. Agregar en `ErrorCode.java` antes de implementar este validador:

- [ ] **Step 2: Agregar ErrorCode INSUFFICIENT_STOCK**

En `ErrorCode.java`, agregar tras `CAPACITY_EXCEEDED`:

```java
INSUFFICIENT_STOCK("NEG-004", "Stock insuficiente para completar la operación."),
```

- [ ] **Step 3: Compilar**

```bash
"C:/Program Files/Apache/Maven/bin/mvn" -f backend/pom.xml clean compile -q
```

---

### Task 7: Mapper ComandaBorradorMapper

**Files:**
- Create: `backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/mapper/ComandaBorradorMapper.java`

- [ ] **Step 1: Crear primero `OpcionMenuSeleccionadaResponse` local**

Archivo: `backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/dto/response/OpcionMenuSeleccionadaResponse.java`

> **Decisión:** se duplica la estructura local en `mesas_comandas` en lugar de importar `OpcionModificacionSeleccionada` desde `reservas`. Razón: el módulo `reservas` ya consume tipos de `mesas_comandas` (visita, mesa); importar a la inversa crearía un ciclo de paquetes y un acoplamiento bidireccional difícil de testear.

```java
package co.edu.unicauca.backend.modules.mesas_comandas.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * Opción de modificación seleccionada sobre el plato de un menú especial
 * (arroz, salsa, ensalada, etc.) tal como se muestra en el formulario de
 * modificar comanda.
 */
@Getter @Builder
public class OpcionMenuSeleccionadaResponse {
    /** Identificador de la opción dentro del catálogo de modificaciones. */
    private final Long opcionId;
    /** Nombre legible de la opción. */
    private final String opcionNombre;
    /** Tipo de componente del menú: {@code "ARROZ"}, {@code "SALSA"}, etc. */
    private final String tipoComponente;
}
```

- [ ] **Step 2: Crear mapper completo**

Archivo: `backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/mapper/ComandaBorradorMapper.java`

```java
package co.edu.unicauca.backend.modules.mesas_comandas.mapper;

import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.BorradorComandaResponse;
import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.ItemBebidaMenuResponse;
import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.ItemBorradorResponse;
import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.OpcionMenuSeleccionadaResponse;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Comanda;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.ComandaItem;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.ComandaMenuModificacion;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Mesa;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ComandaItemRepository;
import co.edu.unicauca.backend.shared.enums.EstacionComanda;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Mapper de la vista del formulario de modificar comanda.
 *
 * <p>Responsabilidades:
 * <ul>
 *   <li>Agrupar ítems base ({@code comandaItemDescripcion IS NULL}) con sus
 *       ítems modificados ({@code mismo productoId + descripcion IS NOT NULL})
 *       en la sub-lista anidada {@code modificaciones}.</li>
 *   <li>Para ítems de menú especial, fusionar el par COCINA+BARRA del mismo
 *       {@code comandaItemMenuGrupo} en un único {@link ItemBorradorResponse}
 *       con la bebida embebida.</li>
 *   <li>Calcular subtotal por ítem y delegar el total acumulado al servicio.</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class ComandaBorradorMapper {

    /** Carga los ítems de la comanda contraparte para localizar la bebida del menú. */
    private final ComandaItemRepository comandaItemRepository;

    /** Orden alfabético case-insensitive por nombre de producto. */
    private static final Comparator<ComandaItem> POR_NOMBRE =
            Comparator.comparing(it -> it.getProducto().getProductoNombre(),
                                 String.CASE_INSENSITIVE_ORDER);

    /**
     * Construye la respuesta completa de {@code GET /api/comandas/borrador}.
     *
     * @param mesa             mesa dueña del borrador (aporta identificador y visita)
     * @param comandasBorrador comandas BORRADOR de la visita (0, 1 o 2)
     * @param totalAcumulado   suma de {@code precio * cantidad} de todos los ítems
     *                         de la visita en estados distintos de COMPLETADO,
     *                         calculada por el servicio
     * @return DTO listo para el frontend; nunca {@code null}
     */
    public BorradorComandaResponse toBorradorResponse(Mesa mesa,
                                                     List<Comanda> comandasBorrador,
                                                     BigDecimal totalAcumulado) {

        Comanda cocina = comandasBorrador.stream()
                .filter(c -> c.getComandaEstacion() == EstacionComanda.COCINA)
                .findFirst().orElse(null);
        Comanda barra = comandasBorrador.stream()
                .filter(c -> c.getComandaEstacion() == EstacionComanda.BARRA)
                .findFirst().orElse(null);

        List<ComandaItem> itemsCocina = cargarItems(cocina);
        List<ComandaItem> itemsBarra  = cargarItems(barra);

        List<ItemBorradorResponse> platos  = mapearEstacion(itemsCocina, itemsBarra);
        List<ItemBorradorResponse> bebidas = mapearEstacion(itemsBarra,  itemsCocina);

        BigDecimal total = totalAcumulado != null ? totalAcumulado : BigDecimal.ZERO;

        return BorradorComandaResponse.builder()
                .visitaId(mesa.getVisitaId())
                .mesaIdentificador(mesa.getMesaIdentificador())
                .comandaCocinaId(cocina != null ? cocina.getComandaId() : null)
                .comandaBarraId(barra  != null ? barra.getComandaId()  : null)
                .platos(platos)
                .bebidas(bebidas)
                .total(total)
                .notasCocina(cocina != null ? cocina.getComandaNotas() : null)
                .notasBarra(barra   != null ? barra.getComandaNotas()  : null)
                .puedeEnviarCocina(!platos.isEmpty())
                .puedeEnviarBarra(!bebidas.isEmpty())
                .build();
    }

    /**
     * Convierte una lista plana de {@link ComandaItem} a una lista de
     * {@link ItemBorradorResponse} sin fusión de menú ni anidado de modificaciones.
     * Lo usa el publisher WebSocket {@code /topic/estacion/{estacion}} al enviar
     * a producción: el dashboard de la estación recibe ítems "tal como están".
     *
     * @param items ítems de la comanda enviada
     * @return lista de respuestas ordenada por nombre de producto
     */
    public List<ItemBorradorResponse> toItemsResponse(List<ComandaItem> items) {
        if (items == null || items.isEmpty()) return List.of();
        return items.stream()
                .sorted(POR_NOMBRE)
                .map(it -> mapearItem(it, BigDecimal.ZERO, List.of(), null))
                .collect(Collectors.toList());
    }

    /**
     * Mapea los ítems de UNA estación, agrupando modificados bajo su ítem base
     * y fusionando los pares de menú especial con la otra estación.
     *
     * @param itemsFuente   ítems de la estación que se quiere mostrar
     * @param itemsContraparte ítems de la otra estación (para localizar la bebida del menú)
     * @return ítems base ordenados alfabéticamente, con modificaciones anidadas
     */
    private List<ItemBorradorResponse> mapearEstacion(List<ComandaItem> itemsFuente,
                                                      List<ComandaItem> itemsContraparte) {
        if (itemsFuente == null || itemsFuente.isEmpty()) return List.of();

        // Particionar por productoId; dentro de cada productoId, separar base (descripcion=null)
        // de modificados (descripcion!=null). Mismo productoId puede tener un base + N modificados.
        Map<Long, List<ComandaItem>> porProducto = itemsFuente.stream()
                .collect(Collectors.groupingBy(it -> it.getProducto().getProductoId(),
                                               LinkedHashMap::new, Collectors.toList()));

        List<ItemBorradorResponse> salida = new ArrayList<>();
        for (List<ComandaItem> grupo : porProducto.values()) {
            List<ComandaItem> bases = grupo.stream()
                    .filter(it -> it.getComandaItemDescripcion() == null)
                    .sorted(POR_NOMBRE)
                    .collect(Collectors.toList());
            List<ComandaItem> modificados = grupo.stream()
                    .filter(it -> it.getComandaItemDescripcion() != null)
                    .sorted(POR_NOMBRE)
                    .collect(Collectors.toList());

            if (bases.isEmpty() && !modificados.isEmpty()) {
                // Caso raro: existen modificados sin base (frontend siempre crea base primero,
                // pero defendemos para no perder filas). Se exponen como ítems sueltos.
                modificados.forEach(it -> salida.add(mapearItem(it, BigDecimal.ZERO, List.of(),
                                                                buscarBebidaDelMenu(itemsContraparte, it))));
                continue;
            }

            for (ComandaItem base : bases) {
                List<ItemBorradorResponse> hijos = modificados.stream()
                        .map(m -> mapearItem(m, calcularSubtotal(m), List.of(), null))
                        .collect(Collectors.toList());

                salida.add(mapearItem(base, calcularSubtotal(base), hijos,
                                      buscarBebidaDelMenu(itemsContraparte, base)));
                // Solo se asocian modificados al primer base por productoId; los demás base
                // (puede haber duplicados solo si el invariante se rompe) se exponen sin hijos.
                modificados = List.of();
            }
        }
        // Orden final por nombre del producto del ítem base.
        salida.sort(Comparator.comparing(ItemBorradorResponse::getProductoNombre,
                                         String.CASE_INSENSITIVE_ORDER));
        return salida;
    }

    /**
     * Construye el DTO de un {@link ComandaItem} dado.
     *
     * @param item            entidad fuente
     * @param subtotal        {@code precioUnitario * cantidad}; cero si se desconoce
     * @param modificaciones  hijos modificados ya mapeados (vacío para ítems hoja)
     * @param bebidaMenu      bebida fusionada del par menú especial; {@code null} si no aplica
     * @return DTO inmutable listo para el frontend
     */
    private ItemBorradorResponse mapearItem(ComandaItem item,
                                            BigDecimal subtotal,
                                            List<ItemBorradorResponse> modificaciones,
                                            ItemBebidaMenuResponse bebidaMenu) {
        return ItemBorradorResponse.builder()
                .comandaItemId(item.getComandaItemId())
                .productoId(item.getProducto().getProductoId())
                .productoNombre(item.getProducto().getProductoNombre())
                .categoriaProducto(item.getProducto().getProductoCategoria().name())
                .precioUnitario(item.getComandaItemPrecio())
                .cantidad(item.getComandaItemCantidad())
                .subtotal(subtotal)
                .descripcion(item.getComandaItemDescripcion())
                .menuGrupo(item.getComandaItemMenuGrupo())
                .modificacionesMenu(mapearModificacionesMenu(item.getModificaciones()))
                .bebida(bebidaMenu)
                .modificaciones(modificaciones)
                .stockActual(item.getProducto().getStockActual())
                .build();
    }

    /**
     * Carga los ítems de una comanda desde el repositorio. Devuelve lista vacía
     * cuando la comanda es {@code null} (no existe BORRADOR para esa estación).
     */
    private List<ComandaItem> cargarItems(Comanda comanda) {
        if (comanda == null) return List.of();
        return comandaItemRepository
                .findByComanda_ComandaIdOrderByProductoNombreAsc(comanda.getComandaId());
    }

    /**
     * Localiza la bebida del menú especial cuyo {@code comandaItemMenuGrupo} coincide
     * con el del ítem dado, dentro de los ítems de la otra estación.
     *
     * @param itemsContraparte ítems de la otra estación
     * @param itemMenu         ítem cocina/barra del menú; cualquier otro ítem retorna {@code null}
     * @return DTO de la bebida; {@code null} si el ítem no es de menú o no hay contraparte
     */
    private ItemBebidaMenuResponse buscarBebidaDelMenu(List<ComandaItem> itemsContraparte,
                                                       ComandaItem itemMenu) {
        String grupo = itemMenu.getComandaItemMenuGrupo();
        if (grupo == null || itemsContraparte == null || itemsContraparte.isEmpty()) return null;
        return itemsContraparte.stream()
                .filter(ci -> grupo.equals(ci.getComandaItemMenuGrupo()))
                .findFirst()
                .map(ci -> ItemBebidaMenuResponse.builder()
                        .productoId(ci.getProducto().getProductoId())
                        .productoNombre(ci.getProducto().getProductoNombre())
                        .build())
                .orElse(null);
    }

    /**
     * Mapea las opciones de modificación de menú especial seleccionadas para un
     * ítem dado.
     *
     * @param origen lista persistida en {@code comanda_menu_modificacion}
     * @return lista de DTOs; vacía si {@code origen} es null o vacía
     */
    private List<OpcionMenuSeleccionadaResponse> mapearModificacionesMenu(
            List<ComandaMenuModificacion> origen) {
        if (origen == null || origen.isEmpty()) return List.of();
        return origen.stream()
                .filter(Objects::nonNull)
                .map(m -> OpcionMenuSeleccionadaResponse.builder()
                        .opcionId(m.getOpcionModificacion().getOpcionId())
                        .opcionNombre(m.getOpcionModificacion().getOpcionNombre())
                        .tipoComponente(m.getOpcionModificacion().getTipoComponente().name())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Calcula {@code precio * cantidad} para un ítem; devuelve {@link BigDecimal#ZERO}
     * si el precio es {@code null} (ítem de menú con precio cero).
     */
    private BigDecimal calcularSubtotal(ComandaItem item) {
        if (item.getComandaItemPrecio() == null) return BigDecimal.ZERO;
        return item.getComandaItemPrecio()
                .multiply(BigDecimal.valueOf(item.getComandaItemCantidad()));
    }
}
```

- [ ] **Step 3: Compilar**

```bash
"C:/Program Files/Apache/Maven/bin/mvn" -f backend/pom.xml clean compile -q
```

---

### Task 8: Servicio principal ComandaBorradorService

**Files:**
- Create: `backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/service/ComandaBorradorService.java`

- [ ] **Step 1: Crear el servicio con los 7 métodos públicos**

```java
package co.edu.unicauca.backend.modules.mesas_comandas.service;

import co.edu.unicauca.backend.modules.inventario.entity.Producto;
import co.edu.unicauca.backend.modules.inventario.repository.ProductoRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.dto.messaging.ComandaNuevaMessage;
import co.edu.unicauca.backend.modules.notificaciones.dto.ws.ComandaEstacionWsMessage;
import co.edu.unicauca.backend.modules.mesas_comandas.dto.request.AgregarItemRequest;
import co.edu.unicauca.backend.modules.mesas_comandas.dto.request.ModificarItemRequest;
import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.BorradorComandaResponse;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.*;
import co.edu.unicauca.backend.modules.mesas_comandas.mapper.ComandaBorradorMapper;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.*;
import co.edu.unicauca.backend.modules.notificaciones.dto.ws.VisitaActualizadaWsMessage;
import co.edu.unicauca.backend.modules.notificaciones.service.NotificacionWsPublisher;
import co.edu.unicauca.backend.shared.config.RabbitMQConfig;
import co.edu.unicauca.backend.shared.enums.EstacionComanda;
import co.edu.unicauca.backend.shared.enums.EstadoComanda;
import co.edu.unicauca.backend.shared.enums.EstadoMesa;
import co.edu.unicauca.backend.shared.exception.BusinessException;
import co.edu.unicauca.backend.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Lógica de negocio del formulario de modificar comanda.
 *
 * <p>Responsabilidades:
 * <ul>
 *   <li>Cargar el borrador (precargado desde pre-orden o vacío).</li>
 *   <li>Agregar, modificar y eliminar ítems con validación de stock y techo
 *       de cantidad.</li>
 *   <li>Persistir modificaciones libres como ítems con el mismo
 *       {@code productoId} del base y descripción no nula.</li>
 *   <li>Persistir notas por estación (cocina/barra).</li>
 *   <li>Enviar la comanda a producción (transición BORRADOR → PENDIENTE) y
 *       cancelar el formulario descartando los borradores.</li>
 * </ul>
 *
 * <p>Tópicos WebSocket utilizados:
 * <ul>
 *   <li>{@code /topic/mesas}: cambios en {@code tieneBorrador} y estado de mesa.</li>
 *   <li>{@code /topic/visita/{visitaId}/orden}: al enviar a producción, refresca
 *       la orden visible al cliente.</li>
 *   <li>{@code /topic/estacion/{estacion}}: al enviar a producción, alimenta el
 *       dashboard de cocina o barra.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class ComandaBorradorService {

    private final ComandaRepository comandaRepository;
    private final ComandaItemRepository comandaItemRepository;
    private final ProductoRepository productoRepository;
    private final MesaRepository mesaRepository;

    private final ComandaBorradorMapper borradorMapper;
    private final ComandaBorradorValidador validador;
    private final MesaValidador mesaValidador;

    private final MesaWsPublisher mesaWsPublisher;
    private final NotificacionWsPublisher notificacionWsPublisher;
    private final EstacionWsPublisher estacionWsPublisher;
    private final RabbitTemplate rabbitTemplate;

    /**
     * Devuelve el borrador completo de la visita: ítems agrupados por estación,
     * notas por estación, total acumulado y flags para habilitar los botones de
     * envío a producción. Si la visita no tiene borradores devuelve estructura
     * vacía pero válida.
     *
     * @param visitaId identificador de la visita
     * @param auth     contexto del usuario autenticado
     * @return DTO listo para el frontend
     */
    @Transactional(readOnly = true)
    public BorradorComandaResponse obtenerBorrador(Long visitaId, Authentication auth) {
        Mesa mesa = mesaValidador.validarOwnership(visitaId, auth);
        return obtenerBorradorInterno(mesa);
    }

    /**
     * Agrega un ítem al borrador de la estación correspondiente al producto
     * (PLATO → COCINA, BEBIDA → BARRA). Si la comanda BORRADOR de la estación
     * no existe, la crea. Si ya existe un ítem con el mismo {@code productoId}
     * y la misma {@code descripcion} acumula la cantidad.
     *
     * @param req  payload con producto, cantidad y descripción opcional
     * @param auth contexto del usuario autenticado
     * @return borrador completo tras la operación
     */
    @Transactional
    public BorradorComandaResponse agregarItem(AgregarItemRequest req, Authentication auth) {
        Mesa mesa = mesaValidador.validarOwnership(req.getVisitaId(), auth);

        Producto producto = productoRepository.findById(req.getProductoId())
                .orElseThrow(() -> new ResourceNotFoundException("Producto", req.getProductoId()));

        if (Boolean.TRUE.equals(producto.getMenuEspecial())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR,
                    "Los menús especiales se cargan vía pre-orden, no se agregan al borrador",
                    HttpStatus.BAD_REQUEST);
        }

        validador.validarStock(producto, req.getCantidad(), 0);
        EstacionComanda estacion = validador.resolverEstacion(producto);

        // Obtener-o-crear comanda BORRADOR de la estación.
        Comanda comanda = comandaRepository
                .findByVisita_VisitaIdAndComandaEstadoAndComandaEstacion(
                        req.getVisitaId(), EstadoComanda.BORRADOR, estacion)
                .orElseGet(() -> comandaRepository.save(Comanda.builder()
                        .visita(mesa.getVisita())
                        .reserva(mesa.getVisita().getReserva())
                        .comandaEstado(EstadoComanda.BORRADOR)
                        .comandaEstacion(estacion)
                        .build()));

        // Si ya existe un ítem con el mismo productoId y misma descripción, se acumula la cantidad.
        // Descripciones distintas (incluido null vs valor) generan ítems separados.
        Optional<ComandaItem> existente = comandaItemRepository
                .findByComanda_ComandaIdAndProducto_ProductoIdAndComandaItemDescripcion(
                        comanda.getComandaId(), req.getProductoId(), req.getDescripcion());
        if (existente.isPresent()) {
            ComandaItem ci = existente.get();
            int nuevaCantidad = ci.getComandaItemCantidad() + req.getCantidad();
            validador.validarStock(producto, nuevaCantidad, ci.getComandaItemCantidad());
            ci.setComandaItemCantidad(nuevaCantidad);
            comandaItemRepository.save(ci);
        } else {
            // Cuando la descripción está presente el ítem se persiste como modificación libre:
            // mismo productoId del base, precio propio (igual al catálogo en este flujo).
            ComandaItem item = ComandaItem.builder()
                    .comanda(comanda)
                    .producto(producto)
                    .comandaItemCantidad(req.getCantidad())
                    .comandaItemPrecio(producto.getProductoPrecio())
                    .comandaItemDescripcion(req.getDescripcion())
                    .build();
            comandaItemRepository.save(item);
        }

        publicarMesasActualizada(req.getVisitaId());
        return obtenerBorradorInterno(mesa);
    }

    /**
     * Modifica la cantidad y/o la descripción de un ítem existente del borrador.
     * Cualquier campo {@code null} en la petición se interpreta como "no cambiar".
     * Revalida el stock cuando se altera la cantidad.
     *
     * @param itemId identificador del {@code comanda_item}
     * @param req    cambios solicitados
     * @param auth   contexto del usuario autenticado
     * @return borrador completo tras la operación
     */
    @Transactional
    public BorradorComandaResponse modificarItem(Long itemId, ModificarItemRequest req, Authentication auth) {
        ComandaItem item = comandaItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("ComandaItem", itemId));

        Comanda comanda = item.getComanda();
        if (comanda.getComandaEstado() != EstadoComanda.BORRADOR) {
            throw new BusinessException(ErrorCode.INVALID_STATE,
                    "El item no pertenece a una comanda en BORRADOR", HttpStatus.CONFLICT);
        }

        Long visitaId = comanda.getVisita().getVisitaId();
        Mesa mesa = mesaValidador.validarOwnership(visitaId, auth);

        if (req.getCantidad() != null) {
            if (req.getCantidad() == 0) {
                throw new BusinessException(ErrorCode.INVALID_STATE,
                        "Use DELETE para eliminar items; cantidad 0 no es válida",
                        HttpStatus.CONFLICT);
            }
            validador.validarStock(item.getProducto(), req.getCantidad(), item.getComandaItemCantidad());
            item.setComandaItemCantidad(req.getCantidad());
        }
        if (req.getDescripcion() != null) {
            item.setComandaItemDescripcion(req.getDescripcion());
        }
        comandaItemRepository.save(item);

        publicarMesasActualizada(visitaId);
        return obtenerBorradorInterno(mesa);
    }

    /**
     * Elimina un ítem del borrador. Reglas:
     * <ul>
     *   <li>Si el ítem es parte de un par menú especial ({@code menuGrupo} no nulo)
     *       se eliminan ambos ítems del par (cocina y barra).</li>
     *   <li>Si es un ítem base ({@code descripcion == null}) se eliminan también
     *       todos los ítems modificados con el mismo {@code productoId}.</li>
     *   <li>Si es un ítem modificado solo se elimina ese ítem.</li>
     *   <li>Si la comanda queda sin ítems se elimina la comanda.</li>
     * </ul>
     *
     * @param itemId identificador del {@code comanda_item}
     * @param auth   contexto del usuario autenticado
     * @return borrador completo tras la operación
     */
    @Transactional
    public BorradorComandaResponse eliminarItem(Long itemId, Authentication auth) {
        ComandaItem item = comandaItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("ComandaItem", itemId));

        Comanda comanda = item.getComanda();
        if (comanda.getComandaEstado() != EstadoComanda.BORRADOR) {
            throw new BusinessException(ErrorCode.INVALID_STATE,
                    "El item no pertenece a una comanda en BORRADOR", HttpStatus.CONFLICT);
        }

        Long visitaId = comanda.getVisita().getVisitaId();
        Mesa mesa = mesaValidador.validarOwnership(visitaId, auth);

        String grupo = item.getComandaItemMenuGrupo();
        if (grupo != null) {
            eliminarParMenu(visitaId, grupo);
        } else if (item.getComandaItemDescripcion() == null) {
            // Ítem base: arrastra a todos los modificados del mismo producto en la misma comanda.
            List<ComandaItem> modificados = comandaItemRepository
                    .findByComanda_ComandaIdAndProducto_ProductoIdAndComandaItemDescripcionIsNotNull(
                            comanda.getComandaId(), item.getProducto().getProductoId());
            comandaItemRepository.deleteAll(modificados);
            comandaItemRepository.delete(item);
            eliminarComandaSiVacia(comanda);
        } else {
            // Ítem modificado: solo se borra esa fila; el base y otros modificados permanecen.
            comandaItemRepository.delete(item);
            eliminarComandaSiVacia(comanda);
        }

        publicarMesasActualizada(visitaId);
        return obtenerBorradorInterno(mesa);
    }

    /**
     * Actualiza las notas de una comanda BORRADOR. Cada estación tiene sus propias
     * notas (la comanda COCINA y la BARRA se manejan por separado), por lo que la
     * petición identifica la comanda y no la visita. Persistir {@code null} borra
     * las notas existentes.
     *
     * @param comandaId identificador de la comanda BORRADOR
     * @param req       payload con el nuevo valor del campo {@code notas}
     * @param auth      contexto del usuario autenticado
     * @return borrador completo tras la operación
     */
    @Transactional
    public BorradorComandaResponse actualizarNotas(Long comandaId, NotasRequest req, Authentication auth) {
        Comanda comanda = comandaRepository.findById(comandaId)
                .orElseThrow(() -> new ResourceNotFoundException("Comanda", comandaId));

        if (comanda.getComandaEstado() != EstadoComanda.BORRADOR) {
            throw new BusinessException(ErrorCode.INVALID_STATE,
                    "Solo comandas en BORRADOR admiten notas", HttpStatus.CONFLICT);
        }

        Long visitaId = comanda.getVisita().getVisitaId();
        Mesa mesa = mesaValidador.validarOwnership(visitaId, auth);

        comanda.setComandaNotas(req.getNotas());
        comandaRepository.save(comanda);

        // No se publica /topic/mesas: las notas no alteran el ícono de borrador ni el estado de la mesa.
        return obtenerBorradorInterno(mesa);
    }

    /**
     * Envía la comanda BORRADOR de una estación a producción: transiciona a
     * PENDIENTE, sella {@code fechaHoraInicio}, valida el stock final (sin
     * decrementarlo), publica el evento de impresión por RabbitMQ y los
     * eventos WebSocket para mapa, cliente y dashboard de estación. Si la
     * mesa estaba en ESPERA pasa a EN_PREPARACION.
     *
     * @param comandaId identificador de la comanda BORRADOR a enviar
     * @param auth      contexto del usuario autenticado
     * @return borrador remanente tras el envío (la otra estación, o estructura vacía)
     */
    @Transactional
    public BorradorComandaResponse enviarAProduccion(Long comandaId, Authentication auth) {
        Comanda comanda = comandaRepository.findById(comandaId)
                .orElseThrow(() -> new ResourceNotFoundException("Comanda", comandaId));

        if (comanda.getComandaEstado() != EstadoComanda.BORRADOR) {
            throw new BusinessException(ErrorCode.INVALID_STATE,
                    "Solo comandas en BORRADOR pueden enviarse a producción", HttpStatus.CONFLICT);
        }

        Long visitaId = comanda.getVisita().getVisitaId();
        Mesa mesa = mesaValidador.validarOwnership(visitaId, auth);

        List<ComandaItem> items = comandaItemRepository
                .findByComanda_ComandaIdOrderByProductoNombreAsc(comandaId);
        validador.validarTieneItems(items);

        // Validación final de stock sin decrementar: el descuento real ocurre cuando
        // producción transiciona PENDIENTE → EN_PREPARACION. Los ítems de menú especial
        // (menuGrupo presente) están excluidos: no consumen inventario.
        for (ComandaItem it : items) {
            if (it.getComandaItemMenuGrupo() == null) {
                // El ítem ya está contabilizado en BORRADOR; cantidadAnterior = cantidad actual
                // para que la fórmula no lo doble-cuente.
                validador.validarStock(it.getProducto(), it.getComandaItemCantidad(),
                        it.getComandaItemCantidad());
            }
        }

        comanda.setComandaEstado(EstadoComanda.PENDIENTE);
        comanda.setComandaFechaHoraInicio(LocalDateTime.now());
        comandaRepository.save(comanda);

        if (mesa.getMesaEstado() == EstadoMesa.ESPERA) {
            mesa.setMesaEstado(EstadoMesa.EN_PREPARACION);
            mesaRepository.save(mesa);
            mesaWsPublisher.publicarCambioEstadoMesa(visitaId, EstadoMesa.EN_PREPARACION);
        }

        // RabbitMQ: bridge de impresión de tickets. NO alimenta los dashboards de producción.
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE,
                RabbitMQConfig.RK_COMANDA_NUEVA,
                ComandaNuevaMessage.builder()
                        .comandaId(comanda.getComandaId())
                        .visitaId(visitaId)
                        .estacion(comanda.getComandaEstacion().name())
                        .fechaHoraInicio(comanda.getComandaFechaHoraInicio())
                        .build());

        estacionWsPublisher.publicarComandaEnviada(
                ComandaEstacionWsMessage.builder()
                        .comandaId(comanda.getComandaId())
                        .visitaId(visitaId)
                        .estacion(comanda.getComandaEstacion().name())
                        .items(borradorMapper.toItemsResponse(items))
                        .build());

        publicarMesasActualizada(visitaId);
        publicarOrdenClienteActualizada(visitaId);

        return obtenerBorradorInterno(mesa);
    }

    /**
     * Cancela el formulario eliminando todas las comandas BORRADOR de la visita
     * (cocina y barra) junto con sus ítems y notas. Operación idempotente: si no
     * había borradores no hace nada y el WebSocket {@code /topic/mesas} se
     * publica de todas formas para mantener al mapa sincronizado.
     *
     * @param visitaId identificador de la visita
     * @param auth     contexto del usuario autenticado
     */
    @Transactional
    public void cancelarFormulario(Long visitaId, Authentication auth) {
        mesaValidador.validarOwnership(visitaId, auth);
        List<Comanda> comandas = comandaRepository
                .findByVisita_VisitaIdAndComandaEstado(visitaId, EstadoComanda.BORRADOR);
        if (!comandas.isEmpty()) {
            comandaRepository.deleteAll(comandas);
        }
        publicarMesasActualizada(visitaId);
    }

    /**
     * Carga el borrador completo a partir de la mesa ya validada. Centraliza el
     * fetch de comandas, el cálculo del total acumulado y el armado del DTO,
     * para que cada operación pueda devolver el estado fresco al final.
     */
    private BorradorComandaResponse obtenerBorradorInterno(Mesa mesa) {
        List<Comanda> comandas = comandaRepository
                .findByVisita_VisitaIdAndComandaEstado(mesa.getVisitaId(), EstadoComanda.BORRADOR);
        // El total acumulado abarca todas las comandas activas de la visita
        // (BORRADOR, PENDIENTE, EN_PREPARACION, LISTO); excluye COMPLETADO.
        BigDecimal totalAcumulado = comandaItemRepository
                .sumTotalActivosByVisita(mesa.getVisitaId(), EstadoComanda.COMPLETADO);
        return borradorMapper.toBorradorResponse(mesa, comandas, totalAcumulado);
    }

    /**
     * Publica la actualización del mapa de mesas para refrescar
     * {@code tieneBorrador} y el estado visual de la mesa en el frontend.
     */
    private void publicarMesasActualizada(Long visitaId) {
        mesaWsPublisher.publicarActualizacionMesa(visitaId, MesaWsPublisher.TipoEventoMesa.ACTUALIZAR);
    }

    /**
     * Publica la orden activa del cliente para que vea reflejada la nueva ronda
     * tras enviar a producción. El mapeo de ítems a {@code ItemVisitaResponse}
     * se delega al patrón existente del módulo {@code visitas}; el implementador
     * debe inyectar el mapper o servicio que ya construya esa lista en vez de
     * duplicar la lógica aquí.
     */
    private void publicarOrdenClienteActualizada(Long visitaId) {
        List<ComandaItem> items = comandaRepository.findItemsEnProduccionByVisita(visitaId);
        BigDecimal total = items.stream()
                .filter(ci -> ci.getComandaItemPrecio() != null)
                .map(ci -> ci.getComandaItemPrecio().multiply(BigDecimal.valueOf(ci.getComandaItemCantidad())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        notificacionWsPublisher.publicarVisitaActualizada(visitaId,
                VisitaActualizadaWsMessage.builder()
                        .visitaId(visitaId)
                        // items: completar con el mapper del módulo visitas (ItemVisitaResponse).
                        .total(total)
                        .build());
    }

    /**
     * Elimina los dos ítems COCINA+BARRA del mismo {@code comandaItemMenuGrupo}
     * en la visita y borra cualquier comanda que quede sin ítems.
     */
    private void eliminarParMenu(Long visitaId, String grupo) {
        List<Comanda> comandas = comandaRepository
                .findByVisita_VisitaIdAndComandaEstado(visitaId, EstadoComanda.BORRADOR);
        for (Comanda c : comandas) {
            List<ComandaItem> items = comandaItemRepository
                    .findByComanda_ComandaIdOrderByProductoNombreAsc(c.getComandaId());
            for (ComandaItem ci : items) {
                if (grupo.equals(ci.getComandaItemMenuGrupo())) {
                    comandaItemRepository.delete(ci);
                }
            }
            eliminarComandaSiVacia(c);
        }
    }

    /**
     * Borra la comanda si ya no tiene ítems asociados, para que el invariante
     * "una comanda BORRADOR existe solo cuando tiene contenido" se mantenga.
     */
    private void eliminarComandaSiVacia(Comanda comanda) {
        long restantes = comandaItemRepository
                .findByComanda_ComandaIdOrderByProductoNombreAsc(comanda.getComandaId()).size();
        if (restantes == 0) {
            comandaRepository.delete(comanda);
        }
    }
}
```

> **Nota de integración:** `publicarOrdenClienteActualizada` deja el campo `items` del WS sin poblar para evitar duplicar el mapeo a `ItemVisitaResponse`. Inyectar el mapper o servicio de `visitas` que ya construya esa lista al integrar con el módulo.

- [ ] **Step 2: Compilar**

```bash
"C:/Program Files/Apache/Maven/bin/mvn" -f backend/pom.xml clean compile -q
```

---

### Task 9: Controller ComandaController

**Files:**
- Create: `backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/controller/ComandaController.java`

- [ ] **Step 1: Crear el controller con los 7 endpoints**

```java
package co.edu.unicauca.backend.modules.mesas_comandas.controller;

import co.edu.unicauca.backend.modules.mesas_comandas.dto.request.AgregarItemRequest;
import co.edu.unicauca.backend.modules.mesas_comandas.dto.request.ModificarItemRequest;
import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.BorradorComandaResponse;
import co.edu.unicauca.backend.modules.mesas_comandas.service.ComandaBorradorService;
import co.edu.unicauca.backend.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Controller REST del formulario de modificar comanda. Toda la base es
 * {@code /api/comandas}. Cada endpoint exige rol {@code MESERO} o {@code ADMIN};
 * el rol ADMIN omite la validación de ownership de mesa.
 */
@RestController
@RequestMapping("/api/comandas")
@RequiredArgsConstructor
@Tag(name = "Comandas", description = "Modificación de comandas desde el formulario del mesero")
public class ComandaController {

    /** Servicio que concentra la lógica de borrador, envío a producción y cancelación. */
    private final ComandaBorradorService borradorService;

    /**
     * Devuelve el borrador completo de la visita. Si no hay borradores
     * devuelve estructura vacía pero válida.
     *
     * @param visitaId identificador de la visita
     * @param auth     contexto del usuario autenticado
     */
    @GetMapping("/borrador")
    @PreAuthorize("hasAnyRole('MESERO', 'ADMIN')")
    @Operation(summary = "Obtener borrador",
            description = "Carga el formulario con ítems precargados o estructura vacía")
    public ResponseEntity<ApiResponse<BorradorComandaResponse>> obtenerBorrador(
            @Parameter(description = "Identificador de la visita") @RequestParam Long visitaId,
            Authentication auth) {
        BorradorComandaResponse data = borradorService.obtenerBorrador(visitaId, auth);
        return ResponseEntity.ok(ApiResponse.ok("Borrador obtenido exitosamente", data));
    }

    /**
     * Agrega un ítem al borrador. Si el producto es bebida va a la comanda de
     * barra; si es plato, a la de cocina. Crea la comanda BORRADOR si no existe.
     *
     * @param request payload del ítem a agregar
     * @param auth    contexto del usuario autenticado
     */
    @PostMapping("/borrador/items")
    @PreAuthorize("hasAnyRole('MESERO', 'ADMIN')")
    @Operation(summary = "Agregar ítem al borrador",
            description = "Agrega un producto y opcionalmente una modificación libre con descripción")
    public ResponseEntity<ApiResponse<BorradorComandaResponse>> agregarItem(
            @Valid @RequestBody AgregarItemRequest request,
            Authentication auth) {
        BorradorComandaResponse data = borradorService.agregarItem(request, auth);
        return ResponseEntity.ok(ApiResponse.ok("Item agregado", data));
    }

    /**
     * Modifica cantidad y/o descripción de un ítem existente. Cualquier campo
     * nulo en el payload se interpreta como "no cambiar".
     *
     * @param itemId  identificador del {@code comanda_item}
     * @param request cambios a aplicar
     * @param auth    contexto del usuario autenticado
     */
    @PatchMapping("/borrador/items/{itemId}")
    @PreAuthorize("hasAnyRole('MESERO', 'ADMIN')")
    @Operation(summary = "Modificar cantidad o descripción de un ítem",
            description = "Actualiza el ítem con validación de stock y techo de 250 unidades")
    public ResponseEntity<ApiResponse<BorradorComandaResponse>> modificarItem(
            @PathVariable Long itemId,
            @Valid @RequestBody ModificarItemRequest request,
            Authentication auth) {
        BorradorComandaResponse data = borradorService.modificarItem(itemId, request, auth);
        return ResponseEntity.ok(ApiResponse.ok("Item modificado", data));
    }

    /**
     * Elimina un ítem del borrador. Si es parte de un par menú especial elimina
     * ambos ítems (cocina y barra). Si es un ítem base elimina también todos
     * sus ítems modificados con el mismo {@code productoId}.
     *
     * @param itemId identificador del {@code comanda_item}
     * @param auth   contexto del usuario autenticado
     */
    @DeleteMapping("/borrador/items/{itemId}")
    @PreAuthorize("hasAnyRole('MESERO', 'ADMIN')")
    @Operation(summary = "Eliminar ítem del borrador",
            description = "Elimina el ítem; en menú especial elimina el par cocina+barra del mismo grupo")
    public ResponseEntity<ApiResponse<BorradorComandaResponse>> eliminarItem(
            @PathVariable Long itemId,
            Authentication auth) {
        BorradorComandaResponse data = borradorService.eliminarItem(itemId, auth);
        return ResponseEntity.ok(ApiResponse.ok("Item eliminado", data));
    }

    /**
     * Envía a producción la comanda BORRADOR de una estación: transiciona a
     * PENDIENTE, valida stock, publica RabbitMQ y eventos WebSocket.
     *
     * @param comandaId identificador de la comanda a enviar
     * @param auth      contexto del usuario autenticado
     */
    @PostMapping("/borrador/{comandaId}/enviar")
    @PreAuthorize("hasAnyRole('MESERO', 'ADMIN')")
    @Operation(summary = "Enviar comanda a producción",
            description = "Transición BORRADOR → PENDIENTE con validación final de stock y eventos asincrónicos")
    public ResponseEntity<ApiResponse<BorradorComandaResponse>> enviarAProduccion(
            @PathVariable Long comandaId,
            Authentication auth) {
        BorradorComandaResponse data = borradorService.enviarAProduccion(comandaId, auth);
        return ResponseEntity.ok(ApiResponse.ok("Comanda enviada a producción", data));
    }

    /**
     * Cancela el formulario eliminando todas las comandas BORRADOR de la visita
     * (cocina y barra) junto con sus ítems y notas.
     *
     * @param visitaId identificador de la visita
     * @param auth     contexto del usuario autenticado
     */
    @DeleteMapping("/borrador")
    @PreAuthorize("hasAnyRole('MESERO', 'ADMIN')")
    @Operation(summary = "Cancelar formulario",
            description = "Elimina todas las comandas BORRADOR de la visita descartando los cambios")
    public ResponseEntity<ApiResponse<Void>> cancelarFormulario(
            @RequestParam Long visitaId,
            Authentication auth) {
        borradorService.cancelarFormulario(visitaId, auth);
        return ResponseEntity.ok(ApiResponse.ok("Cambios descartados", null));
    }

    /**
     * Persiste las notas de una comanda BORRADOR (cocina o barra). Cada estación
     * tiene su propio campo de notas, por lo que la petición identifica la
     * comanda y no la visita.
     *
     * @param comandaId identificador de la comanda BORRADOR
     * @param request   payload con el nuevo valor de notas (puede ser nulo para borrar)
     * @param auth      contexto del usuario autenticado
     */
    @PatchMapping("/borrador/{comandaId}/notas")
    @PreAuthorize("hasAnyRole('MESERO', 'ADMIN')")
    @Operation(summary = "Actualizar notas de la comanda",
            description = "Persiste en tiempo real las notas de cocina o barra del borrador")
    public ResponseEntity<ApiResponse<BorradorComandaResponse>> actualizarNotas(
            @PathVariable Long comandaId,
            @Valid @RequestBody NotasRequest request,
            Authentication auth) {
        BorradorComandaResponse data = borradorService.actualizarNotas(comandaId, request, auth);
        return ResponseEntity.ok(ApiResponse.ok("Notas actualizadas", data));
    }
}
```

- [ ] **Step 2: Compilar**

```bash
"C:/Program Files/Apache/Maven/bin/mvn" -f backend/pom.xml clean compile -q
```

---

### Task 10: Documentación y registro de endpoints

**Files:**
- Modify: `backend/ENDPOINTS.md`

- [ ] **Step 1: Agregar sección "Comandas" tras "Mesas"**

```markdown
## Comandas (`/api/comandas`)

| Method | Endpoint | Acceso | Descripción |
|--------|----------|--------|-------------|
| GET | `/borrador?visitaId=` | **MESERO / ADMIN** | Carga el formulario. Ítems precargados desde pre-orden si existe; vacío en walk-in. Valida ownership de la mesa. |
| POST | `/borrador/items` | **MESERO / ADMIN** | Agrega ítem al borrador (PLATO→COCINA, BEBIDA→BARRA). Soporta descripción opcional para modificación libre. Valida stock. |
| PATCH | `/borrador/items/{itemId}` | **MESERO / ADMIN** | Modifica cantidad y/o descripción del ítem; valida stock y techo de 250. |
| DELETE | `/borrador/items/{itemId}` | **MESERO / ADMIN** | Elimina ítem. Si es parte de un menú especial elimina el par cocina+barra. |
| POST | `/borrador/{comandaId}/enviar` | **MESERO / ADMIN** | BORRADOR → PENDIENTE. Valida stock sin decrementarlo (el descuento ocurre al transicionar a EN_PREPARACION). Publica `comanda.nueva` en RabbitMQ (bridge impresión), WS `/topic/estacion/{estacion}` (dashboard producción), `/topic/mesas` y `/topic/visita/{visitaId}/orden`. |
| DELETE | `/borrador?visitaId=` | **MESERO / ADMIN** | Elimina todas las comandas BORRADOR de la visita; dispara WS `/topic/mesas`. |
| PATCH | `/borrador/{comandaId}/notas` | **MESERO / ADMIN** | Persiste las notas (cocina o barra) de una comanda BORRADOR. |
```

- [ ] **Step 2: Agregar a "Productos"**

```markdown
| GET | `/buscar?q=` | **Authenticated** | Búsqueda parcial case-insensitive de productos del catálogo, excluye menús especiales. |
```

- [ ] **Step 3: Agregar nota WebSocket en sección "WebSocket Integration"**

```markdown
- `POST /api/comandas/borrador/{comandaId}/enviar` → `/topic/estacion/{estacion}` (COCINA o BARRA) + `/topic/mesas` + `/topic/visita/{visitaId}/orden` + RabbitMQ `comanda.nueva`
- `POST /api/comandas/borrador/items` → `/topic/mesas`
- `PATCH /api/comandas/borrador/items/{itemId}` → `/topic/mesas`
- `DELETE /api/comandas/borrador/items/{itemId}` → `/topic/mesas`
- `DELETE /api/comandas/borrador?visitaId=` → `/topic/mesas`
```

- [ ] **Step 4: Compilar y verificar build limpio**

```bash
"C:/Program Files/Apache/Maven/bin/mvn" -f backend/pom.xml clean compile -q
```

Expected: BUILD SUCCESS sin warnings nuevos. Si Spring no levantara por inyección, revisar que todos los `@Component`/`@Service` estén anotados y que `RabbitTemplate` esté disponible (lo está vía `RabbitMQConfig`).

---

## Self-Review

**Cobertura de spec (numeración nueva CA-01 a CA-11):**
| CA | Tarea cubre | Endpoint |
|----|-------------|----------|
| CA-01 | Task 7+8+notas (mapper+service+notas) | GET /borrador + PATCH /borrador/{id}/notas |
| CA-02 | Task 2 | GET /productos/buscar |
| CA-03 | Task 6+8 (validador stock + agregar) | POST /borrador/items |
| CA-04 | Task 8 (agregar con descripcion) | POST /borrador/items |
| CA-05 | Task 8 (modificar + stock) | PATCH /borrador/items/{id} |
| CA-06 | Task 4+6 (DTO @Min/@Max + validador) | (transversal) |
| CA-07 | Task 8 (eliminar base + modificados) | DELETE /borrador/items/{id} |
| CA-08 | Task 7 (flags puedeEnviarCocina/Barra) | (flags en BorradorComandaResponse) |
| CA-09 | Task 8 (enviarAProduccion + WS + RabbitMQ + mesa→EN_PREPARACION) | POST /borrador/{id}/enviar |
| CA-10 | Sin endpoint (frontend cierra; persistencia incremental ya activa) | — |
| CA-11 | Sin endpoint (frontend redirige al mapa) | — |
| CA-01 Cancelar | Task 8 (cancelarFormulario) | DELETE /borrador?visitaId= |

**Numeración de CAs:** alineada con spec 2026-05-09 (CA-01 a CA-11). Tabla de cobertura actualizada arriba.

**Placeholder scan:** el plan ya no contiene stubs en el mapper. Único punto de integración pendiente: `publicarOrdenClienteActualizada` en Task 8 deja `items` del `VisitaActualizadaWsMessage` sin poblar para que el implementador inyecte el mapper de `visitas` que produce `ItemVisitaResponse` en lugar de duplicar la lógica.

**Type consistency:** `EstacionComanda` (COCINA/BARRA), `EstadoComanda` (BORRADOR/PENDIENTE/...), `CategoriaProducto` (PLATO/BEBIDA) usados consistentemente. Métodos `findByVisita_VisitaIdAndComandaEstado` y `findByVisita_VisitaIdAndComandaEstadoAndComandaEstacion` referenciados igual en repo y service.

**WebSocket consistency:** todos los call sites de `mesaWsPublisher.publicarActualizacionMesa(visitaId, ACTUALIZAR)` usan la misma firma; `notificacionWsPublisher.publicarVisitaActualizada` y `simpMessagingTemplate.convertAndSend("/topic/estacion/...")` solo se invocan en `enviarAProduccion`. RabbitMQ solo en `enviarAProduccion` (bridge de impresión). **Se introduce un tópico nuevo** `/topic/estacion/{estacion}` para dashboards de producción — registrar en `docs/coding-patterns.md` y en `ENDPOINTS.md` sección WebSocket.

---

## Execution Handoff

Plan guardado en `docs/superpowers/plans/2026-05-08-modificar-comanda-hu05.md`. Dos opciones de ejecución:

1. **Subagent-Driven (recomendado)** — Despacho un subagente fresco por tarea, reviso entre tareas, iteración rápida.
2. **Inline Execution** — Ejecuto las tareas en esta sesión usando `executing-plans`, con checkpoints de revisión.

> **Antes de ejecutar:** la sección 6 (Functional Clarifications) tiene 9 preguntas pendientes. Recomiendo confirmarlas primero — algunas (CA-15 alcance, stock comprometido) cambian el comportamiento observable.

¿Qué aproximación prefieres? ¿Y quieres revisar/responder las 9 clarificaciones funcionales antes de empezar?
