# Cambiar y notificar estado de comanda — Plan de Implementación (PA-102)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.
>
> **Project rule (`feedback_no_commits`):** Nunca ejecutar `git commit` automáticamente. Cada tarea termina con un paso "Reportar add+mensaje propuesto" — la usuaria revisa y ejecuta el commit.

**Goal:** Implementar las transiciones de estado de comanda en producción (Pendiente→EnPreparación→Listo) con descuento de inventario dual (VENTA_DIRECTA contra Producto.stockActual; PREPARACION contra Insumo.stockActual vía Receta), endpoint para notificar cambio desde producción, sincronización WebSocket coherente con el cliente, y fix del mapping de estado visible al cliente.

**Architecture:** Tres endpoints REST nuevos (`POST /api/comandas/produccion/{id}/iniciar`, `/listo`, `POST /api/notificaciones/cambio`). Service `ComandaProduccionService` extendido con métodos transaccionales que orquestan validación de estado/estación + descuento de inventario (servicio nuevo `InventarioDescuentoService`) + persistencia + publicación WS. `ComandaBorradorValidador` ramificado por `TipoProducto`. WS unificado por `NotificacionWsPublisher` ya existente con nuevo tipo de evento `ACTUALIZADA`.

**Tech Stack:** Spring Boot 3.5, Java 21, JPA/Hibernate, PostgreSQL 15, JUnit 5, Mockito, MockMvc, Postman (YAML, Postman for VS Code).

**Spec:** `docs/superpowers/specs/2026-05-15-cambiar-estado-comanda-design.md`.

---

## File Structure

### Crear
- `backend/src/main/java/co/edu/unicauca/backend/modules/inventario/service/InventarioDescuentoService.java` — descuento dual con auditoría.
- `backend/src/main/java/co/edu/unicauca/backend/modules/inventario/repository/RecetaRepository.java` — si no existe.
- `backend/src/main/java/co/edu/unicauca/backend/modules/inventario/repository/InsumoRepository.java` — si no existe.
- `backend/src/main/java/co/edu/unicauca/backend/modules/inventario/repository/MovimientoInventarioRepository.java` — si no existe.
- `backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/dto/request/NotificarCambioRequest.java`.
- `backend/src/main/java/co/edu/unicauca/backend/modules/notificaciones/dto/response/NotificarCambioResponse.java`.
- Tests: `InventarioDescuentoServiceTest`, ampliar `ComandaProduccionServiceTest`, `ComandaProduccionControllerTest`, `NotificacionServiceTest`, `NotificacionControllerTest`, `ComandaItemRepositoryTest`, `ComandaBorradorValidadorTest`, `VisitaEstadoMapperTest`.
- Postman manuales: `70-07`, `90-12`, `90-13`.
- Postman automated: carpeta `comandas-produccion-estado/`.

### Modificar
- `backend/src/main/java/co/edu/unicauca/backend/modules/notificaciones/dto/ws/TipoEventoProduccion.java` — agregar `ACTUALIZADA`.
- `backend/src/main/java/co/edu/unicauca/backend/modules/notificaciones/dto/ws/ComandaProduccionEventoWsMessage.java` — agregar `String nuevoEstado`.
- Call-sites del record antiguo (2): `NotificacionService.servirPlatos`, `NotificacionService.servirBebidas`, `NotificacionService.atenderCambio`, `ComandaBorradorService.confirmarBorrador`.
- `backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/repository/ComandaItemRepository.java` — query `sumCantidadInsumoComprometida`.
- `backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/service/ComandaBorradorValidador.java` — rama PREPARACION.
- `backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/service/ComandaProduccionService.java` — añadir `iniciarPreparacion`, `marcarListo`, `notificarCambio` no, ese va en `NotificacionService`.
- `backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/controller/ComandaProduccionController.java` — añadir 2 endpoints.
- `backend/src/main/java/co/edu/unicauca/backend/modules/notificaciones/service/NotificacionService.java` — añadir `notificarCambio`.
- `backend/src/main/java/co/edu/unicauca/backend/modules/notificaciones/controller/NotificacionController.java` — endpoint `cambio`.
- `backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/mapper/VisitaEstadoMapper.java` — fix mapping.
- `backend/ENDPOINTS.md`, `backend/CLAUDE.md`, `backend/docs/coding-patterns.md`.

---

## Task 0: Verificación de baseline

**Files:** ninguno (solo lectura).

- [ ] **Step 1: Verificar tests baseline**

Run: `cd backend && ./mvnw test -q`
Expected: BUILD SUCCESS. Anotar número de tests para comparar al final.

- [ ] **Step 2: Verificar que la rama es `PA-102-cambiar-estado-comanda`**

Run: `git rev-parse --abbrev-ref HEAD`
Expected: `PA-102-cambiar-estado-comanda`.

---

## Task 1: Repositorios de inventario (esqueleto)

**Files:**
- Create: `backend/src/main/java/co/edu/unicauca/backend/modules/inventario/repository/RecetaRepository.java`
- Create: `backend/src/main/java/co/edu/unicauca/backend/modules/inventario/repository/InsumoRepository.java`
- Create: `backend/src/main/java/co/edu/unicauca/backend/modules/inventario/repository/MovimientoInventarioRepository.java`

> Si alguno ya existe, omitir su creación; verificar que expone los métodos requeridos.

- [ ] **Step 1: Comprobar si existen**

Run: `ls backend/src/main/java/co/edu/unicauca/backend/modules/inventario/repository/ 2>nul`
Crear los que falten.

- [ ] **Step 2: Crear `RecetaRepository`**

```java
package co.edu.unicauca.backend.modules.inventario.repository;

import co.edu.unicauca.backend.modules.inventario.entity.Receta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Acceso a datos de {@link Receta}. Proporciona la lista de insumos y
 * cantidades necesarias para preparar un producto.
 */
@Repository
public interface RecetaRepository extends JpaRepository<Receta, Receta.RecetaId> {

    /**
     * Devuelve todas las recetas asociadas a un producto, con el insumo cargado
     * para evitar consultas adicionales al ejecutar el descuento.
     *
     * @param productoId identificador del producto
     * @return lista de recetas; vacía si el producto no tiene receta registrada
     */
    @org.springframework.data.jpa.repository.Query(
        "SELECT r FROM Receta r JOIN FETCH r.insumo WHERE r.productoId = :productoId")
    List<Receta> findByProductoIdFetchInsumo(@org.springframework.data.repository.query.Param("productoId") Long productoId);
}
```

- [ ] **Step 3: Crear `InsumoRepository` (si falta)**

```java
package co.edu.unicauca.backend.modules.inventario.repository;

import co.edu.unicauca.backend.modules.inventario.entity.Insumo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InsumoRepository extends JpaRepository<Insumo, Long> {
}
```

- [ ] **Step 4: Crear `MovimientoInventarioRepository` (si falta)**

```java
package co.edu.unicauca.backend.modules.inventario.repository;

import co.edu.unicauca.backend.modules.inventario.entity.MovimientoInventario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MovimientoInventarioRepository extends JpaRepository<MovimientoInventario, Long> {
}
```

- [ ] **Step 5: Compilar**

Run: `cd backend && ./mvnw -q compile`
Expected: BUILD SUCCESS.

- [ ] **Step 6: Reportar add+mensaje**

```
git add backend/src/main/java/co/edu/unicauca/backend/modules/inventario/repository/
```
Mensaje propuesto:
```
chore(inventario): agregar repositorios para descuento de comandas
```

---

## Task 2: DTOs nuevos (request/response notificación cambio)

**Files:**
- Create: `backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/dto/request/NotificarCambioRequest.java`
- Create: `backend/src/main/java/co/edu/unicauca/backend/modules/notificaciones/dto/response/NotificarCambioResponse.java`

- [ ] **Step 1: `NotificarCambioRequest`**

```java
package co.edu.unicauca.backend.modules.mesas_comandas.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Cuerpo de la solicitud para que el personal de producción registre una
 * notificación de cambio sobre una comanda pendiente.
 */
@Getter
@Setter
@NoArgsConstructor
public class NotificarCambioRequest {

    /** Identificador de la comanda en estado {@code PENDIENTE} sobre la que se registra el cambio. */
    @NotNull(message = "El identificador de la comanda es obligatorio")
    private Long comandaId;
}
```

- [ ] **Step 2: `NotificarCambioResponse`**

```java
package co.edu.unicauca.backend.modules.notificaciones.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * Respuesta devuelta tras crear una notificación de cambio. Expone el
 * identificador para que el frontend pueda referenciar la notificación en
 * acciones posteriores y su estado inicial.
 */
@Getter
@Builder
public class NotificarCambioResponse {

    /** Identificador de la notificación recién creada. */
    private final Long notificacionId;

    /** Estado de la notificación: {@code "ACTIVA"} al momento de la creación. */
    private final String estado;
}
```

- [ ] **Step 3: Compilar**

Run: `cd backend && ./mvnw -q compile`
Expected: BUILD SUCCESS.

- [ ] **Step 4: Reportar add+mensaje**

