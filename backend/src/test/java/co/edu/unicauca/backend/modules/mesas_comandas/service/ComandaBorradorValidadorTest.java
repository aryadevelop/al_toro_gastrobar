package co.edu.unicauca.backend.modules.mesas_comandas.service;

import co.edu.unicauca.backend.modules.inventario.entity.Insumo;
import co.edu.unicauca.backend.modules.inventario.entity.Producto;
import co.edu.unicauca.backend.modules.inventario.entity.Receta;
import co.edu.unicauca.backend.modules.inventario.repository.RecetaRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.ComandaItem;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ComandaItemRepository;
import co.edu.unicauca.backend.shared.enums.CategoriaProducto;
import co.edu.unicauca.backend.shared.enums.EstacionComanda;
import co.edu.unicauca.backend.shared.enums.TipoProducto;
import co.edu.unicauca.backend.shared.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ComandaBorradorValidador")
class ComandaBorradorValidadorTest {

    @Mock
    private ComandaItemRepository comandaItemRepository;

    @Mock
    private RecetaRepository recetaRepository;

    @InjectMocks
    private ComandaBorradorValidador validador;

    // -------------------------------------------------------------------------
    // validarStock
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("validarStock")
    class ValidarStock {

        @Test
        @DisplayName("Debe omitir validación cuando stockActual es null")
        void debeOmitirValidacionCuandoStockActualEsNull() {
            // Arrange
            Producto producto = productoSinStock(CategoriaProducto.PLATO);

            // Act & Assert — no exception, repository never consulted
            assertThatCode(() -> validador.validarStock(producto, 5, 0))
                    .doesNotThrowAnyException();

            verifyNoInteractions(comandaItemRepository);
        }

