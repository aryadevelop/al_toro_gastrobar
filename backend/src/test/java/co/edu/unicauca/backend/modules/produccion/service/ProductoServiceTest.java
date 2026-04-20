package co.edu.unicauca.backend.modules.produccion.service;

import co.edu.unicauca.backend.modules.inventario.entity.OpcionModificacion;
import co.edu.unicauca.backend.modules.inventario.repository.ProductoOpcionModificacionRepository;
import co.edu.unicauca.backend.modules.produccion.dto.response.CategoriaCartaResponse;
import co.edu.unicauca.backend.modules.produccion.dto.response.MenuEspecialResponse;
import co.edu.unicauca.backend.modules.produccion.entity.CategoriaCarta;
import co.edu.unicauca.backend.modules.produccion.entity.Producto;
import co.edu.unicauca.backend.modules.produccion.mapper.ProductoMapper;
import co.edu.unicauca.backend.modules.produccion.repository.ProductoRepository;
import co.edu.unicauca.backend.shared.enums.EstadoGenerico;
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
    @Mock ProductoOpcionModificacionRepository productoOpcionModificacionRepository;
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
            when(productoMapper.toMenuEspecialResponse(any(), any()))
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
            when(productoMapper.toMenuEspecialResponse(any(), any()))
                    .thenReturn(mock(MenuEspecialResponse.class));

            productoService.obtenerMenusEspeciales();

            verify(productoMapper, times(2)).toMenuEspecialResponse(any(), any());
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
}