```
git add backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/dto/request/NotificarCambioRequest.java
git add backend/src/main/java/co/edu/unicauca/backend/modules/notificaciones/dto/response/NotificarCambioResponse.java
```
Mensaje:
```
feat(notificaciones): DTOs de notificación de cambio desde producción
```

---

## Task 3: Extender enum y record WS (`ACTUALIZADA` + `nuevoEstado`)

**Files:**
- Modify: `backend/src/main/java/co/edu/unicauca/backend/modules/notificaciones/dto/ws/TipoEventoProduccion.java`
- Modify: `backend/src/main/java/co/edu/unicauca/backend/modules/notificaciones/dto/ws/ComandaProduccionEventoWsMessage.java`
- Modify (call-sites del record): `NotificacionService` (3 sitios), `ComandaBorradorService` (1 sitio).

- [ ] **Step 1: Agregar `ACTUALIZADA` al enum**

Reemplazar archivo:
```java
package co.edu.unicauca.backend.modules.notificaciones.dto.ws;

/**
 * Tipo de evento WebSocket asociado al ciclo de vida de una comanda en una
 * estación de producción.
 *
 * <ul>
 *   <li>{@code CREADA} — La comanda transicionó de {@code BORRADOR} a
 *       {@code PENDIENTE} y debe aparecer en la columna de pendientes.</li>
 *   <li>{@code ACTUALIZADA} — La comanda cambió de estado dentro del tablero
 *       (por ejemplo {@code PENDIENTE→EN_PREPARACION} o
 *       {@code EN_PREPARACION→LISTO}); el campo {@code nuevoEstado} del
 *       payload indica la columna destino.</li>
 *   <li>{@code ELIMINADA} — La comanda dejó de estar visible en el tablero.</li>
 *   <li>{@code COMPLETADA} — El mesero registró el servicio de la comanda.</li>
 * </ul>
 */
public enum TipoEventoProduccion {
    CREADA,
    ACTUALIZADA,
    ELIMINADA,
    COMPLETADA
}
```

- [ ] **Step 2: Extender record con `nuevoEstado`**

Reemplazar archivo:
```java
package co.edu.unicauca.backend.modules.notificaciones.dto.ws;

import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.ComandaProduccionResumenResponse;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Mensaje WebSocket emitido a los tópicos de producción
 * ({@code /topic/produccion/cocina} y {@code /topic/produccion/barra}).
 *
 * @param tipo        tipo de evento que describe el cambio en la comanda
 * @param estacion    estación productora; debe coincidir con el tópico
 * @param comandaId   identificador de la comanda afectada
 * @param resumen     resumen completo. Presente cuando {@code tipo == CREADA}.
 *                    {@code null} en {@code ACTUALIZADA}, {@code ELIMINADA} y
 *                    {@code COMPLETADA}.
 * @param nuevoEstado nombre del estado destino. Presente cuando
 *                    {@code tipo == ACTUALIZADA}; {@code null} en los demás.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ComandaProduccionEventoWsMessage(
        TipoEventoProduccion tipo,
        String estacion,
        Long comandaId,
        ComandaProduccionResumenResponse resumen,
        String nuevoEstado) {
}
```

- [ ] **Step 3: Localizar call-sites del record**

Run: `grep -rn "new ComandaProduccionEventoWsMessage" backend/src/main`
Expected: ~4 ocurrencias (NotificacionService.servirPlatos, servirBebidas, atenderCambio + cualquier otro).

- [ ] **Step 4: Actualizar call-sites — agregar `null` final**

Para cada ocurrencia, transformar:
```java
new ComandaProduccionEventoWsMessage(
        TipoEventoProduccion.COMPLETADA,
        comanda.getComandaEstacion().name(),
        comanda.getComandaId(),
        null)
```
en:
```java
new ComandaProduccionEventoWsMessage(
        TipoEventoProduccion.COMPLETADA,
        comanda.getComandaEstacion().name(),
        comanda.getComandaId(),
        null,
        null)
```
(Análogo para `ELIMINADA`.)

Repetir para `ComandaBorradorService` si publica `CREADA`. En `CREADA`, mantener `resumen` no nulo y `nuevoEstado=null`.

- [ ] **Step 5: Compilar**

Run: `cd backend && ./mvnw -q compile`
Expected: BUILD SUCCESS. Si falla, completar call-sites faltantes.

- [ ] **Step 6: Ejecutar tests existentes para verificar que el record extendido no rompió nada**

Run: `cd backend && ./mvnw -q test -Dtest=NotificacionWsPublisherTest,NotificacionServiceTest,ComandaBorradorServiceTest`
Expected: PASS (puede requerir ajustar ensambles de tests si construyen el record literalmente).

- [ ] **Step 7: Reportar add+mensaje**

```
git add backend/src/main/java/co/edu/unicauca/backend/modules/notificaciones/dto/ws/
git add backend/src/main/java/co/edu/unicauca/backend/modules/notificaciones/service/NotificacionService.java
git add backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/service/ComandaBorradorService.java
```
Mensaje:
```
feat(notificaciones): agregar evento ACTUALIZADA y campo nuevoEstado al WS de producción
```

---

## Task 4: Nueva query `sumCantidadInsumoComprometida` + test `@DataJpaTest`

**Files:**
- Modify: `backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/repository/ComandaItemRepository.java`
- Test: `backend/src/test/java/co/edu/unicauca/backend/modules/mesas_comandas/repository/ComandaItemRepositoryTest.java` (extender si existe; crear si no).

- [ ] **Step 1: Escribir test fallido** (extender la clase test existente; nuevo `@Nested`)

```java
@Nested
@DisplayName("sumCantidadInsumoComprometida")
class SumCantidadInsumoComprometida {

    @Test
    @DisplayName("sin comandas activas → 0")
    void sinComandas_retornaCero() {
        BigDecimal total = comandaItemRepository.sumCantidadInsumoComprometida(insumoSeed.getInsumoId());
        assertThat(total).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("ítem PREPARACION en BORRADOR → suma recetaCantidad × cantidad")
    void itemEnBorrador_sumaCantidad() {
        // crear comanda BORRADOR con item de producto PREPARACION cuya receta usa insumoSeed con cantidad 0.250
        // crear ComandaItem con cantidad 4 → comprometido esperado = 1.000
        BigDecimal total = comandaItemRepository.sumCantidadInsumoComprometida(insumoSeed.getInsumoId());
        assertThat(total).isEqualByComparingTo("1.000");
    }

    @Test
    @DisplayName("ítem en COMPLETADO se excluye")
    void itemCompletado_seExcluye() { /* análogo, no debe sumar */ }

    @Test
    @DisplayName("ítem con comandaItemMenuGrupo se excluye")
    void itemMenuEspecial_seExcluye() { /* análogo */ }

    @Test
    @DisplayName("producto VENTA_DIRECTA se excluye")
    void productoVentaDirecta_seExcluye() { /* análogo */ }
}
```

- [ ] **Step 2: Verificar que el test falla por método inexistente**

Run: `cd backend && ./mvnw -q test -Dtest=ComandaItemRepositoryTest$SumCantidadInsumoComprometida`
Expected: COMPILATION FAIL (`sumCantidadInsumoComprometida` no existe).

- [ ] **Step 3: Implementar la query**

En `ComandaItemRepository.java`, agregar dentro de la interfaz:

```java
/**
 * Suma la cantidad de un insumo comprometida por todas las comandas en
 * estado {@code BORRADOR} o {@code PENDIENTE} que utilicen ese insumo a
 * través de un producto de tipo {@code PREPARACION}.
 *
 * <p>Excluye los ítems con {@code comandaItemMenuGrupo IS NOT NULL} (los
 * menús especiales no decrementan inventario por contrato del módulo) y los
 * ítems con productos {@code VENTA_DIRECTA} (que se controlan por
 * {@code Producto.stockActual}).
 *
 * @param insumoId identificador del insumo
 * @return suma de {@code recetaCantidad × comandaItemCantidad}; {@code 0}
 *         si no hay comandas que comprometan el insumo
 */
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
java.math.BigDecimal sumCantidadInsumoComprometida(@Param("insumoId") Long insumoId);
```

- [ ] **Step 4: Verificar tests verdes**

Run: `cd backend && ./mvnw -q test -Dtest=ComandaItemRepositoryTest`
Expected: PASS.

- [ ] **Step 5: Reportar add+mensaje**

```
git add backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/repository/ComandaItemRepository.java
git add backend/src/test/java/co/edu/unicauca/backend/modules/mesas_comandas/repository/ComandaItemRepositoryTest.java
```
Mensaje:
```
feat(mesas_comandas): query sumCantidadInsumoComprometida para validador dual
```

---

## Task 5: `InventarioDescuentoService` (TDD)

**Files:**
- Test: `backend/src/test/java/co/edu/unicauca/backend/modules/inventario/service/InventarioDescuentoServiceTest.java`
- Create: `backend/src/main/java/co/edu/unicauca/backend/modules/inventario/service/InventarioDescuentoService.java`