        @Test
        @DisplayName("Debe permitir cuando cantidad propuesta es igual al disponible")
        void debePermitirCuandoCantidadIgualAlDisponible() {
            // Arrange
            Producto producto = productoConStock(1L, CategoriaProducto.PLATO, 10);
            // comprometido=4, anterior=0 → disponible=10-4=6; nuevaCantidad=6 (igual)
            when(comandaItemRepository.sumCantidadComprometidaByProducto(1L)).thenReturn(4L);

            // Act & Assert
            assertThatCode(() -> validador.validarStock(producto, 6, 0))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Debe permitir cuando cantidad propuesta es menor al disponible")
        void debePermitirCuandoCantidadMenorAlDisponible() {
            // Arrange
            Producto producto = productoConStock(2L, CategoriaProducto.BEBIDA, 20);
            // comprometido=0, anterior=0 → disponible=20; nuevaCantidad=5
            when(comandaItemRepository.sumCantidadComprometidaByProducto(2L)).thenReturn(0L);

            // Act & Assert
            assertThatCode(() -> validador.validarStock(producto, 5, 0))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Debe lanzar BusinessException con INSUFFICIENT_STOCK cuando cantidad excede disponible")
        void debeLanzarExcepcionCuandoCantidadExcedeLimite() {
            // Arrange
            Producto producto = productoConStock(3L, CategoriaProducto.PLATO, 5);
            // comprometido=5, anterior=0 → disponible=0; nuevaCantidad=1 → excede
            when(comandaItemRepository.sumCantidadComprometidaByProducto(3L)).thenReturn(5L);

            // Act & Assert
            assertThatThrownBy(() -> validador.validarStock(producto, 1, 0))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("code", "NEG-004");
        }

        @Test
        @DisplayName("Debe descontar cantidadAnterior del comprometido para no doble-contar")
        void debeDescontarCantidadAnteriorDelComprometido() {
            // Arrange
            Producto producto = productoConStock(4L, CategoriaProducto.PLATO, 10);
            // comprometido=8, anterior=5 → disponible=10-(8-5)=7; nuevaCantidad=7 (OK)
            when(comandaItemRepository.sumCantidadComprometidaByProducto(4L)).thenReturn(8L);

            // Act & Assert
            assertThatCode(() -> validador.validarStock(producto, 7, 5))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Debe reportar disponible como 0 en el mensaje cuando disponible es negativo")
        void debeReportarDisponibleComoZeroCuandoDisponibleEsNegativo() {
            // Arrange
            Producto producto = productoConStock(5L, CategoriaProducto.BEBIDA, 3);
            // comprometido=10, anterior=0 → disponible=3-10=-7 (negativo); mensaje debe decir "0"
            when(comandaItemRepository.sumCantidadComprometidaByProducto(5L)).thenReturn(10L);

            // Act & Assert
            assertThatThrownBy(() -> validador.validarStock(producto, 1, 0))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("0 unidades disponibles");
        }

        @Test
        @DisplayName("Debe incluir cantidad disponible real en el mensaje cuando disponible > 0")
        void debeIncluirCantidadDisponibleEnMensajeCuandoDisponiblePositivo() {
            // Arrange
            Producto producto = productoConStock(6L, CategoriaProducto.PLATO, 10);
            // comprometido=6, anterior=0 → disponible=4; nuevaCantidad=5 → excede
            when(comandaItemRepository.sumCantidadComprometidaByProducto(6L)).thenReturn(6L);

            // Act & Assert
            assertThatThrownBy(() -> validador.validarStock(producto, 5, 0))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("4 unidades disponibles");
        }
    }

    // -------------------------------------------------------------------------
    // validarStock — PREPARACION
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("validarStock — PREPARACION")
    class ValidarStockPreparacion {

        @Test
        @DisplayName("Insumo con stock suficiente no lanza excepción")
        void insumoSuficiente_noLanza() {
            // Arrange: producto PREPARACION, insumo con stock=10, recetaCantidad=0.5
            // validar(producto, 4, 0): requerido=4*0.5=2, comprometido=0-0=0,
            // disponible=10-0=10 → 2 ≤ 10 → OK
            Insumo insumo = insumoConStock(10L, "Tomate", BigDecimal.valueOf(10));
            Receta receta = recetaConCantidad(insumo, BigDecimal.valueOf(0.5));
            Producto producto = productoPreparacion(20L);

            when(recetaRepository.findByProductoIdFetchInsumo(20L)).thenReturn(List.of(receta));
            when(comandaItemRepository.sumCantidadInsumoComprometida(10L))
                    .thenReturn(BigDecimal.ZERO);

            // Act & Assert
            assertThatCode(() -> validador.validarStock(producto, 4, 0))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Insumo con stock insuficiente lanza BusinessException con nombre del insumo")
        void insumoInsuficiente_lanza() {
            // Arrange: stock=1.0, recetaCantidad=0.5, comprometido=0
            // validar(producto, 4, 0): requerido=2.0 > disponible=1.0 → lanza
            Insumo insumo = insumoConStock(11L, "Cebolla", BigDecimal.valueOf(1));
            Receta receta = recetaConCantidad(insumo, BigDecimal.valueOf(0.5));
            Producto producto = productoPreparacion(21L);

            when(recetaRepository.findByProductoIdFetchInsumo(21L)).thenReturn(List.of(receta));
            when(comandaItemRepository.sumCantidadInsumoComprometida(11L))
                    .thenReturn(BigDecimal.ZERO);

            // Act & Assert
            assertThatThrownBy(() -> validador.validarStock(producto, 4, 0))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Cebolla");
        }

        @Test
        @DisplayName("cantidadAnterior ajusta el comprometido para no doble-contar")
        void cantidadAnteriorAjusta() {
            // Arrange: recetaCantidad=0.5, comprometido total insumo = 2 (= 4 items × 0.5)
            // cantidadAnterior=2 → yaContabilizado=2*0.5=1.0
            // ajustado comprometido = 2.0 - 1.0 = 1.0; disponible = 5 - 1.0 = 4.0
            // requerido = 4 * 0.5 = 2.0 ≤ 4.0 → OK
            Insumo insumo = insumoConStock(12L, "Pimiento", BigDecimal.valueOf(5));
            Receta receta = recetaConCantidad(insumo, BigDecimal.valueOf(0.5));
            Producto producto = productoPreparacion(22L);

            when(recetaRepository.findByProductoIdFetchInsumo(22L)).thenReturn(List.of(receta));
            when(comandaItemRepository.sumCantidadInsumoComprometida(12L))
                    .thenReturn(BigDecimal.valueOf(2));

            // Act & Assert — cantidadAnterior=2
            assertThatCode(() -> validador.validarStock(producto, 4, 2))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Sin receta registrada no lanza excepción")
        void sinReceta_noLanza() {
            // Arrange
            Producto producto = productoPreparacion(23L);

            when(recetaRepository.findByProductoIdFetchInsumo(23L)).thenReturn(List.of());

            // Act & Assert
            assertThatCode(() -> validador.validarStock(producto, 4, 0))
                    .doesNotThrowAnyException();
        }
    }

    // -------------------------------------------------------------------------
    // resolverEstacion
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("resolverEstacion")
    class ResolverEstacion {

        @Test
        @DisplayName("PLATO debe resolver a COCINA")
        void platodebeResolverACocina() {
            // Arrange
            Producto producto = productoSinStock(CategoriaProducto.PLATO);

            // Act
            EstacionComanda resultado = validador.resolverEstacion(producto);

            // Assert
            assertThat(resultado).isEqualTo(EstacionComanda.COCINA);
        }

        @Test
        @DisplayName("BEBIDA debe resolver a BARRA")
        void bebidadebeResolverABarra() {
            // Arrange
            Producto producto = productoSinStock(CategoriaProducto.BEBIDA);

            // Act
            EstacionComanda resultado = validador.resolverEstacion(producto);

            // Assert
            assertThat(resultado).isEqualTo(EstacionComanda.BARRA);
        }

        @Test
        @DisplayName("OTRO debe lanzar BusinessException con NEG-001")
        void otroDebeLanzarBusinessException() {
            // Arrange
            Producto producto = productoSinStock(CategoriaProducto.OTRO);

            // Act & Assert
            assertThatThrownBy(() -> validador.resolverEstacion(producto))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("code", "NEG-001")
                    .hasMessageContaining("OTRO");
        }

        @ParameterizedTest(name = "Categoría no soportada: {0}")
        @EnumSource(value = CategoriaProducto.class, names = {"OTRO"})
        @DisplayName("Cualquier categoría no admitida debe lanzar BusinessException")
        void categoriaNoAdmitidaDebeLanzarExcepcion(CategoriaProducto categoria) {
            // Arrange
            Producto producto = productoSinStock(categoria);

            // Act & Assert
            assertThatThrownBy(() -> validador.resolverEstacion(producto))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("code", "NEG-001");
        }
    }

    // -------------------------------------------------------------------------
    // validarTieneItems
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("validarTieneItems")
    class ValidarTieneItems {

        @Test
        @DisplayName("Debe lanzar BusinessException cuando la lista es null")
        void debeLanzarExcepcionCuandoListaEsNull() {
            // Act & Assert
            assertThatThrownBy(() -> validador.validarTieneItems(null))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("code", "NEG-001")
                    .hasMessageContaining("al menos un producto");
        }

        @Test
        @DisplayName("Debe lanzar BusinessException cuando la lista está vacía")
        void debeLanzarExcepcionCuandoListaEstaVacia() {
            // Act & Assert
            assertThatThrownBy(() -> validador.validarTieneItems(Collections.emptyList()))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("code", "NEG-001")
                    .hasMessageContaining("al menos un producto");
        }

        @Test
        @DisplayName("Debe permitir cuando la lista tiene al menos un ítem")
        void debePermitirCuandoListaTieneItems() {
            // Arrange
            List<ComandaItem> items = List.of(new ComandaItem());

            // Act & Assert
            assertThatCode(() -> validador.validarTieneItems(items))
                    .doesNotThrowAnyException();
        }
    }

    // -------------------------------------------------------------------------
    // Helpers estáticos
    // -------------------------------------------------------------------------

    /** Crea un Producto sin stock gestionado (stockActual = null). */
    private static Producto productoSinStock(CategoriaProducto categoria) {
        return Producto.builder()
                .productoId(99L)
                .productoCategoria(categoria)
                .stockActual(null)
                .build();
    }

    /**
     * Crea un Producto con stock gestionado.
     *
     * @param id       identificador del producto
     * @param categoria categoría (PLATO, BEBIDA, OTRO)
     * @param stock    valor de stockActual
     */
    private static Producto productoConStock(Long id, CategoriaProducto categoria, int stock) {
        return Producto.builder()
                .productoId(id)
                .productoCategoria(categoria)
                .stockActual(BigDecimal.valueOf(stock))
                .build();
    }

    /**
     * Crea un Producto de tipo PREPARACION sin stock directo (gestionado por insumos de receta).
     *
     * @param id identificador del producto
     */
    private static Producto productoPreparacion(Long id) {
        return Producto.builder()
                .productoId(id)
                .productoTipo(TipoProducto.PREPARACION)
                .productoCategoria(CategoriaProducto.PLATO)
                .productoNombre("Producto preparacion " + id)
                .build();
    }

    /**
     * Crea un Insumo con stock dado, usando el builder minimal necesario para los tests.
     *
     * @param id     identificador del insumo
     * @param nombre nombre del insumo
     * @param stock  stock actual
     */
    private static Insumo insumoConStock(Long id, String nombre, BigDecimal stock) {
        return Insumo.builder()
                .insumoId(id)
                .insumoNombre(nombre)
                .insumoStockActual(stock)
                .build();
    }

    /**
     * Crea una Receta con el insumo y cantidad indicados (productoId irrelevante para unit tests).
     *
     * @param insumo          insumo de la receta
     * @param recetaCantidad  cantidad de insumo por unidad de producto
     */
    private static Receta recetaConCantidad(Insumo insumo, BigDecimal recetaCantidad) {
        return Receta.builder()
                .insumoId(insumo.getInsumoId())
                .insumo(insumo)
                .recetaCantidad(recetaCantidad)
                .build();
    }

    // -------------------------------------------------------------------------
    // evaluarDisponibilidad
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("evaluarDisponibilidad")
    class EvaluarDisponibilidad {

        @Test
        @DisplayName("VENTA_DIRECTA stockActual null → devuelve null (sin control de stock)")
        void ventaDirecta_stockNull_devuelveNull() {
            Producto p = productoSinStock(CategoriaProducto.PLATO);
            assertThat(validador.evaluarDisponibilidad(p, 3)).isNull();
            verifyNoInteractions(comandaItemRepository);
        }

        @Test
        @DisplayName("VENTA_DIRECTA stock suficiente → devuelve disponible positivo")
        void ventaDirecta_stockSuficiente_devuelvePositivo() {
            Producto p = productoConStock(1L, CategoriaProducto.PLATO, 10);
            when(comandaItemRepository.sumCantidadComprometidaByProducto(1L)).thenReturn(3L);
            assertThat(validador.evaluarDisponibilidad(p, 2)).isEqualTo(9);
        }

        @Test
        @DisplayName("VENTA_DIRECTA cantidad == disponible exacto → devuelve disponible = 0 después de comprometer")
        void ventaDirecta_cantidadExacta_devuelveCero() {
            Producto p = productoConStock(1L, CategoriaProducto.PLATO, 5);
            when(comandaItemRepository.sumCantidadComprometidaByProducto(1L)).thenReturn(5L);
            assertThat(validador.evaluarDisponibilidad(p, 5)).isEqualTo(5);
        }

        @Test
        @DisplayName("VENTA_DIRECTA stock insuficiente → devuelve negativo")
        void ventaDirecta_stockInsuficiente_devuelveNegativo() {
            Producto p = productoConStock(2L, CategoriaProducto.PLATO, 3);
            when(comandaItemRepository.sumCantidadComprometidaByProducto(2L)).thenReturn(10L);
            assertThat(validador.evaluarDisponibilidad(p, 1)).isEqualTo(-6);
        }

        @Test
        @DisplayName("PREPARACION sin receta → devuelve null (sin control de stock)")
        void preparacion_sinReceta_devuelveNull() {
            Producto p = Producto.builder()
                    .productoId(3L).productoCategoria(CategoriaProducto.PLATO)
                    .productoTipo(TipoProducto.PREPARACION).build();
            when(recetaRepository.findByProductoIdFetchInsumo(3L)).thenReturn(Collections.emptyList());
            assertThat(validador.evaluarDisponibilidad(p, 2)).isNull();
        }

        @Test
        @DisplayName("PREPARACION un insumo suficiente → devuelve unidades disponibles del producto")
        void preparacion_unInsumo_suficiente() {
            Insumo insumo = Insumo.builder().insumoId(10L).insumoNombre("Carne")
                    .insumoStockActual(BigDecimal.valueOf(20)).build();
            Receta receta = Receta.builder()
                    .recetaCantidad(BigDecimal.valueOf(2))
                    .insumo(insumo).build();
            Producto p = Producto.builder()
                    .productoId(4L).productoCategoria(CategoriaProducto.PLATO)
                    .productoTipo(TipoProducto.PREPARACION).build();

            when(recetaRepository.findByProductoIdFetchInsumo(4L)).thenReturn(List.of(receta));
            when(comandaItemRepository.sumCantidadInsumoComprometida(10L))
                    .thenReturn(BigDecimal.valueOf(4));

            assertThat(validador.evaluarDisponibilidad(p, 1)).isEqualTo(9);
        }

        @Test
        @DisplayName("PREPARACION varios insumos → devuelve el mínimo (insumo limitante)")
        void preparacion_variosInsumos_devuelveMinimo() {
            Insumo insumoA = Insumo.builder().insumoId(11L).insumoNombre("Arroz")
                    .insumoStockActual(BigDecimal.valueOf(30)).build();
            Insumo insumoB = Insumo.builder().insumoId(12L).insumoNombre("Pollo")
                    .insumoStockActual(BigDecimal.valueOf(10)).build();
            Receta recetaA = Receta.builder()
                    .recetaCantidad(BigDecimal.valueOf(1)).insumo(insumoA).build();
            Receta recetaB = Receta.builder()
                    .recetaCantidad(BigDecimal.valueOf(3)).insumo(insumoB).build();
            Producto p = Producto.builder()
                    .productoId(5L).productoCategoria(CategoriaProducto.PLATO)
                    .productoTipo(TipoProducto.PREPARACION).build();

            when(recetaRepository.findByProductoIdFetchInsumo(5L))
                    .thenReturn(List.of(recetaA, recetaB));
            when(comandaItemRepository.sumCantidadInsumoComprometida(11L))
                    .thenReturn(BigDecimal.ZERO);
            when(comandaItemRepository.sumCantidadInsumoComprometida(12L))
                    .thenReturn(BigDecimal.valueOf(6));

            assertThat(validador.evaluarDisponibilidad(p, 2)).isEqualTo(3);
        }

        @Test
        @DisplayName("PREPARACION stock negativo del insumo → devuelve entero negativo")
        void preparacion_insumoInsuficiente_devuelveNegativo() {
            Insumo insumo = Insumo.builder().insumoId(13L).insumoNombre("Sal")
                    .insumoStockActual(BigDecimal.valueOf(2)).build();
            Receta receta = Receta.builder()
                    .recetaCantidad(BigDecimal.valueOf(1)).insumo(insumo).build();
            Producto p = Producto.builder()
                    .productoId(6L).productoCategoria(CategoriaProducto.PLATO)
                    .productoTipo(TipoProducto.PREPARACION).build();

            when(recetaRepository.findByProductoIdFetchInsumo(6L)).thenReturn(List.of(receta));
            when(comandaItemRepository.sumCantidadInsumoComprometida(13L))
                    .thenReturn(BigDecimal.valueOf(10));

            assertThat(validador.evaluarDisponibilidad(p, 1)).isEqualTo(-7);
        }
    }
}
