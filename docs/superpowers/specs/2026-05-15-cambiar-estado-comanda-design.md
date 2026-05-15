# Diseño — Cambiar y notificar estado de comanda (HE-04-HU-03 / PA-102)

**Rama:** `PA-102-cambiar-estado-comanda`
**Fecha:** 2026-05-15
**Autora:** Paula Muñoz

> Este spec se rige por los documentos de convenciones del proyecto:
> `backend/docs/api-conventions.md`, `backend/docs/coding-patterns.md`,
> `backend/docs/testing.md`, `backend/docs/postman-conventions.md`,
> y las reglas críticas de `backend/CLAUDE.md`.

## 1. Resumen ejecutivo

Habilitar al personal de producción (cocina/barra) para mover una comanda entre las columnas Pendiente → En preparación → Listo del tablero de su estación, descontar el inventario al iniciar y notificar al mesero (notificaciones existentes y mecanismo manual de cambio). Verificar el flujo en tiempo real para el dashboard del cliente y arreglar el mapping de estados visibles. Excluye CA-01 (ya en PA-99), CA-03 (impresión, futuro), CA-06 (ya en `servirPlatos/Bebidas`).

## 2. Scope

### Incluye
- **CA-02** Notificar cambio: endpoint para que producción cree una notificación `CAMBIO` sobre una comanda PENDIENTE de su estación.
- **CA-04** Iniciar preparación: PENDIENTE → EN_PREPARACION + descuento de inventario (dual VENTA_DIRECTA/PREPARACION) + `comandaFechaHoraInicio`.
- **CA-05** Marcar listo: EN_PREPARACION → LISTO + `comandaFechaHoraListo` + creación automática de `Notificacion(PLATOS_LISTOS|BEBIDAS_LISTAS)`.
- Refactor de `ComandaBorradorValidador.validarStock` con ramas por `TipoProducto`.
- Nuevo evento WS `ACTUALIZADA` en `TipoEventoProduccion` con campo `nuevoEstado`.
- Fix del mapping `VisitaEstadoMapper.resolverEstadoItem` (PENDIENTE → "En espera"; mantener BORRADOR).
- Revisión exhaustiva: cada servicio que mute comandas o items debe publicar a `/topic/visita/{id}/orden` vía `NotificacionWsPublisher.publicarVisitaActualizada`.

### Excluye
- CA-01, CA-03, CA-06.
- Validación de stock al transicionar PRE_RESERVA→BORRADOR y warnings al obtener borrador → ticket aparte (ver sección 12).

## 3. Decisiones confirmadas

| Tema | Decisión |
|---|---|
| Endpoints CA-04/05 | `POST /api/comandas/produccion/{comandaId}/iniciar` y `/listo` (`ComandaProduccionController`) |
| Endpoint CA-02 | `POST /api/notificaciones/cambio` (`NotificacionController`, body con `comandaId`) |
| Notificación al pasar a LISTO | Crear automáticamente `PLATOS_LISTOS` o `BEBIDAS_LISTAS` según `EstacionComanda` |
| Descuento VENTA_DIRECTA | Restar `comandaItemCantidad` de `Producto.stockActual` (skip si `stockActual` null) |
| Descuento PREPARACION | Por cada `Receta`: restar `recetaCantidad × comandaItemCantidad` de `Insumo.stockActual` |
| Menús especiales | `comandaItemMenuGrupo IS NOT NULL` → no descontar |
| Auditoría inventario | Crear `MovimientoInventario(tipo=EGRESO, empleado=actor)` por cada producto/insumo descontado |
| Orden en CA-04 | Descuento PRIMERO; cambio de estado solo si descuento exitoso (rollback atómico vía `@Transactional`) |
| Validador en borrador | Dual: VENTA_DIRECTA contra `Producto.stockActual`; PREPARACION contra disponibilidad de insumos en receta |
| WS estación | Evento `ACTUALIZADA` con `nuevoEstado` en payload, vía `NotificacionWsPublisher.publicarEventoProduccion` |
| WS cliente | `NotificacionWsPublisher.publicarVisitaActualizada(visitaId)` al final de cada transición |
| Mapping cliente | BORRADOR/PENDIENTE → "En espera"; EN_PREPARACION → "En preparación"; LISTO/COMPLETADO → "Servido" |
| Cobertura objetivo | Máxima posible, mínimo por capa según `docs/testing.md` |
| Postman | YAML (VS Code plugin), 1 manual por endpoint + colección automatizada con cobertura completa |
| Commits | No commits automáticos — entregar `git add` y mensaje propuesto en español, formato `<tipo>(<módulo>): ...` con co-author |