- [ ] **Step 1: Escribir el test (todas las ramas en un solo archivo)**

```java
package co.edu.unicauca.backend.modules.inventario.service;

import co.edu.unicauca.backend.modules.inventario.entity.*;
import co.edu.unicauca.backend.modules.inventario.repository.*;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Comanda;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.ComandaItem;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ComandaItemRepository;
import co.edu.unicauca.backend.modules.usuarios.entity.Empleado;
import co.edu.unicauca.backend.shared.enums.*;
import co.edu.unicauca.backend.shared.exception.BusinessException;
import org.junit.jupiter.api.*;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("InventarioDescuentoService")
class InventarioDescuentoServiceTest {

    @Mock ComandaItemRepository comandaItemRepository;
    @Mock RecetaRepository recetaRepository;
    @Mock InsumoRepository insumoRepository;
    @Mock co.edu.unicauca.backend.modules.inventario.repository.ProductoRepository productoRepository;
    @Mock MovimientoInventarioRepository movimientoRepository;
    @InjectMocks InventarioDescuentoService service;

    private Empleado actor() { return Empleado.builder().build(); }
    private Comanda comanda() { return Comanda.builder().comandaId(1L).build(); }

    private Producto productoVentaDirecta(Long id, BigDecimal stock) {
        return Producto.builder().productoId(id).productoTipo(TipoProducto.VENTA_DIRECTA).stockActual(stock).build();
    }
    private Producto productoPreparacion(Long id) {
        return Producto.builder().productoId(id).productoTipo(TipoProducto.PREPARACION).build();
    }
    private ComandaItem item(Producto p, int cantidad, String menuGrupo) {
        return ComandaItem.builder().comanda(comanda()).producto(p).comandaItemCantidad(cantidad).comandaItemMenuGrupo(menuGrupo).build();
    }
    private Insumo insumo(Long id, String stock) {
        return Insumo.builder().insumoId(id).insumoStockActual(new BigDecimal(stock)).build();
    }
    private Receta receta(Long productoId, Insumo insumo, String cantidad) {
        return Receta.builder().productoId(productoId).insumoId(insumo.getInsumoId()).insumo(insumo).recetaCantidad(new BigDecimal(cantidad)).build();
    }

    @Nested @DisplayName("descontarPorComanda")
    class DescontarPorComanda {

        @Test @DisplayName("VENTA_DIRECTA con stock suficiente → resta y persiste movimiento")
        void ventaDirectaSuficiente_descuentaYRegistraMovimiento() {
            Producto p = productoVentaDirecta(10L, new BigDecimal("5"));
            when(comandaItemRepository.findByComanda_ComandaIdOrderByProductoNombreAsc(1L))
                    .thenReturn(List.of(item(p, 2, null)));

            service.descontarPorComanda(comanda(), actor());

            assertThat(p.getStockActual()).isEqualByComparingTo("3");
            verify(productoRepository).save(p);
            verify(movimientoRepository).save(argThat(m ->
                    m.getProducto() == p && m.getInsumo() == null
                            && m.getMovimientoTipo() == TipoMovimiento.EGRESO
                            && m.getMovimientoCantidad().compareTo(new BigDecimal("2")) == 0));
        }

        @Test @DisplayName("VENTA_DIRECTA con stockActual null → skip silencioso")
        void ventaDirectaSinStock_skip() {
            Producto p = productoVentaDirecta(10L, null);
            when(comandaItemRepository.findByComanda_ComandaIdOrderByProductoNombreAsc(1L))
                    .thenReturn(List.of(item(p, 2, null)));

            service.descontarPorComanda(comanda(), actor());

            verifyNoInteractions(productoRepository, movimientoRepository);
        }

        @Test @DisplayName("VENTA_DIRECTA con stock insuficiente → INSUFFICIENT_STOCK")
        void ventaDirectaInsuficiente_lanza() {
            Producto p = productoVentaDirecta(10L, new BigDecimal("1"));
            when(comandaItemRepository.findByComanda_ComandaIdOrderByProductoNombreAsc(1L))
                    .thenReturn(List.of(item(p, 5, null)));

            assertThatThrownBy(() -> service.descontarPorComanda(comanda(), actor()))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Stock insuficiente");
        }

        @Test @DisplayName("PREPARACION receta vacía → no descuenta")
        void preparacionRecetaVacia_noDescuenta() {
            Producto p = productoPreparacion(20L);
            when(comandaItemRepository.findByComanda_ComandaIdOrderByProductoNombreAsc(1L))
                    .thenReturn(List.of(item(p, 1, null)));
            when(recetaRepository.findByProductoIdFetchInsumo(20L)).thenReturn(List.of());

            service.descontarPorComanda(comanda(), actor());

            verifyNoInteractions(insumoRepository, movimientoRepository);
        }

        @Test @DisplayName("PREPARACION 2 insumos suficientes → descuenta cada insumo y registra movimientos")
        void preparacionVariosInsumos() {
            Producto p = productoPreparacion(20L);
            Insumo i1 = insumo(100L, "10"); Insumo i2 = insumo(101L, "5");
            when(comandaItemRepository.findByComanda_ComandaIdOrderByProductoNombreAsc(1L))
                    .thenReturn(List.of(item(p, 2, null)));
            when(recetaRepository.findByProductoIdFetchInsumo(20L))
                    .thenReturn(List.of(receta(20L, i1, "0.5"), receta(20L, i2, "1.0")));

            service.descontarPorComanda(comanda(), actor());

            assertThat(i1.getInsumoStockActual()).isEqualByComparingTo("9.0");
            assertThat(i2.getInsumoStockActual()).isEqualByComparingTo("3.0");
            verify(insumoRepository).save(i1);
            verify(insumoRepository).save(i2);
            verify(movimientoRepository, times(2)).save(any(MovimientoInventario.class));
        }

        @Test @DisplayName("PREPARACION insumo limitante insuficiente → INSUFFICIENT_STOCK con nombre del insumo")
        void preparacionInsumoInsuficiente_lanzaConDetalle() {
            Producto p = productoPreparacion(20L);
            Insumo i = insumo(100L, "0.4");
            i.setInsumoNombre("Limón");
            when(comandaItemRepository.findByComanda_ComandaIdOrderByProductoNombreAsc(1L))
                    .thenReturn(List.of(item(p, 2, null)));
            when(recetaRepository.findByProductoIdFetchInsumo(20L))
                    .thenReturn(List.of(receta(20L, i, "0.5")));

            assertThatThrownBy(() -> service.descontarPorComanda(comanda(), actor()))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Limón");
        }

        @Test @DisplayName("Item con comandaItemMenuGrupo → ignorado")
        void menuEspecial_ignorado() {
            Producto p = productoVentaDirecta(10L, new BigDecimal("5"));
            when(comandaItemRepository.findByComanda_ComandaIdOrderByProductoNombreAsc(1L))
                    .thenReturn(List.of(item(p, 2, "grupo-uuid")));

            service.descontarPorComanda(comanda(), actor());

            assertThat(p.getStockActual()).isEqualByComparingTo("5");
            verifyNoInteractions(productoRepository, movimientoRepository);
        }
    }
}
```

- [ ] **Step 2: Verificar test falla**

Run: `cd backend && ./mvnw -q test -Dtest=InventarioDescuentoServiceTest`
Expected: COMPILATION FAIL.

- [ ] **Step 3: Implementar `InventarioDescuentoService`**

