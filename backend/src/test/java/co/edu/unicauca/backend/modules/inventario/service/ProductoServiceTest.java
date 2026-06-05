package co.edu.unicauca.backend.modules.inventario.service;

import co.edu.unicauca.backend.modules.inventario.dto.response.CategoriaCartaResponse;
import co.edu.unicauca.backend.modules.inventario.dto.response.MenuEspecialResponse;
import co.edu.unicauca.backend.modules.inventario.dto.response.ProductoBusquedaResponse;
import co.edu.unicauca.backend.modules.inventario.dto.response.ProductoInventarioResponse;
import co.edu.unicauca.backend.modules.inventario.entity.CategoriaCarta;
import co.edu.unicauca.backend.modules.inventario.entity.Insumo;
import co.edu.unicauca.backend.modules.inventario.entity.OpcionModificacion;
import co.edu.unicauca.backend.modules.inventario.entity.Producto;
import co.edu.unicauca.backend.modules.inventario.entity.Receta;
import co.edu.unicauca.backend.modules.inventario.mapper.ProductoMapper;
import co.edu.unicauca.backend.modules.inventario.repository.CategoriaCartaRepository;
import co.edu.unicauca.backend.modules.inventario.repository.ProductoOpcionModificacionRepository;
import co.edu.unicauca.backend.modules.inventario.repository.ProductoRepository;
import co.edu.unicauca.backend.modules.inventario.repository.RecetaRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ComandaItemRepository;
import co.edu.unicauca.backend.shared.enums.EstadoGenerico;
import co.edu.unicauca.backend.shared.enums.TipoProducto;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios de {@link ProductoService}.
 *
 * <p>Cubre los métodos:
 * <ul>
 *   <li>{@link ProductoService#obtenerCarta()}</li>
 *   <li>{@link ProductoService#obtenerMenusEspeciales()}</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ProductoService")
class ProductoServiceTest {

    @Mock ProductoRepository productoRepository;
    @Mock CategoriaCartaRepository categoriaCartaRepository;
    @Mock ProductoOpcionModificacionRepository productoOpcionModificacionRepository;
    @Mock RecetaRepository recetaRepository;
    @Mock ComandaItemRepository comandaItemRepository;
    @Mock co.edu.unicauca.backend.modules.inventario.repository.MenuBebidaDisponibleRepository menuBebidaDisponibleRepository;
    @Mock ProductoMapper productoMapper;

    @InjectMocks
    ProductoService productoService;

    // ── Helpers ───────────────────────────────────────────────────────────────

    private CategoriaCarta categoriaConOrden(Integer id, Integer orden) {
        return CategoriaCarta.builder()
                .categoriacartaId(id)
                .categoriaNombre("Categoria " + id)
                .orden(orden)
                .activo(true)
                .build();
    }

    private Producto productoEnCategoria(Long id, CategoriaCarta categoria) {
        return Producto.builder()
                .productoId(id)
                .productoNombre("Producto " + id)
                .categoriaCarta(categoria)
                .productoEstado(EstadoGenerico.ACTIVO)
                .productoPrecio(new BigDecimal("10.00"))
                .productoTipo(co.edu.unicauca.backend.shared.enums.TipoProducto.PREPARACION)
                .productoCategoria(co.edu.unicauca.backend.shared.enums.CategoriaProducto.PLATO)
                .build();
    }

    private CategoriaCartaResponse categoriaResponseConOrden(Integer orden) {
        return CategoriaCartaResponse.builder()
                .categoriaId(orden)
                .categoriaNombre("Categoria " + orden)
                .orden(orden)
                .productos(List.of())
                .build();
    }

    // =========================================================================
    //  obtenerCarta
    // =========================================================================

    @Nested
    @DisplayName("obtenerCarta")
    class ObtenerCartaTests {

        @Test
        @DisplayName("sin productos retorna lista vacia")
        void sinProductos_retornaListaVacia() {
            when(productoRepository.findProductosCarta(EstadoGenerico.ACTIVO))
                    .thenReturn(List.of());

            List<CategoriaCartaResponse> result = productoService.obtenerCarta();

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("productos agrupados por categoria retorna un CategoriaCartaResponse por categoria")
        void productosAgrupadosPorCategoria() {
            CategoriaCarta cat1 = categoriaConOrden(1, 1);
            CategoriaCarta cat2 = categoriaConOrden(2, 2);

            Producto p1 = productoEnCategoria(1L, cat1);
            Producto p2 = productoEnCategoria(2L, cat1);
            Producto p3 = productoEnCategoria(3L, cat2);

            when(productoRepository.findProductosCarta(EstadoGenerico.ACTIVO))
                    .thenReturn(List.of(p1, p2, p3));
            when(productoMapper.toCategoriaCartaResponse(eq(cat1), any()))
                    .thenReturn(categoriaResponseConOrden(1));
            when(productoMapper.toCategoriaCartaResponse(eq(cat2), any()))
                    .thenReturn(categoriaResponseConOrden(2));

            List<CategoriaCartaResponse> result = productoService.obtenerCarta();

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("categorias ordenadas por campo orden ascendente")
        void categoriasOrdenadosPorOrden() {
            CategoriaCarta cat1 = categoriaConOrden(1, 1);
            CategoriaCarta cat2 = categoriaConOrden(2, 2);

            Producto p1 = productoEnCategoria(1L, cat1);
            Producto p2 = productoEnCategoria(2L, cat2);

            // Devuelve primero la categoria con orden mayor para verificar que el servicio las reordena
            when(productoRepository.findProductosCarta(EstadoGenerico.ACTIVO))
                    .thenReturn(List.of(p2, p1));
            when(productoMapper.toCategoriaCartaResponse(eq(cat1), any()))
                    .thenReturn(categoriaResponseConOrden(1));
            when(productoMapper.toCategoriaCartaResponse(eq(cat2), any()))
                    .thenReturn(categoriaResponseConOrden(2));

            List<CategoriaCartaResponse> result = productoService.obtenerCarta();

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getOrden()).isLessThan(result.get(1).getOrden());
        }

        @Test
        @DisplayName("llama findProductosCarta con EstadoGenerico.ACTIVO")
        void llamaFindProductosCartaConEstadoActivo() {
            when(productoRepository.findProductosCarta(EstadoGenerico.ACTIVO))
                    .thenReturn(List.of());

            productoService.obtenerCarta();

            verify(productoRepository).findProductosCarta(EstadoGenerico.ACTIVO);
        }
    }

    // =========================================================================
    //  obtenerMenusEspeciales
    // =========================================================================

    @Nested
    @DisplayName("obtenerMenusEspeciales")
    class ObtenerMenusEspecialesTests {

        @Test
        @DisplayName("sin menus especiales retorna lista vacia")
        void sinMenusEspeciales_retornaListaVacia() {
            when(productoRepository
                    .findByProductoEstadoAndMenuEspecialTrueOrderByProductoNombreAsc(EstadoGenerico.ACTIVO))
                    .thenReturn(List.of());

            List<MenuEspecialResponse> result = productoService.obtenerMenusEspeciales();

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("cada menu especial consulta sus opciones de modificacion")
        void cadaMenuConsultaSusOpciones() {
            Producto menu1 = mock(Producto.class);
            Producto menu2 = mock(Producto.class);
            when(menu1.getProductoId()).thenReturn(1L);
            when(menu2.getProductoId()).thenReturn(2L);

            when(productoRepository
                    .findByProductoEstadoAndMenuEspecialTrueOrderByProductoNombreAsc(EstadoGenerico.ACTIVO))
                    .thenReturn(List.of(menu1, menu2));
            when(productoOpcionModificacionRepository
                    .findOpcionesActivasByProductoId(anyLong(), eq(EstadoGenerico.ACTIVO)))
                    .thenReturn(List.of());
            when(productoMapper.toMenuEspecialResponse(any(), any(), any()))
                    .thenReturn(mock(MenuEspecialResponse.class));

            productoService.obtenerMenusEspeciales();

            verify(productoOpcionModificacionRepository, times(2))
                    .findOpcionesActivasByProductoId(anyLong(), eq(EstadoGenerico.ACTIVO));
        }

        @Test
        @DisplayName("delega a mapper toMenuEspecialResponse por cada menu")
        void delegaAMapper_toMenuEspecialResponse() {
            Producto menu1 = mock(Producto.class);
            Producto menu2 = mock(Producto.class);
            when(menu1.getProductoId()).thenReturn(1L);
            when(menu2.getProductoId()).thenReturn(2L);

            List<OpcionModificacion> opciones = List.of(mock(OpcionModificacion.class));

            when(productoRepository
                    .findByProductoEstadoAndMenuEspecialTrueOrderByProductoNombreAsc(EstadoGenerico.ACTIVO))
                    .thenReturn(List.of(menu1, menu2));
            when(productoOpcionModificacionRepository
                    .findOpcionesActivasByProductoId(anyLong(), eq(EstadoGenerico.ACTIVO)))
                    .thenReturn(opciones);
            when(productoMapper.toMenuEspecialResponse(any(), any(), any()))
                    .thenReturn(mock(MenuEspecialResponse.class));

            productoService.obtenerMenusEspeciales();

            verify(productoMapper, times(2)).toMenuEspecialResponse(any(), any(), any());
        }

        @Test
        @DisplayName("llama findByProductoEstadoAndMenuEspecialTrue con EstadoGenerico.ACTIVO")
        void llamaFindByEstadoAndMenuEspecialTrue() {
            when(productoRepository
                    .findByProductoEstadoAndMenuEspecialTrueOrderByProductoNombreAsc(EstadoGenerico.ACTIVO))
                    .thenReturn(List.of());

            productoService.obtenerMenusEspeciales();

            verify(productoRepository)
                    .findByProductoEstadoAndMenuEspecialTrueOrderByProductoNombreAsc(EstadoGenerico.ACTIVO);
        }
    }

    // =========================================================================
    //  listarProductosInventario
    // =========================================================================

    @Nested
    @DisplayName("listarProductosInventario")
    class ListarProductosInventarioTests {

        @Test
        @DisplayName("sin filtros retorna todos los productos")
        void sinFiltros_retornaTodosLosProductos() {
            CategoriaCarta categoria = categoriaConOrden(1, 1);
            Producto producto = productoEnCategoria(1L, categoria);
            ProductoInventarioResponse response = ProductoInventarioResponse.builder()
                    .productoId(1L)
                    .productoNombre("Producto 1")
                    .categoriaNombre("Categoria 1")
                    .productoPrecio(producto.getProductoPrecio())
                    .stockActual(producto.getStockActual())
                    .productoEstado(producto.getProductoEstado().name())
                    .build();

            when(productoRepository.buscarPorCategoriaYNombre(null, null))
                    .thenReturn(List.of(producto));
            when(productoMapper.toInventarioResponse(producto, producto.getStockActual()))
                    .thenReturn(response);

            List<ProductoInventarioResponse> result = productoService.listarProductosInventario(null, null);

            assertThat(result).containsExactly(response);
            verify(productoRepository).buscarPorCategoriaYNombre(null, null);
        }

        @Test
        @DisplayName("categoria inexistente lanza excepcion de negocio")
        void categoriaInexistente_lanzaExcepcion() {
            when(categoriaCartaRepository.findByCategoriaNombreIgnoreCase("Bebidas"))
                    .thenReturn(Optional.empty());

            org.assertj.core.api.Assertions.assertThatThrownBy(
                    () -> productoService.listarProductosInventario("Bebidas", null))
                    .isInstanceOf(co.edu.unicauca.backend.shared.exception.BusinessException.class)
                    .hasMessage("No existe la categoría 'Bebidas'.");
        }

        @Test
        @DisplayName("producto de preparacion usa la disponibilidad de insumos si stock es null")
        void productoPreparacion_usarDisponibilidadDeInsumos() {
            CategoriaCarta categoria = categoriaConOrden(1, 1);
            Producto producto = productoEnCategoria(1L, categoria);
            producto.setStockActual(null);
            producto.setProductoTipo(TipoProducto.PREPARACION);

            Insumo insumo = Insumo.builder()
                    .insumoId(1L)
                    .insumoNombre("Harina")
                    .insumoStockActual(BigDecimal.valueOf(10.000))
                    .insumoUnidad(co.edu.unicauca.backend.shared.enums.UnidadMedida.KG)
                    .insumoEstado(co.edu.unicauca.backend.shared.enums.EstadoGenerico.ACTIVO)
                    .build();

            Receta receta = Receta.builder()
                    .insumoId(insumo.getInsumoId())
                    .productoId(producto.getProductoId())
                    .insumo(insumo)
                    .recetaCantidad(BigDecimal.valueOf(2.000))
                    .build();

            ProductoInventarioResponse response = ProductoInventarioResponse.builder()
                    .productoId(1L)
                    .productoNombre("Producto 1")
                    .categoriaNombre("Categoria 1")
                    .productoPrecio(producto.getProductoPrecio())
                    .stockActual(BigDecimal.valueOf(4))
                    .productoEstado(producto.getProductoEstado().name())
                    .build();

            when(productoRepository.buscarPorCategoriaYNombre(null, null))
                    .thenReturn(List.of(producto));
            when(recetaRepository.findByProductoIdFetchInsumo(producto.getProductoId()))
                    .thenReturn(List.of(receta));
            when(comandaItemRepository.sumCantidadInsumoComprometida(insumo.getInsumoId()))
                    .thenReturn(BigDecimal.valueOf(2.000));
            when(productoMapper.toInventarioResponse(producto, BigDecimal.valueOf(4)))
                    .thenReturn(response);

            List<ProductoInventarioResponse> result = productoService.listarProductosInventario(null, null);

            assertThat(result).containsExactly(response);
        }
    }

    // =========================================================================
    //  buscarProductos
    // =========================================================================

    @Nested
    @DisplayName("buscarProductos")
    class BuscarProductosTests {

        @Test
        @DisplayName("query null retorna lista vacia sin consultar repositorio")
        void queryNull_retornaListaVacia() {
            List<ProductoBusquedaResponse> result = productoService.buscarProductos(null);

            assertThat(result).isEmpty();
            verify(productoRepository, never()).buscarPorNombreSinMenu(any(), any());
        }

        @Test
        @DisplayName("query en blanco retorna lista vacia sin consultar repositorio")
        void queryBlank_retornaListaVacia() {
            List<ProductoBusquedaResponse> result = productoService.buscarProductos("   ");

            assertThat(result).isEmpty();
            verify(productoRepository, never()).buscarPorNombreSinMenu(any(), any());
        }

        @Test
        @DisplayName("query valida hace trim y delega al mapper por cada resultado")
        void queryValida_trimYDelegaAlMapper() {
            Producto p1 = mock(Producto.class);
            Producto p2 = mock(Producto.class);
            ProductoBusquedaResponse r1 = mock(ProductoBusquedaResponse.class);
            ProductoBusquedaResponse r2 = mock(ProductoBusquedaResponse.class);

            when(p1.getStockActual()).thenReturn(BigDecimal.valueOf(10));
            when(p1.getProductoTipo()).thenReturn(TipoProducto.VENTA_DIRECTA);
            when(p2.getStockActual()).thenReturn(BigDecimal.valueOf(8));
            when(p2.getProductoTipo()).thenReturn(TipoProducto.VENTA_DIRECTA);
            when(productoRepository.buscarPorNombreSinMenu("emp", EstadoGenerico.ACTIVO.name()))
                    .thenReturn(List.of(p1, p2));
            when(productoMapper.toBusquedaResponse(eq(p1), any(BigDecimal.class))).thenReturn(r1);
            when(productoMapper.toBusquedaResponse(eq(p2), any(BigDecimal.class))).thenReturn(r2);

            List<ProductoBusquedaResponse> result = productoService.buscarProductos("  emp  ");

            assertThat(result).containsExactly(r1, r2);
            verify(productoRepository).buscarPorNombreSinMenu("emp", EstadoGenerico.ACTIVO.name());
            verify(productoMapper, times(2)).toBusquedaResponse(any(Producto.class), any(BigDecimal.class));
        }

        @Test
        @DisplayName("query sin coincidencias retorna lista vacia")
        void querySinCoincidencias_retornaListaVacia() {
            when(productoRepository.buscarPorNombreSinMenu("xyz", EstadoGenerico.ACTIVO.name()))
                    .thenReturn(List.of());

            List<ProductoBusquedaResponse> result = productoService.buscarProductos("xyz");

            assertThat(result).isEmpty();
        }
    }
}