## 4. Endpoints (siguiendo `api-conventions.md`)

| Método | Ruta | Auth | Body | Respuesta |
|---|---|---|---|---|
| POST | `/api/comandas/produccion/{comandaId}/iniciar` | `hasRole('PRODUCCION')` + `EstacionResolver` | vacío | `ApiResponse<ComandaProduccionResumenResponse>` |
| POST | `/api/comandas/produccion/{comandaId}/listo` | `hasRole('PRODUCCION')` + `EstacionResolver` | vacío | `ApiResponse<ComandaProduccionResumenResponse>` |
| POST | `/api/notificaciones/cambio` | `hasRole('PRODUCCION')` + `EstacionResolver` | `NotificarCambioRequest{ Long comandaId }` con `@NotNull` | `ApiResponse<NotificarCambioResponse{ notificacionId, estado }>` |

**Cumple convenciones:** plural (`comandas`, `notificaciones`); kebab-case (no aplica segmentos compuestos); `{comandaId}` camelCase; sustantivos no verbos (acción `iniciar`/`listo` como subrecurso de la comanda); `@PreAuthorize` por rol, no prefijo en URL.

Uso de `ApiResponse` (factory de `coding-patterns.md`):
- happy iniciar/listo → `ApiResponse.ok("Comanda iniciada/lista", resumen)`.
- happy notificar cambio → `ApiResponse.created("Notificación de cambio creada", response)`.

## 5. Lógica detallada (sigue patrón `Service` de `coding-patterns.md`)

Todos los servicios usan `@RequiredArgsConstructor`, sin `@Slf4j`, Javadoc paso a paso. 404 con `ResourceNotFoundException("Comanda", id)`; reglas de negocio con `BusinessException(ErrorCode, mensaje, HttpStatus)`.

### 5.1 `ComandaProduccionService.iniciarPreparacion(Long comandaId, Authentication auth)` — `@Transactional`
1. `comandaRepository.findById(comandaId).orElseThrow(() -> new ResourceNotFoundException("Comanda", comandaId))`.
2. Validar `comanda.comandaEstado == PENDIENTE` → si no, `BusinessException(INVALID_STATE, "...", CONFLICT)`.
3. Validar estación del actor coincide con `comanda.comandaEstacion` mediante `EstacionResolver` → si no, `BusinessException(ACCESS_DENIED, "...", FORBIDDEN)`.
4. **Descontar inventario** (`InventarioDescuentoService.descontarPorComanda(comanda, empleado)`). Si lanza → rollback total, comanda intacta.
5. `comanda.setComandaEstado(EN_PREPARACION); comanda.setComandaFechaHoraInicio(LocalDateTime.now()); comandaRepository.save(comanda);`.
6. `wsPublisher.publicarEventoProduccion(comanda.getComandaEstacion(), new ComandaProduccionEventoWsMessage(ACTUALIZADA, estacion, comandaId, null, "EN_PREPARACION"))`.
7. `wsPublisher.publicarVisitaActualizada(new VisitaActualizadaWsMessage(visitaId))`.
8. Retornar `produccionMapper.toResumen(comanda)`.