```java
package co.edu.unicauca.backend.modules.inventario.service;

import co.edu.unicauca.backend.modules.inventario.entity.Insumo;
import co.edu.unicauca.backend.modules.inventario.entity.MovimientoInventario;
import co.edu.unicauca.backend.modules.inventario.entity.Producto;
import co.edu.unicauca.backend.modules.inventario.entity.Receta;
import co.edu.unicauca.backend.modules.inventario.repository.InsumoRepository;
import co.edu.unicauca.backend.modules.inventario.repository.MovimientoInventarioRepository;
import co.edu.unicauca.backend.modules.inventario.repository.ProductoRepository;
import co.edu.unicauca.backend.modules.inventario.repository.RecetaRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Comanda;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.ComandaItem;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ComandaItemRepository;
import co.edu.unicauca.backend.modules.usuarios.entity.Empleado;
import co.edu.unicauca.backend.shared.enums.TipoMovimiento;
import co.edu.unicauca.backend.shared.enums.TipoProducto;
import co.edu.unicauca.backend.shared.exception.BusinessException;
import co.edu.unicauca.backend.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Aplica el descuento de inventario asociado a iniciar la preparación de una
 * comanda. Para productos de venta directa decrementa el stock del producto;
 * para productos de preparación recorre la receta y decrementa el stock de
 * cada insumo. En ambos casos persiste un {@link MovimientoInventario} de
 * tipo {@code EGRESO} para conservar la trazabilidad.
 *
 * <p>Los ítems pertenecientes a un menú especial (con
 * {@code comandaItemMenuGrupo} no nulo) se ignoran por contrato del módulo.
 */
@Service
@RequiredArgsConstructor
public class InventarioDescuentoService {

    private final ComandaItemRepository comandaItemRepository;
    private final RecetaRepository recetaRepository;
    private final ProductoRepository productoRepository;
    private final InsumoRepository insumoRepository;
    private final MovimientoInventarioRepository movimientoRepository;

    /**
     * Descuenta el inventario consumido por todos los ítems de la comanda y
     * registra los movimientos correspondientes. Cualquier insuficiencia
     * provoca {@link BusinessException} para que la transacción del llamador
     * revierta los cambios.
     *
     * @param comanda comanda cuyos ítems se procesan
     * @param actor   empleado responsable del descuento (queda registrado en
     *                cada {@link MovimientoInventario})
     * @throws BusinessException con {@link ErrorCode#INSUFFICIENT_STOCK} si
     *                           algún producto o insumo no tiene stock
     *                           suficiente
     */
    @Transactional
    public void descontarPorComanda(Comanda comanda, Empleado actor) {
        // Carga los ítems en un orden determinista para que los movimientos queden trazables
        List<ComandaItem> items = comandaItemRepository
                .findByComanda_ComandaIdOrderByProductoNombreAsc(comanda.getComandaId());

        for (ComandaItem item : items) {
            // Los ítems de menú especial nunca decrementan inventario
            if (item.getComandaItemMenuGrupo() != null) {
                continue;
            }
            Producto producto = item.getProducto();
            int cantidad = item.getComandaItemCantidad();

            if (producto.getProductoTipo() == TipoProducto.VENTA_DIRECTA) {
                descontarVentaDirecta(producto, cantidad, actor);
            } else if (producto.getProductoTipo() == TipoProducto.PREPARACION) {
                descontarPreparacion(producto, cantidad, actor);
            }
        }
    }

    private void descontarVentaDirecta(Producto producto, int cantidad, Empleado actor) {
        // Cuando el catálogo no gestiona stock para el producto, el descuento se omite silenciosamente
        if (producto.getStockActual() == null) {
            return;
        }
        BigDecimal pedido = BigDecimal.valueOf(cantidad);
        BigDecimal nuevo = producto.getStockActual().subtract(pedido);
        if (nuevo.signum() < 0) {
            throw new BusinessException(
                    ErrorCode.INSUFFICIENT_STOCK,
                    "Stock insuficiente del producto '" + producto.getProductoNombre() + "'.",
                    HttpStatus.CONFLICT);
        }
        producto.setStockActual(nuevo);
        productoRepository.save(producto);
        movimientoRepository.save(MovimientoInventario.builder()
                .producto(producto)
                .movimientoCantidad(pedido)
                .movimientoTipo(TipoMovimiento.EGRESO)
                .empleado(actor)
                .build());
    }

    private void descontarPreparacion(Producto producto, int cantidad, Empleado actor) {
        // El producto puede no tener receta registrada todavía; en ese caso no hay nada que descontar
        List<Receta> recetas = recetaRepository.findByProductoIdFetchInsumo(producto.getProductoId());
        BigDecimal cantidadDecimal = BigDecimal.valueOf(cantidad);

        for (Receta receta : recetas) {
            Insumo insumo = receta.getInsumo();
            BigDecimal requerido = receta.getRecetaCantidad().multiply(cantidadDecimal);
            BigDecimal nuevo = insumo.getInsumoStockActual().subtract(requerido);
            if (nuevo.signum() < 0) {
                throw new BusinessException(
                        ErrorCode.INSUFFICIENT_STOCK,
                        "Stock insuficiente del insumo '" + insumo.getInsumoNombre()
                                + "' para preparar '" + producto.getProductoNombre() + "'.",
                        HttpStatus.CONFLICT);
            }
            insumo.setInsumoStockActual(nuevo);
            insumoRepository.save(insumo);
            movimientoRepository.save(MovimientoInventario.builder()
                    .insumo(insumo)
                    .movimientoCantidad(requerido)
                    .movimientoTipo(TipoMovimiento.EGRESO)
                    .empleado(actor)
                    .build());
        }
    }
}
```

- [ ] **Step 4: Verificar tests verdes**

Run: `cd backend && ./mvnw -q test -Dtest=InventarioDescuentoServiceTest`
Expected: PASS (todos los `@Test` del nested).

- [ ] **Step 5: Reportar add+mensaje**

```
git add backend/src/main/java/co/edu/unicauca/backend/modules/inventario/service/InventarioDescuentoService.java
git add backend/src/test/java/co/edu/unicauca/backend/modules/inventario/service/InventarioDescuentoServiceTest.java
```
Mensaje:
```
feat(inventario): servicio de descuento dual con auditoría de movimientos
```

---

## Task 6: Refactor `ComandaBorradorValidador.validarStock` con rama PREPARACION

**Files:**
- Modify: `backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/service/ComandaBorradorValidador.java`
- Test: `backend/src/test/java/co/edu/unicauca/backend/modules/mesas_comandas/service/ComandaBorradorValidadorTest.java`

- [ ] **Step 1: Agregar tests para PREPARACION**

Añadir nested class al test existente:
```java
@Nested @DisplayName("validarStock — PREPARACION")
class ValidarStockPreparacion {

    @Test @DisplayName("insumo suficiente → no lanza")
    void insumoSuficiente_noLanza() {
        Insumo i = Insumo.builder().insumoId(100L).insumoStockActual(new BigDecimal("10")).build();
        Receta r = Receta.builder().productoId(20L).insumoId(100L).insumo(i).recetaCantidad(new BigDecimal("0.5")).build();
        Producto p = Producto.builder().productoId(20L).productoTipo(TipoProducto.PREPARACION).build();
        when(recetaRepository.findByProductoIdFetchInsumo(20L)).thenReturn(List.of(r));
        when(comandaItemRepository.sumCantidadInsumoComprometida(100L)).thenReturn(new BigDecimal("0"));

        assertThatCode(() -> validador.validarStock(p, 4, 0)).doesNotThrowAnyException();
    }

    @Test @DisplayName("insumo insuficiente → INSUFFICIENT_STOCK con nombre")
    void insumoInsuficiente_lanza() { /* análogo, comprometido o stock bajo */ }

    @Test @DisplayName("cantidadAnterior se descuenta del comprometido")
    void cantidadAnteriorAjusta() { /* item ya contabilizado: comprometido=2 con receta 0.5 → ajuste 1.0 */ }

    @Test @DisplayName("producto sin receta → no valida (no lanza)")
    void sinReceta_noLanza() { /* recetaRepository devuelve vacío */ }
}
```

- [ ] **Step 2: Verificar fallo**

Run: `cd backend && ./mvnw -q test -Dtest=ComandaBorradorValidadorTest$ValidarStockPreparacion`
Expected: FAIL (no compila o no contempla rama PREPARACION).

- [ ] **Step 3: Refactorizar validador**

Reemplazar contenido del método `validarStock` y añadir dependencia:

```java
private final ComandaItemRepository comandaItemRepository;
private final RecetaRepository recetaRepository;

public void validarStock(Producto producto, int nuevaCantidad, int cantidadAnterior) {
    if (producto.getProductoTipo() == TipoProducto.PREPARACION) {
        validarStockPreparacion(producto, nuevaCantidad, cantidadAnterior);
        return;
    }
    validarStockVentaDirecta(producto, nuevaCantidad, cantidadAnterior);
}

private void validarStockVentaDirecta(Producto producto, int nuevaCantidad, int cantidadAnterior) {
    if (producto.getStockActual() == null) {
        return;
    }
    long comprometido = comandaItemRepository.sumCantidadComprometidaByProducto(producto.getProductoId());
    long disponible = producto.getStockActual().longValue() - (comprometido - cantidadAnterior);
    if (nuevaCantidad > disponible) {
        throw new BusinessException(
                ErrorCode.INSUFFICIENT_STOCK,
                "Solo hay " + Math.max(disponible, 0) + " unidades disponibles de este producto",
                HttpStatus.CONFLICT);
    }
}

private void validarStockPreparacion(Producto producto, int nuevaCantidad, int cantidadAnterior) {
    var recetas = recetaRepository.findByProductoIdFetchInsumo(producto.getProductoId());
    if (recetas.isEmpty()) {
        return;
    }
    java.math.BigDecimal nueva = java.math.BigDecimal.valueOf(nuevaCantidad);
    java.math.BigDecimal anterior = java.math.BigDecimal.valueOf(cantidadAnterior);

    for (var r : recetas) {
        var insumo = r.getInsumo();
        java.math.BigDecimal requerido = r.getRecetaCantidad().multiply(nueva);
        java.math.BigDecimal yaContabilizado = r.getRecetaCantidad().multiply(anterior);
        java.math.BigDecimal comprometido = comandaItemRepository
                .sumCantidadInsumoComprometida(insumo.getInsumoId())
                .subtract(yaContabilizado);
        java.math.BigDecimal disponible = insumo.getInsumoStockActual().subtract(comprometido);
        if (requerido.compareTo(disponible) > 0) {
            throw new BusinessException(
                    ErrorCode.INSUFFICIENT_STOCK,
                    "Stock insuficiente del insumo '" + insumo.getInsumoNombre()
                            + "' para preparar '" + producto.getProductoNombre() + "'.",
                    HttpStatus.CONFLICT);
        }
    }
}
```

- [ ] **Step 4: Verificar todos los tests del validador (incluyendo VENTA_DIRECTA existentes)**

Run: `cd backend && ./mvnw -q test -Dtest=ComandaBorradorValidadorTest`
Expected: PASS.

- [ ] **Step 5: Verificar suite completa de comandas (no romper nada)**

Run: `cd backend && ./mvnw -q test -Dtest=Comanda*`
Expected: PASS.

- [ ] **Step 6: Reportar add+mensaje**

```
git add backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/service/ComandaBorradorValidador.java
git add backend/src/test/java/co/edu/unicauca/backend/modules/mesas_comandas/service/ComandaBorradorValidadorTest.java
```
Mensaje:
```
refactor(mesas_comandas): validar stock dual por TipoProducto en borrador
```

---

## Task 7: `ComandaProduccionService.iniciarPreparacion` (TDD)

**Files:**
- Modify: `backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/service/ComandaProduccionService.java`
- Test: `backend/src/test/java/co/edu/unicauca/backend/modules/mesas_comandas/service/ComandaProduccionServiceTest.java`

- [ ] **Step 1: Tests del nuevo método**

Añadir al test existente:
```java
@Nested @DisplayName("iniciarPreparacion")
class IniciarPreparacion {

    @Test @DisplayName("estado PENDIENTE + estación correcta + descuento OK → EN_PREPARACION + WS ACTUALIZADA + WS cliente")
    void happyPath() { /* mock comanda PENDIENTE, EstacionResolver retorna estacion match, InventarioDescuentoService no lanza */ }

    @Test @DisplayName("comanda inexistente → ResourceNotFoundException")
    void inexistente_404() { }

    @Test @DisplayName("estado != PENDIENTE → INVALID_STATE")
    void estadoIncorrecto_409() { }

    @Test @DisplayName("estación no resuelta → ACCESS_DENIED")
    void estacionAjena_403() { }

    @Test @DisplayName("descuento lanza INSUFFICIENT_STOCK → comanda intacta y excepción propaga")
    void descuentoFalla_noTransiciona() { /* verify(comandaRepository, never()).save y verify(wsPublisher, never()).publicarEventoProduccion */ }
}
```

- [ ] **Step 2: Implementar método en `ComandaProduccionService`**

Agregar dependencias `InventarioDescuentoService inventarioDescuentoService;`, `NotificacionWsPublisher wsPublisher;`, `EmpleadoRepository empleadoRepository;`. Y método:

```java
/**
 * Transiciona una comanda en estado {@code PENDIENTE} a {@code EN_PREPARACION}.
 * Antes del cambio de estado descuenta el inventario consumido por todos los
 * ítems no pertenecientes a un menú especial; si el descuento falla la
 * transacción revierte y la comanda permanece en {@code PENDIENTE}.
 *
 * @param comandaId identificador de la comanda
 * @param auth      contexto de autenticación; debe pertenecer a la estación
 *                  de la comanda
 * @return resumen de la comanda actualizada
 */
@Transactional
public ComandaProduccionResumenResponse iniciarPreparacion(Long comandaId, Authentication auth) {
    java.util.Set<co.edu.unicauca.backend.shared.enums.EstacionComanda> estaciones =
            estacionResolver.resolverEstaciones(auth);

    Comanda comanda = comandaRepository.findById(comandaId)
            .orElseThrow(() -> new co.edu.unicauca.backend.shared.exception.ResourceNotFoundException("Comanda", comandaId));

    if (comanda.getComandaEstado() != EstadoComanda.PENDIENTE) {
        throw new BusinessException(
                ErrorCode.INVALID_STATE,
                "Solo se pueden iniciar comandas en estado PENDIENTE.",
                HttpStatus.CONFLICT);
    }
    if (!estaciones.contains(comanda.getComandaEstacion())) {
        throw new BusinessException(
                ErrorCode.ACCESS_DENIED,
                "La comanda no pertenece a una estación accesible para el usuario.",
                HttpStatus.FORBIDDEN);
    }

    co.edu.unicauca.backend.modules.usuarios.entity.Empleado actor =
            empleadoRepository.findByUsuario_UsuarioEmail(auth.getName())
                    .orElseThrow(() -> new co.edu.unicauca.backend.shared.exception.ResourceNotFoundException("Empleado", auth.getName()));

    // Descuento primero; si falla, la transacción revierte y la comanda permanece en PENDIENTE
    inventarioDescuentoService.descontarPorComanda(comanda, actor);

    comanda.setComandaEstado(EstadoComanda.EN_PREPARACION);
    comanda.setComandaFechaHoraInicio(java.time.LocalDateTime.now());
    comandaRepository.save(comanda);

    Mesa mesa = mesaRepository.findByVisita_VisitaId(comanda.getVisita().getVisitaId()).orElse(null);
    int total = comandaItemRepository.sumCantidadByComandaIdIn(java.util.Set.of(comandaId)).stream()
            .findFirst().map(r -> ((Number) r[1]).intValue()).orElse(0);
    ComandaProduccionResumenResponse resumen = comandaProduccionMapper.toResumen(comanda, mesa, total);

    wsPublisher.publicarEventoProduccion(comanda.getComandaEstacion(),
            new co.edu.unicauca.backend.modules.notificaciones.dto.ws.ComandaProduccionEventoWsMessage(
                    co.edu.unicauca.backend.modules.notificaciones.dto.ws.TipoEventoProduccion.ACTUALIZADA,
                    comanda.getComandaEstacion().name(),
                    comandaId,
                    null,
                    EstadoComanda.EN_PREPARACION.name()));
    wsPublisher.publicarVisitaActualizada(comanda.getVisita().getVisitaId(),
            co.edu.unicauca.backend.modules.notificaciones.dto.ws.VisitaActualizadaWsMessage.builder()
                    .visitaId(comanda.getVisita().getVisitaId()).build());

    return resumen;
}
```

> Nota: si `EmpleadoRepository.findByUsuario_UsuarioEmail` no existe, usar el lookup que use `NotificacionService` para resolver el empleado actor (verificar grep `findByUsuario_UsuarioEmail` antes de implementar; ajustar firma).

- [ ] **Step 3: Verificar tests verdes**

Run: `cd backend && ./mvnw -q test -Dtest=ComandaProduccionServiceTest$IniciarPreparacion`
Expected: PASS.

- [ ] **Step 4: Reportar add+mensaje**

```
git add backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/service/ComandaProduccionService.java
git add backend/src/test/java/co/edu/unicauca/backend/modules/mesas_comandas/service/ComandaProduccionServiceTest.java
```
Mensaje:
```
feat(mesas_comandas): iniciar preparación con descuento de inventario
```

---

## Task 8: `ComandaProduccionService.marcarListo` (TDD)

**Files:** mismas que Task 7 + `NotificacionRepository`.

- [ ] **Step 1: Tests del nuevo método**

```java
@Nested @DisplayName("marcarListo")
class MarcarListo {
    @Test @DisplayName("EN_PREPARACION COCINA → LISTO + Notificacion PLATOS_LISTOS + WS ACTUALIZADA + visita + mesa")
    void happyCocina() { }
    @Test @DisplayName("EN_PREPARACION BARRA → LISTO + Notificacion BEBIDAS_LISTAS")
    void happyBarra() { }
    @Test @DisplayName("estado != EN_PREPARACION → INVALID_STATE")
    void estadoIncorrecto_409() { }
    @Test @DisplayName("estación ajena → ACCESS_DENIED")
    void estacionAjena_403() { }
    @Test @DisplayName("comanda inexistente → 404")
    void inexistente_404() { }
}
```

- [ ] **Step 2: Implementar `marcarListo`**

Inyectar `NotificacionRepository notificacionRepository;` y `MesaWsPublisher mesaWsPublisher;`.