### 5.2 `ComandaProduccionService.marcarListo(Long comandaId, Authentication auth)` — `@Transactional`
1. Cargar comanda; 404 si no existe.
2. Validar `comandaEstado == EN_PREPARACION` → 409 si no.
3. Validar estación del actor.
4. `comanda.setComandaEstado(LISTO); comanda.setComandaFechaHoraListo(now()); save`.
5. Crear `Notificacion(tipo = estacion==COCINA ? PLATOS_LISTOS : BEBIDAS_LISTAS, comanda, mesa = visita.mesa, estado = ACTIVA)` y persistir.
6. `wsPublisher.publicarEventoProduccion(estacion, ACTUALIZADA(nuevoEstado="LISTO"))`.
7. `wsPublisher.publicarVisitaActualizada(visitaId)`.
8. `mesaWsPublisher.publicarActualizacionMesa(visitaId, NOTIFICACION)` (señal-no-data al mapa).
9. Retornar resumen.

### 5.3 `NotificacionService.notificarCambio(Long comandaId, Authentication auth)` — `@Transactional`
1. Cargar comanda; 404.
2. Validar `comandaEstado == PENDIENTE` → 409 (CAMBIO solo aplica sobre pendientes).
3. Validar estación del actor.
4. Si existe `Notificacion(comanda, tipo=CAMBIO, estado=ACTIVA)` → 409 (no duplicar).
5. Persistir `Notificacion(tipo=CAMBIO, comanda, mesa=visita.mesa, estado=ACTIVA)`.
6. `mesaWsPublisher.publicarActualizacionMesa(visitaId, NOTIFICACION)`.
7. Retornar `NotificarCambioResponse(notificacionId, "ACTIVA")`.

## 6. Descuento de inventario — `InventarioDescuentoService` (módulo `inventario`)

`@Service @RequiredArgsConstructor` + `@Transactional` (propaga la transacción del llamador).

```
descontarPorComanda(Comanda comanda, Empleado actor):
    items = comandaItemRepository.findByComanda_ComandaIdOrderByProductoNombreAsc(comanda.id)
    para cada item con item.comandaItemMenuGrupo == null:
        producto = item.producto
        si producto.productoTipo == VENTA_DIRECTA:
            si producto.stockActual != null:
                nuevo = producto.stockActual - item.cantidad
                si nuevo < 0: throw INSUFFICIENT_STOCK
                producto.stockActual = nuevo; productoRepository.save(producto)
                movimientoRepository.save(MovimientoInventario.builder()
                    .producto(producto).movimientoCantidad(item.cantidad)
                    .movimientoTipo(EGRESO).empleado(actor).build())
        else si producto.productoTipo == PREPARACION:
            recetas = recetaRepository.findByProductoId(producto.id)
            para cada receta:
                cantidad = receta.recetaCantidad * item.cantidad
                nuevo = receta.insumo.insumoStockActual - cantidad
                si nuevo < 0: throw INSUFFICIENT_STOCK("... insumo: <nombre>")
                receta.insumo.insumoStockActual = nuevo; insumoRepository.save(receta.insumo)
                movimientoRepository.save(MovimientoInventario(insumo, cantidad, EGRESO, actor))
```

`INSUFFICIENT_STOCK` es `ErrorCode` existente (HTTP 409). Errores propagan el rollback del `@Transactional`.

## 7. Refactor `ComandaBorradorValidador.validarStock`

Ramificar por `producto.productoTipo`:
- VENTA_DIRECTA → fórmula actual.
- PREPARACION → para cada `Receta` del producto: `comprometido` = nueva query `sumCantidadInsumoComprometida(insumoId)` (ítems en BORRADOR/PENDIENTE de productos PREPARACION usando ese insumo). Validar `recetaCantidad × nuevaCantidad ≤ insumo.stockActual − (comprometido − recetaCantidad × cantidadAnterior)`.
- Mensaje de error señala insumo limitante.

Nueva query en `ComandaItemRepository`:
```java
@Query("""
    SELECT COALESCE(SUM(r.recetaCantidad * ci.comandaItemCantidad), 0)
    FROM ComandaItem ci
    JOIN Receta r ON r.productoId = ci.producto.productoId
    WHERE r.insumoId = :insumoId
      AND ci.comandaItemMenuGrupo IS NULL
      AND ci.producto.productoTipo = co.edu.unicauca.backend.shared.enums.TipoProducto.PREPARACION
      AND ci.comanda.comandaEstado IN (
          co.edu.unicauca.backend.shared.enums.EstadoComanda.BORRADOR,
          co.edu.unicauca.backend.shared.enums.EstadoComanda.PENDIENTE)
""")
BigDecimal sumCantidadInsumoComprometida(@Param("insumoId") Long insumoId);
```