```java
/**
 * Transiciona una comanda en estado {@code EN_PREPARACION} a {@code LISTO},
 * crea la notificación correspondiente al mesero según la estación
 * ({@code PLATOS_LISTOS} para cocina, {@code BEBIDAS_LISTAS} para barra) y
 * propaga el cambio por los tópicos de producción y de la visita.
 */
@Transactional
public ComandaProduccionResumenResponse marcarListo(Long comandaId, Authentication auth) {
    java.util.Set<co.edu.unicauca.backend.shared.enums.EstacionComanda> estaciones =
            estacionResolver.resolverEstaciones(auth);

    Comanda comanda = comandaRepository.findById(comandaId)
            .orElseThrow(() -> new co.edu.unicauca.backend.shared.exception.ResourceNotFoundException("Comanda", comandaId));

    if (comanda.getComandaEstado() != EstadoComanda.EN_PREPARACION) {
        throw new BusinessException(ErrorCode.INVALID_STATE,
                "Solo se pueden marcar listas comandas en EN_PREPARACION.", HttpStatus.CONFLICT);
    }
    if (!estaciones.contains(comanda.getComandaEstacion())) {
        throw new BusinessException(ErrorCode.ACCESS_DENIED,
                "La comanda no pertenece a una estación accesible para el usuario.", HttpStatus.FORBIDDEN);
    }

    comanda.setComandaEstado(EstadoComanda.LISTO);
    comanda.setComandaFechaHoraListo(java.time.LocalDateTime.now());
    comandaRepository.save(comanda);

    Mesa mesa = mesaRepository.findByVisita_VisitaId(comanda.getVisita().getVisitaId()).orElse(null);

    co.edu.unicauca.backend.shared.enums.TipoNotificacion tipo =
            comanda.getComandaEstacion() == co.edu.unicauca.backend.shared.enums.EstacionComanda.COCINA
                    ? co.edu.unicauca.backend.shared.enums.TipoNotificacion.PLATOS_LISTOS
                    : co.edu.unicauca.backend.shared.enums.TipoNotificacion.BEBIDAS_LISTAS;

    notificacionRepository.save(co.edu.unicauca.backend.modules.notificaciones.entity.Notificacion.builder()
            .comanda(comanda)
            .mesa(mesa)
            .empleado(mesa != null ? mesa.getMesero() : null)
            .notificacionTipo(tipo)
            .notificacionEstado(co.edu.unicauca.backend.shared.enums.EstadoNotificacion.ACTIVA)
            .build());

    int total = comandaItemRepository.sumCantidadByComandaIdIn(java.util.Set.of(comandaId)).stream()
            .findFirst().map(r -> ((Number) r[1]).intValue()).orElse(0);
    ComandaProduccionResumenResponse resumen = comandaProduccionMapper.toResumen(comanda, mesa, total);

    wsPublisher.publicarEventoProduccion(comanda.getComandaEstacion(),
            new co.edu.unicauca.backend.modules.notificaciones.dto.ws.ComandaProduccionEventoWsMessage(
                    co.edu.unicauca.backend.modules.notificaciones.dto.ws.TipoEventoProduccion.ACTUALIZADA,
                    comanda.getComandaEstacion().name(),
                    comandaId, null, EstadoComanda.LISTO.name()));
    wsPublisher.publicarVisitaActualizada(comanda.getVisita().getVisitaId(),
            co.edu.unicauca.backend.modules.notificaciones.dto.ws.VisitaActualizadaWsMessage.builder()
                    .visitaId(comanda.getVisita().getVisitaId()).build());
    mesaWsPublisher.publicarActualizacionMesa(comanda.getVisita().getVisitaId(),
            co.edu.unicauca.backend.modules.notificaciones.service.MesaWsPublisher.TipoEventoMesa.NOTIFICACION);

    return resumen;
}
```

- [ ] **Step 3: Verificar tests**

Run: `cd backend && ./mvnw -q test -Dtest=ComandaProduccionServiceTest$MarcarListo`
Expected: PASS.

- [ ] **Step 4: Reportar add+mensaje**

```
git add backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/service/ComandaProduccionService.java
git add backend/src/test/java/co/edu/unicauca/backend/modules/mesas_comandas/service/ComandaProduccionServiceTest.java
```
Mensaje:
```
feat(mesas_comandas): marcar comanda como lista con notificación al mesero
```

---

## Task 9: Endpoints `iniciar` y `listo` en `ComandaProduccionController`

**Files:**
- Modify: `backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/controller/ComandaProduccionController.java`
- Test: `backend/src/test/java/co/edu/unicauca/backend/modules/mesas_comandas/controller/ComandaProduccionControllerTest.java`

- [ ] **Step 1: Tests `@WebMvcTest`**

Añadir nesteds `IniciarPreparacion` y `MarcarListo` siguiendo el patrón existente del archivo, con casos: 200, 404, 409, 403 (rol inválido y estación incorrecta), 401 (sin usuario).

- [ ] **Step 2: Implementar endpoints**

Añadir al controller:
```java
/**
 * Transiciona la comanda indicada de {@code PENDIENTE} a
 * {@code EN_PREPARACION}, ejecutando el descuento de inventario asociado.
 */
@PostMapping("/{comandaId}/iniciar")
@PreAuthorize("hasRole('PRODUCCION')")
@Operation(summary = "Iniciar preparación de una comanda")
public ResponseEntity<ApiResponse<ComandaProduccionResumenResponse>> iniciar(
        @PathVariable Long comandaId, Authentication auth) {
    ComandaProduccionResumenResponse data = comandaProduccionService.iniciarPreparacion(comandaId, auth);
    return ResponseEntity.ok(ApiResponse.ok("Comanda iniciada exitosamente", data));
}

/**
 * Marca la comanda indicada como {@code LISTO} y crea la notificación al
 * mesero según la estación.
 */
@PostMapping("/{comandaId}/listo")
@PreAuthorize("hasRole('PRODUCCION')")
@Operation(summary = "Marcar comanda como lista")
public ResponseEntity<ApiResponse<ComandaProduccionResumenResponse>> marcarListo(
        @PathVariable Long comandaId, Authentication auth) {
    ComandaProduccionResumenResponse data = comandaProduccionService.marcarListo(comandaId, auth);
    return ResponseEntity.ok(ApiResponse.ok("Comanda marcada como lista", data));
}
```

- [ ] **Step 3: Verificar tests**

Run: `cd backend && ./mvnw -q test -Dtest=ComandaProduccionControllerTest`
Expected: PASS.

- [ ] **Step 4: Reportar add+mensaje**

```
git add backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/controller/ComandaProduccionController.java
git add backend/src/test/java/co/edu/unicauca/backend/modules/mesas_comandas/controller/ComandaProduccionControllerTest.java
```
Mensaje:
```
feat(mesas_comandas): endpoints iniciar y listo en tablero de producción
```

---

## Task 10: `NotificacionService.notificarCambio` + endpoint

**Files:**
- Modify: `backend/src/main/java/co/edu/unicauca/backend/modules/notificaciones/service/NotificacionService.java`
- Modify: `backend/src/main/java/co/edu/unicauca/backend/modules/notificaciones/controller/NotificacionController.java`
- Test: `NotificacionServiceTest`, `NotificacionControllerTest`.

- [ ] **Step 1: Tests del service**

```java
@Nested @DisplayName("notificarCambio")
class NotificarCambio {
    @Test @DisplayName("comanda PENDIENTE estación COCINA → crea Notificacion CAMBIO + mesa publish")
    void happy() { }
    @Test @DisplayName("comanda inexistente → ResourceNotFoundException")
    void inexistente_404() { }
    @Test @DisplayName("comanda no PENDIENTE → INVALID_STATE")
    void estado_409() { }
    @Test @DisplayName("estación ajena → ACCESS_DENIED")
    void estacion_403() { }
    @Test @DisplayName("notificación CAMBIO ACTIVA ya existe → INVALID_STATE")
    void duplicada_409() { }
}
```

- [ ] **Step 2: Implementar método (inyectar `EstacionResolver`)**

```java
/**
 * Crea una notificación {@code CAMBIO} sobre una comanda {@code PENDIENTE}
 * para que el mesero acuda a la mesa y acuerde con el cliente la
 * sustitución del producto que producción no puede preparar.
 *
 * @param comandaId identificador de la comanda
 * @param auth      contexto del usuario autenticado (rol PRODUCCION,
 *                  estación que coincida con la de la comanda)
 * @return identificador y estado inicial de la notificación creada
 */
@Transactional
public co.edu.unicauca.backend.modules.notificaciones.dto.response.NotificarCambioResponse notificarCambio(
        Long comandaId, Authentication auth) {

    java.util.Set<co.edu.unicauca.backend.shared.enums.EstacionComanda> estaciones =
            estacionResolver.resolverEstaciones(auth);

    Comanda comanda = comandaRepository.findById(comandaId)
            .orElseThrow(() -> new ResourceNotFoundException("Comanda", comandaId));

    if (comanda.getComandaEstado() != EstadoComanda.PENDIENTE) {
        throw new BusinessException(ErrorCode.INVALID_STATE,
                "Solo se puede notificar cambio sobre comandas PENDIENTE.", HttpStatus.CONFLICT);
    }
    if (!estaciones.contains(comanda.getComandaEstacion())) {
        throw new BusinessException(ErrorCode.ACCESS_DENIED,
                "La comanda no pertenece a una estación accesible para el usuario.", HttpStatus.FORBIDDEN);
    }

    boolean existeActiva = notificacionRepository
            .findFirstByComanda_ComandaIdAndNotificacionTipoAndNotificacionEstado(
                    comandaId, TipoNotificacion.CAMBIO, EstadoNotificacion.ACTIVA)
            .isPresent();
    if (existeActiva) {
        throw new BusinessException(ErrorCode.INVALID_STATE,
                "Ya existe una notificación de cambio activa para esta comanda.", HttpStatus.CONFLICT);
    }

    Long visitaId = comanda.getVisita().getVisitaId();
    Mesa mesa = mesaRepository.findByVisita_VisitaId(visitaId)
            .orElseThrow(() -> new BusinessException(ErrorCode.BUSINESS_ERROR,
                    "La visita no tiene mesa asignada.", HttpStatus.CONFLICT));

    Notificacion notificacion = notificacionRepository.save(Notificacion.builder()
            .comanda(comanda)
            .mesa(mesa)
            .empleado(mesa.getMesero())
            .notificacionTipo(TipoNotificacion.CAMBIO)
            .notificacionEstado(EstadoNotificacion.ACTIVA)
            .build());

    mesaWsPublisher.publicarActualizacionMesa(visitaId, MesaWsPublisher.TipoEventoMesa.NOTIFICACION);

    return co.edu.unicauca.backend.modules.notificaciones.dto.response.NotificarCambioResponse.builder()
            .notificacionId(notificacion.getNotificacionId())
            .estado("ACTIVA")
            .build();
}
```

> Si el método de `NotificacionRepository` no existe, agregarlo: `Optional<Notificacion> findFirstByComanda_ComandaIdAndNotificacionTipoAndNotificacionEstado(Long, TipoNotificacion, EstadoNotificacion);`.

- [ ] **Step 3: Endpoint controller**

```java
/**
 * Endpoint para que el personal de producción registre una notificación de
 * cambio sobre una comanda pendiente.
 */
@PostMapping("/cambio")
@PreAuthorize("hasRole('PRODUCCION')")
@Operation(summary = "Notificar cambio sobre una comanda pendiente")
public ResponseEntity<ApiResponse<NotificarCambioResponse>> notificarCambio(
        @Valid @RequestBody NotificarCambioRequest request, Authentication auth) {
    NotificarCambioResponse data = notificacionService.notificarCambio(request.getComandaId(), auth);
    return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.created("Notificación de cambio creada", data));
}
```

- [ ] **Step 4: Tests controller**

`@WebMvcTest` con casos 201, 400 body inválido, 401, 403, 404, 409.

- [ ] **Step 5: Verificar tests**

Run: `cd backend && ./mvnw -q test -Dtest=NotificacionServiceTest,NotificacionControllerTest`
Expected: PASS.

- [ ] **Step 6: Reportar add+mensaje**

```
git add backend/src/main/java/co/edu/unicauca/backend/modules/notificaciones/
git add backend/src/test/java/co/edu/unicauca/backend/modules/notificaciones/
```
Mensaje:
```
feat(notificaciones): endpoint para notificar cambio desde producción
```

---

## Task 11: Fix mapping cliente `VisitaEstadoMapper`

**Files:**
- Modify: `backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/mapper/VisitaEstadoMapper.java`
- Test: `backend/src/test/java/co/edu/unicauca/backend/modules/mesas_comandas/mapper/VisitaEstadoMapperTest.java`

- [ ] **Step 1: Tests cubriendo los 5 estados**

```java
@Test void borrador_devuelveEnEspera() { assertThat(map(EstadoComanda.BORRADOR)).isEqualTo("En espera"); }
@Test void pendiente_devuelveEnEspera() { assertThat(map(EstadoComanda.PENDIENTE)).isEqualTo("En espera"); }
@Test void enPreparacion_devuelveEnPreparacion() { assertThat(map(EstadoComanda.EN_PREPARACION)).isEqualTo("En preparación"); }
@Test void listo_devuelveServido() { assertThat(map(EstadoComanda.LISTO)).isEqualTo("Servido"); }
@Test void completado_devuelveServido() { assertThat(map(EstadoComanda.COMPLETADO)).isEqualTo("Servido"); }
@Test void preReserva_devuelveEnEspera() { assertThat(map(EstadoComanda.PRE_RESERVA)).isEqualTo("En espera"); }
```

- [ ] **Step 2: Reemplazar el switch del mapper**

```java
private String resolverEstadoItem(EstadoComanda estado) {
    return switch (estado) {
        case BORRADOR, PENDIENTE -> "En espera";
        case EN_PREPARACION      -> "En preparación";
        case LISTO, COMPLETADO   -> "Servido";
        default                  -> "En espera";
    };
}
```
Actualizar Javadoc del método y del campo `ItemVisitaResponse.estadoItem`.

- [ ] **Step 3: Verificar tests**

Run: `cd backend && ./mvnw -q test -Dtest=VisitaEstadoMapperTest`
Expected: PASS.

- [ ] **Step 4: Reportar add+mensaje**

```
git add backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/mapper/VisitaEstadoMapper.java
git add backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/dto/response/ItemVisitaResponse.java
git add backend/src/test/java/co/edu/unicauca/backend/modules/mesas_comandas/mapper/VisitaEstadoMapperTest.java
```
Mensaje:
```
fix(mesas_comandas): mapear PENDIENTE a "En espera" en estado de visita
```

---

## Task 12: Auditoría de consistencia WS al cliente

**Files (revisión + posible add):**
- `ComandaBorradorService.confirmarBorrador` y mutadores de items.
- `NotificacionService.atenderCambio`, `servirPlatos`, `servirBebidas`.

- [ ] **Step 1: Inventariar mutadores**

Run: `grep -rn "comandaRepository.save\|comandaItemRepository.save\|comandaItemRepository.delete" backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/service/ backend/src/main/java/co/edu/unicauca/backend/modules/notificaciones/service/`

- [ ] **Step 2: Cruzar con call-sites de `publicarVisitaActualizada`**

Run: `grep -rn "publicarVisitaActualizada\|publicarOrdenClienteActualizada" backend/src/main/java/`

- [ ] **Step 3: Para cada mutador sin publish: agregar al final del método transaccional**

```java
wsPublisher.publicarVisitaActualizada(visitaId,
        VisitaActualizadaWsMessage.builder().visitaId(visitaId).build());
```

Inyectar `NotificacionWsPublisher wsPublisher;` si falta.

- [ ] **Step 4: Para cada cambio, agregar al test del service correspondiente un `verify(wsPublisher).publicarVisitaActualizada(...)`**

- [ ] **Step 5: Verificar suite completa**

Run: `cd backend && ./mvnw -q test`
Expected: PASS.

- [ ] **Step 6: Reportar add+mensaje**

```
git add backend/src/main/java/co/edu/unicauca/backend/modules/mesas_comandas/service/
git add backend/src/main/java/co/edu/unicauca/backend/modules/notificaciones/service/
git add backend/src/test/java/
```
Mensaje:
```
fix(mesas_comandas): publicar actualización al cliente en todos los mutadores de comanda
```

---

## Task 13: Postman Manual

**Files:**
- Create: `backend/postman/postman/collections/manual-testing/Al Toro - Manual Testing/70-07 Notificar cambio – COCINERO.request.yaml`
- Create: `.../90-12 Iniciar preparación – COCINERO.request.yaml`
- Create: `.../90-13 Marcar listo – COCINERO.request.yaml`

- [ ] **Step 1: Plantilla — copiar `90-08 Tablero produccion – COCINERO.request.yaml` como base**

Verificar la estructura YAML del archivo base (login autónomo, password `Al.Toro2026!`, token `tmpCocineroToken`, `afterResponse` con cleanup `pm.environment.unset`).

- [ ] **Step 2: Crear `90-12 Iniciar preparación – COCINERO.request.yaml`**

```yaml
name: 90-12 Iniciar preparación – COCINERO
method: POST
url: '{{baseUrl}}/api/comandas/produccion/{{tmpComandaId}}/iniciar'
auth:
  type: bearer
  bearer:
    token: '{{tmpCocineroToken}}'
events:
  - listen: prerequest
    script:
      exec:
        - |
          const cocineroEmail = 'cocinero@altoro.com';
          const password = 'Al.Toro2026!';
          const comandaId = pm.environment.get('comandaIdPendiente') || '1';
          pm.environment.set('tmpComandaId', comandaId);
          pm.sendRequest({
              url: pm.environment.get('baseUrl') + '/api/auth/login',
              method: 'POST',
              header: { 'Content-Type': 'application/json' },
              body: { mode: 'raw', raw: JSON.stringify({ email: cocineroEmail, password: password }) }
          }, (err, res) => {
              if (!err && res.code === 200) {
                  pm.environment.set('tmpCocineroToken', res.json().data.accessToken);
              }
          });
  - listen: test
    script:
      exec:
        - |
          pm.environment.unset('tmpCocineroToken');
          pm.environment.unset('tmpComandaId');
```

- [ ] **Step 3: Crear `90-13 Marcar listo – COCINERO.request.yaml`** (análogo, URL `/listo`, variable `comandaIdEnPreparacion`).

- [ ] **Step 4: Crear `70-07 Notificar cambio – COCINERO.request.yaml`**

URL: `{{baseUrl}}/api/notificaciones/cambio`. Body raw JSON `{ "comandaId": {{tmpComandaId}} }`. Resto análogo.

- [ ] **Step 5: Probar manualmente con servidor corriendo**

(Requiere `docker compose up` y seed con un cocinero `cocinero@altoro.com` / password `Al.Toro2026!`).

- [ ] **Step 6: Reportar add+mensaje**