`RecetaRepository.findByProductoId(Long)` — agregar si no existe.

## 8. WebSocket (sigue patrón `coding-patterns.md`)

- Extender `enum TipoEventoProduccion` con `ACTUALIZADA`.
- Agregar campo opcional `String nuevoEstado` (Jackson `@JsonInclude(NON_NULL)`) a `ComandaProduccionEventoWsMessage`. Solo viaja en `ACTUALIZADA`. Mantener compat con `CREADA/ELIMINADA/COMPLETADA`.
- Toda emisión a `/topic/produccion/{cocina|barra}` pasa por `NotificacionWsPublisher.publicarEventoProduccion(...)`. Publicar **después de persistir**.
- Cliente: `publicarVisitaActualizada(new VisitaActualizadaWsMessage(visitaId))` al final de cada transición.
- **Auditoría de consistencia (F9):** revisar `ComandaBorradorService.confirmarBorrador`, `NotificacionService.atenderCambio/servirPlatos/servirBebidas` y demás mutadores. Cada uno termina con un único publish al cliente. Test de integración por call site.

Actualizar `backend/CLAUDE.md` (sección WebSocket) y `backend/docs/coding-patterns.md` (patrón "evento unificado de producción") para incluir `ACTUALIZADA` y el campo `nuevoEstado`.

## 9. DTOs y entidades

Sigue reglas de `coding-patterns.md` (`@Getter @Builder` + final, enums como `String` vía `.name()`, Javadoc por campo).

- `TipoEventoProduccion`: + `ACTUALIZADA`.
- `ComandaProduccionEventoWsMessage`: + `String nuevoEstado` (NON_NULL).
- `NotificarCambioRequest` (nuevo, `mesas_comandas/dto/request`): `{ @NotNull Long comandaId }`.
- `NotificarCambioResponse` (nuevo, `notificaciones/dto/response`): `{ Long notificacionId, String estado }`.

## 10. Mapping cliente

`VisitaEstadoMapper.resolverEstadoItem`:
```java
return switch (estado) {
    case BORRADOR, PENDIENTE -> "En espera";
    case EN_PREPARACION      -> "En preparación";
    case LISTO, COMPLETADO   -> "Servido";
    default                  -> "En espera"; // PRE_RESERVA defensivo
};
```
Actualizar Javadoc del campo `ItemVisitaResponse.estadoItem` y del mapper.

## 11. Pruebas — sigue `testing.md` y `coding-patterns.md`

### 11.1 Cobertura mínima por capa (`testing.md`)
| Capa | Mínimo | Objetivo en este PR |
|---|---|---|
| Services | 90–95% | 100% |
| Controllers | 85–90% | 95–100% |
| Mappers | 90–95% | 100% |
| Validators | 95%+ | 100% |
| Repositories (custom queries) | 70–80% | 90%+ (cubrir nueva query) |

Branch coverage en cada `if/else`, `switch`, `try/catch` (regla `testing.md`).

### 11.2 Service Tests (patrón obligatorio)
`@ExtendWith(MockitoExtension.class) @MockitoSettings(strictness = Strictness.LENIENT)` + helpers privados + `@Nested @DisplayName` por método. Nombres: `condicion_resultadoEsperado()`.