```
git add "backend/postman/postman/collections/manual-testing/Al Toro - Manual Testing/70-07 Notificar cambio – COCINERO.request.yaml"
git add "backend/postman/postman/collections/manual-testing/Al Toro - Manual Testing/90-12 Iniciar preparación – COCINERO.request.yaml"
git add "backend/postman/postman/collections/manual-testing/Al Toro - Manual Testing/90-13 Marcar listo – COCINERO.request.yaml"
```
Mensaje:
```
test(postman): pruebas manuales para iniciar, listo y notificar cambio
```

---

## Task 14: Postman Automated — colección `comandas-produccion-estado`

**Files:**
- Create: `backend/postman/postman/collections/comandas-produccion-estado/.resources/definition.yaml`
- Create: una carpeta por endpoint con sus casos.

- [ ] **Step 1: Plantilla — copiar de `comandas-produccion/` (HU-02) como base estructural**

Replicar `definition.yaml`, variables (`cocineroToken`, `bartenderToken`, `meseroToken`, `clienteToken`, `passwordValida`, etc.) y el patrón de `beforeRequest`.

- [ ] **Step 2: Carpeta `iniciar-preparacion/` con archivos**

Ejemplo `01-01 Iniciar preparacion - 200 OK.request.yaml`:
- Antes: login cocinero, set comandaId pendiente seed.
- Request: POST `{{baseUrl}}/api/comandas/produccion/{{comandaIdPendiente}}/iniciar`.
- Tests: `pm.expect(pm.response.code).to.equal(200)`; valida estructura `data.comandaEstado === 'EN_PREPARACION'`; guarda `comandaIdEnPreparacion` para tests dependientes locales (no entre archivos).

Casos:
- `01-01 200 OK – cocina`
- `01-02 200 OK – barra`
- `01-03 401 sin token`
- `01-04 403 rol MESERO`
- `01-05 403 rol CAJERO`
- `01-06 403 rol CLIENTE`
- `01-07 403 estación incorrecta` (cocinero intenta sobre comanda BARRA)
- `01-08 404 comanda inexistente`
- `01-09 409 estado no PENDIENTE`
- `01-10 409 stock VENTA_DIRECTA insuficiente` (seed: stockActual=0)
- `01-11 409 stock PREPARACION insumo limitante` (seed: insumoStockActual=0)
- `01-12 200 OK – menú especial sin descuento`

- [ ] **Step 3: Carpeta `marcar-listo/`**

Casos: 200 cocina (`PLATOS_LISTOS` creada), 200 barra (`BEBIDAS_LISTAS`), 401, 403 rol/estación, 404, 409 estado inválido. Verificar en `afterResponse` la creación de la notificación con un GET adicional si hace falta.

- [ ] **Step 4: Carpeta `notificar-cambio/`**

Casos: 201 happy, 400 body sin `comandaId`, 401, 403 rol/estación, 404, 409 duplicado, 409 estado inválido.

- [ ] **Step 5: Asegurar códigos serializados en assertions**

Buscar y reemplazar enum por código:
```javascript
pm.expect(body.code).to.equal('NEG-002');     // INVALID_STATE
pm.expect(body.code).to.equal('NEG-001');     // BUSINESS_ERROR / INSUFFICIENT_STOCK
pm.expect(body.code).to.equal('AUTH-002');    // ACCESS_DENIED
pm.expect(body.code).to.equal('ENT-001');     // ENTITY_NOT_FOUND
pm.expect(body.code).to.equal('VAL-001');     // VALIDATION_ERROR
```

- [ ] **Step 6: Si requiere seeds nuevos, ampliar `V3__dev_data.sql`**

Conservar regla "no más allá de V5" (`backend/CLAUDE.md`). Si añade seeds: comandas en estado PENDIENTE / EN_PREPARACION para cocina y barra, productos VENTA_DIRECTA con stock 0, productos PREPARACION con insumo en stock 0, comanda con menú especial.

- [ ] **Step 7: Ejecutar runner**

Run: `npx newman run "backend/postman/postman/collections/comandas-produccion-estado/" -e backend/postman/postman/environments/dev.environment.json`
Expected: 100% pass.

- [ ] **Step 8: Reportar add+mensaje**

```
git add backend/postman/postman/collections/comandas-produccion-estado/
git add backend/src/main/resources/db/migration/V3__dev_data.sql  # si modificado
```
Mensaje:
```
test(postman): cobertura automatizada de transiciones de estado en producción
```

---

## Task 15: Documentación

**Files:**
- Modify: `backend/ENDPOINTS.md`
- Modify: `backend/CLAUDE.md`
- Modify: `backend/docs/coding-patterns.md`

- [ ] **Step 1: `ENDPOINTS.md` — agregar 3 endpoints**

```
POST /api/comandas/produccion/{comandaId}/iniciar    PRODUCCION    Inicia preparación, descuenta inventario
POST /api/comandas/produccion/{comandaId}/listo      PRODUCCION    Marca lista, crea notificación PLATOS_LISTOS / BEBIDAS_LISTAS
POST /api/notificaciones/cambio                      PRODUCCION    Crea notificación CAMBIO sobre comanda pendiente
```

- [ ] **Step 2: `backend/CLAUDE.md` — sección WebSocket**

Actualizar la línea sobre `ComandaProduccionEventoWsMessage`:
- Reemplazar `tipo ∈ {CREADA, ELIMINADA, COMPLETADA}` por `{CREADA, ACTUALIZADA, ELIMINADA, COMPLETADA}`.
- Documentar `nuevoEstado` en `ACTUALIZADA`.

- [ ] **Step 3: `backend/docs/coding-patterns.md` — bloque "evento unificado de producción"**

Análogo: incluir `ACTUALIZADA` y `nuevoEstado`.

- [ ] **Step 4: Reportar add+mensaje**

```
git add backend/ENDPOINTS.md backend/CLAUDE.md backend/docs/coding-patterns.md
```
Mensaje:
```
docs: documentar endpoints y evento ACTUALIZADA del WS de producción
```

---

## Task 16: Verificación final + cobertura

**Files:** ninguno (solo verificación).

- [ ] **Step 1: Suite completa**

Run: `cd backend && ./mvnw clean test jacoco:report`
Expected: BUILD SUCCESS, 0 fallidos, número de tests > baseline.

- [ ] **Step 2: Verificar cobertura por capa (`backend/target/site/jacoco/index.html`)**

Mínimos según `docs/testing.md`:
- Services 90–95% (objetivo 100% en clases nuevas)
- Controllers 85–90%
- Mappers 90–95%
- Validators 95%+
- Repositories (custom queries) 70–80%

- [ ] **Step 3: Smoke manual (servidor corriendo)**

Probar flujo end-to-end:
1. Login MESERO, asignar mesa con preorden.
2. Login MESERO, confirmar comanda (BORRADOR→PENDIENTE).
3. Login COCINERO, GET tablero — comanda en pendientes.
4. POST `/iniciar` — pasa a `enPreparacion`, stock decrementado.
5. POST `/listo` — pasa a `listos`, notificación PLATOS_LISTOS creada.
6. Login MESERO, atender PLATOS_LISTOS → comanda COMPLETADO, sale del tablero.
7. Verificar dashboard cliente: estados visibles "En espera"/"En preparación"/"Servido".
8. Probar POST `/api/notificaciones/cambio` sobre otra PENDIENTE → mesero ve icono.
9. Mesero "atender cambio" — comanda regresa a borrador o se fusiona (flujo existente).

- [ ] **Step 4: Reportar diff total y mensajes acumulados**

Run: `git status; git diff --stat origin/develop..HEAD`

Reportar a la usuaria los pasos pendientes de commit (uno por tarea) y esperar instrucciones.

---

## Self-Review

**Spec coverage:**
- CA-02 → Task 10 ✓
- CA-04 → Tasks 5 (descuento) + 7 (transición) + 9 (endpoint) ✓
- CA-05 → Tasks 8 + 9 ✓
- Validador dual → Task 6 ✓
- WS `ACTUALIZADA` → Task 3 + 7 + 8 ✓
- Mapping cliente → Task 11 ✓
- Auditoría WS cliente → Task 12 ✓
- Postman manual → Task 13 ✓
- Postman automated → Task 14 ✓
- Documentación → Task 15 ✓
- Cobertura final → Task 16 ✓
- Excluido (PRE_RESERVA→BORRADOR) → ticket aparte (sec 12 spec) ✓

**Placeholders:** ninguno — cada tarea tiene código real o reglas verificables.

**Type consistency:**
- `descontarPorComanda(Comanda, Empleado)` mismo nombre en Tasks 5, 7.
- `sumCantidadInsumoComprometida(Long)` mismo nombre en Tasks 4, 6.
- `findByProductoIdFetchInsumo(Long)` mismo nombre en Tasks 1, 5, 6.
- `publicarEventoProduccion(EstacionComanda, ComandaProduccionEventoWsMessage)` y `publicarVisitaActualizada(Long, VisitaActualizadaWsMessage)` consistentes con la firma actual del publisher.