Clases:
- `ComandaProduccionServiceTest` — `iniciarPreparacion` y `marcarListo`: happy COCINA y BARRA, estado inválido, estación incorrecta, comanda inexistente, descuento falla → no transiciona, publish WS.
- `InventarioDescuentoServiceTest` — todas las ramas: VENTA_DIRECTA con/sin stock, suficiente/insuficiente; PREPARACION receta vacía/N insumos/insumo insuficiente; menú especial omitido; comanda mixta; persistencia de `MovimientoInventario`.
- `ComandaBorradorValidadorTest` — extender con casos PREPARACION (suficiente, insuficiente por receta, comprometido por terceros, `cantidadAnterior > 0`).
- `NotificacionServiceTest` — `notificarCambio` happy + duplicado + estado inválido + estación incorrecta; tests existentes intactos.
- `VisitaEstadoMapperTest` — los 5 estados + null defensivo.

### 11.3 Controller Tests (patrón obligatorio)
`@WebMvcTest(controllers = X.class)` + `PermissiveSecurityConfig` interna + `@MockitoBean` (Spring Boot 3.4+) para service, `JwtTokenProvider`, `UserDetailsService`, `SesionRepository`. `@WithMockUser(username, roles)`.

- `ComandaProduccionControllerTest` (extender) — `iniciar` y `listo`: 200 OK, 401 sin token, 403 rol incorrecto / estación incorrecta, 404, 409 estado inválido / stock insuficiente.
- `NotificacionControllerTest` (extender) — `cambio`: 200/201, 400 body sin `comandaId`, 401, 403, 404, 409 (duplicado y estado inválido).

### 11.4 Repository Test
- `ComandaItemRepositoryTest` (`@DataJpaTest` con H2Dialect, sigue patrón existente) — `sumCantidadInsumoComprometida`: insumo sin uso, insumo en BORRADOR, insumo en PENDIENTE, exclusión de menú especial, exclusión de VENTA_DIRECTA, exclusión de COMPLETADO.

### 11.5 Postman — Manual (`backend/postman/manual-testing/`)
Sigue `postman-conventions.md`:
- Formato YAML (Postman for VS Code).
- 1 archivo por endpoint, formato `XX-YY Descripción – ROL.request.yaml`.
- `beforeRequest` con login autónomo `pm.sendRequest`, password `Al.Toro2026!`, token con prefijo `tmp` (`tmpCocineroToken`/`tmpBartenderToken`).
- `afterResponse` solo cleanup (`pm.environment.unset`).
- No tests automatizados.

Numeración (confirmada):
- **70-07 Notificar cambio – COCINERO.request.yaml** — siguiente disponible en el rango de Notificaciones (último existente: `70-06 Atender cambio – sin borrador`).
- **90-12 Iniciar preparación – COCINERO.request.yaml** — siguiente disponible en el rango de Comandas/Producción (último existente: `90-11 Detalle comanda produccion – COCINERO`).
- **90-13 Marcar listo – COCINERO.request.yaml**.

Recomendado además crear variantes BARTENDER si el seed lo permite (sin numeración fija; usar `90-12 ... – BARTENDER` etc. solo si aporta cobertura manual real).

### 11.6 Postman — Automated (`backend/postman/collections/`)
Sigue `postman-conventions.md`:
- Carpeta nueva `comandas-produccion-estado/` con `.resources/definition.yaml` (collection-level hooks).
- Variables de entorno: `cocineroToken`, `bartenderToken`, `passwordValida`, `emailCocinero`, `emailBartender`.
- Login autónomo en `beforeRequest`; cleanup en el `beforeRequest` siguiente; `afterResponse` solo tests + guardar IDs.
- Independencia obligatoria (cada test corre solo).
- Códigos de error **serializados**: `NEG-001`, `NEG-002`, `AUTH-001`, `AUTH-002`, `ENT-001`, `VAL-001` (NO el enum).
- Estado limpio prerrequisito: documentar el script SQL si introducimos seeds nuevos en `V3__dev_data.sql` (sigue convención de no superar V5).
- 1 carpeta por endpoint con cobertura completa según `postman-conventions.md` (200, 401, 403, 404, 409, 400 según aplique).

Casos planificados:
- Iniciar preparación: 200 cocina, 200 barra, 401, 403 rol incorrecto (MESERO/CLIENTE/CAJERO), 403 estación incorrecta, 404 inexistente, 409 estado inválido, 409 stock VENTA_DIRECTA insuficiente, 409 stock PREPARACION (insumo limitante), 200 menú especial sin descuento.
- Marcar listo: 200 cocina (verifica creación PLATOS_LISTOS), 200 barra (BEBIDAS_LISTAS), 401, 403 rol/estación, 404, 409 estado inválido.
- Notificar cambio: 201 happy, 400 body inválido, 401, 403 rol/estación, 404, 409 duplicado, 409 estado inválido.

### 11.7 Comando final
`./mvnw clean test jacoco:report` antes del commit (regla `testing.md`).

## 12. Recomendaciones fuera de scope (ticket nuevo)

**Validación PRE_RESERVA→BORRADOR + warning al obtener borrador.** Hoy `MesaAsignarService.procesarReserva` transiciona sin validar stock; las pre-órdenes no se cuentan como comprometidas mientras estén en PRE_RESERVA. Propuesta: cada `GET` de borrador ejecuta el validador dual y pobla `advertenciasPreorden: [{productoId, nombre, cantidadEnComanda, cantidadDisponible}]` en la respuesta. Sobrevive a refresh (recalcula en cada GET), sin entidad nueva ni notificación. La transición a BORRADOR ocurre igual (la preorden debe quedar modificable). Crear ticket Jira aparte.

## 13. Documentación a actualizar

- `backend/ENDPOINTS.md`: agregar 3 endpoints nuevos.
- `backend/CLAUDE.md`: ampliar sección WebSocket con `ACTUALIZADA` y `nuevoEstado`.
- `backend/docs/coding-patterns.md`: actualizar el bloque "patrón evento unificado de producción" para reflejar `ACTUALIZADA`.
- Javadoc formal en clases/métodos nuevos (estilo formal, sin referencias a HU/Jira/plan, regla `feedback_javadoc_style`).

## 14. Fases del plan

| Fase | Descripción |
|---|---|
| F0 | Aprobación de spec |
| F1 | Enum/DTO/payload WS (`TipoEventoProduccion.ACTUALIZADA`, `nuevoEstado`, `NotificarCambioRequest/Response`) |
| F2 | Repositorios: `sumCantidadInsumoComprometida`, `RecetaRepository.findByProductoId` (si falta) — con test `@DataJpaTest` |
| F3 | `InventarioDescuentoService` + `InventarioDescuentoServiceTest` (TDD) |
| F4 | Refactor `ComandaBorradorValidador` (rama PREPARACION) + tests extendidos |
| F5 | `ComandaProduccionService.iniciarPreparacion/marcarListo` + service test |
| F6 | Endpoints `iniciar`/`listo` en `ComandaProduccionController` + `@WebMvcTest` |
| F7 | `NotificacionService.notificarCambio` + endpoint `POST /api/notificaciones/cambio` + tests |
| F8 | Publicar WS (`ACTUALIZADA` y `publicarVisitaActualizada`) en transiciones nuevas vía publisher |
| F9 | Fix `VisitaEstadoMapper` + revisión exhaustiva consistencia WS al cliente; agregar publish donde falte |
| F10 | Postman manual (3 requests YAML) + automatizado (carpeta `comandas-produccion-estado/`) según `postman-conventions.md` |
| F11 | Documentación (`ENDPOINTS.md`, `CLAUDE.md`, `coding-patterns.md`, `postman-conventions.md` si aplica) |
| F12 | `./mvnw clean test jacoco:report` (cobertura ≥ mínimos por capa, objetivo 100% en clases nuevas) + smoke. Reportar `git add` y mensaje propuesto, sin commit automático |

## 15. Constraints

- No commits automáticos (`feedback_no_commits`).
- Cobertura objetivo 100% en clases nuevas; mínimos por capa de `testing.md` no negociables.
- Tests existentes verdes en cada fase.
- Estilo Javadoc formal (`feedback_javadoc_style`).
- No tocar el frontend (`backend/CLAUDE.md`).
- No `@Slf4j` ni `log.debug()` en services.
- Mappers con comparador estático y orden por categoría ANTES de mapear.
- Códigos de error serializados en assertions Postman.
