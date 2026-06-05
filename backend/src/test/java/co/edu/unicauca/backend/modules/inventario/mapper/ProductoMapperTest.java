package co.edu.unicauca.backend.modules.inventario.mapper;

import co.edu.unicauca.backend.modules.inventario.dto.response.CategoriaCartaResponse;
import co.edu.unicauca.backend.modules.inventario.dto.response.MenuEspecialResponse;
import co.edu.unicauca.backend.modules.inventario.dto.response.ProductoBebidaResponse;
import co.edu.unicauca.backend.modules.inventario.dto.response.ProductoBusquedaResponse;
import co.edu.unicauca.backend.modules.inventario.dto.response.ProductoCartaResponse;
import co.edu.unicauca.backend.modules.inventario.entity.CategoriaCarta;
import co.edu.unicauca.backend.modules.inventario.entity.OpcionModificacion;
import co.edu.unicauca.backend.modules.inventario.entity.Producto;
import co.edu.unicauca.backend.shared.enums.CategoriaProducto;
import co.edu.unicauca.backend.shared.enums.EstadoGenerico;
import co.edu.unicauca.backend.shared.enums.TipoComponenteMenu;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class ProductoMapperTest {

    private ProductoMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ProductoMapper();
    }

    private Producto productoBase() {
        return Producto.builder()
                .productoId(1L)
                .productoNombre("Empanada")
                .productoPrecio(BigDecimal.valueOf(5000))
                .productoCategoria(CategoriaProducto.PLATO)
                .productoEstado(EstadoGenerico.ACTIVO)
                .productoDescripcion("Empanada de pipián")
                .build();
    }

    @Test
    @DisplayName("toCategoriaCartaResponse → mapea categoría y productos")
    void toCategoriaCartaResponse_mapeaCorrectamente() {
        CategoriaCarta cat = CategoriaCarta.builder()
                .categoriacartaId(1)
                .categoriaNombre("Entradas")
                .orden(1)
                .build();
        List<Producto> productos = List.of(productoBase());

        CategoriaCartaResponse resp = mapper.toCategoriaCartaResponse(cat, productos);

        assertThat(resp.getCategoriaId()).isEqualTo(1);
        assertThat(resp.getCategoriaNombre()).isEqualTo("Entradas");
        assertThat(resp.getOrden()).isEqualTo(1);
        assertThat(resp.getProductos()).hasSize(1);
        assertThat(resp.getProductos().get(0).getProductoNombre()).isEqualTo("Empanada");
    }

    @Test
    @DisplayName("toCategoriaCartaResponse → lista vacía de productos")
    void toCategoriaCartaResponse_sinProductos_retornaListaVacia() {
        CategoriaCarta cat = CategoriaCarta.builder().categoriacartaId(2).categoriaNombre("Bebidas").build();

        CategoriaCartaResponse resp = mapper.toCategoriaCartaResponse(cat, List.of());

        assertThat(resp.getProductos()).isEmpty();
    }

    @Test
    @DisplayName("toProductoCartaResponse → mapea campos del producto")
    void toProductoCartaResponse_mapeaCampos() {
        ProductoCartaResponse resp = mapper.toProductoCartaResponse(productoBase());

        assertThat(resp.getProductoId()).isEqualTo(1L);
        assertThat(resp.getProductoNombre()).isEqualTo("Empanada");
        assertThat(resp.getProductoPrecio()).isEqualByComparingTo(BigDecimal.valueOf(5000));
        assertThat(resp.getProductoCategoria()).isEqualTo(CategoriaProducto.PLATO.name());
    }

    @Test
    @DisplayName("toMenuEspecialResponse → agrupa opciones por TipoComponenteMenu")
    void toMenuEspecialResponse_agrupaPorTipoComponente() {
        Producto menu = Producto.builder()
                .productoId(2L)
                .productoNombre("Menú Especial")
                .productoPrecio(BigDecimal.valueOf(35000))
                .productoCategoria(CategoriaProducto.PLATO)
                .build();
        List<OpcionModificacion> opciones = List.of(
                OpcionModificacion.builder().opcionId(1L).opcionNombre("Pollo").tipoComponente(TipoComponenteMenu.SALSA_PROTEINA_1).opcionEstado(EstadoGenerico.ACTIVO).build(),
                OpcionModificacion.builder().opcionId(2L).opcionNombre("Blanco").tipoComponente(TipoComponenteMenu.ARROZ).opcionEstado(EstadoGenerico.ACTIVO).build()
        );

        MenuEspecialResponse resp = mapper.toMenuEspecialResponse(menu, opciones, List.of());

        assertThat(resp.getProductoId()).isEqualTo(2L);
        assertThat(resp.getModificacionesPorComponente()).hasSize(2);
    }

    @Test
    @DisplayName("toMenuEspecialResponse → sin opciones retorna lista vacía de grupos")
    void toMenuEspecialResponse_sinOpciones_retornaGruposVacios() {
        MenuEspecialResponse resp = mapper.toMenuEspecialResponse(productoBase(), List.of(), List.of());

        assertThat(resp.getModificacionesPorComponente()).isEmpty();
    }

    @Test
    @DisplayName("toMenuEspecialResponse incluye bebidasDisponibles cuando se proveen")
    void toMenuEspecialResponse_includesBebidas() {
        // Create mock Producto for menu
        Producto menu = new Producto();
        menu.setProductoId(10L);
        menu.setProductoNombre("Menú 8b");
        menu.setProductoDescripcion("Descripción");
        menu.setProductoPrecio(new java.math.BigDecimal("35000"));

        // Create mock Producto for bebidas
        Producto jugo1 = new Producto();
        jugo1.setProductoId(101L);
        jugo1.setProductoNombre("Jugo de Maracuyá");
        jugo1.setProductoPrecio(new java.math.BigDecimal("8000"));

        Producto jugo2 = new Producto();
        jugo2.setProductoId(102L);
        jugo2.setProductoNombre("Jugo de Lulo");
        jugo2.setProductoPrecio(new java.math.BigDecimal("8000"));

        MenuEspecialResponse resp = mapper.toMenuEspecialResponse(menu, List.of(), List.of(jugo1, jugo2));

        assertThat(resp.getBebidasDisponibles()).hasSize(2)
            .extracting(ProductoBebidaResponse::productoNombre)
            .containsExactlyInAnyOrder("Jugo de Maracuyá", "Jugo de Lulo");
    }

    @Test
    @DisplayName("toBusquedaResponse → mapea id, nombre, precio, stock, estado y categoría")
    void toBusquedaResponse_mapeaCampos() {
        ProductoBusquedaResponse resp = mapper.toBusquedaResponse(productoBase(), BigDecimal.valueOf(3));

        assertThat(resp.getProductoId()).isEqualTo(1L);
        assertThat(resp.getProductoNombre()).isEqualTo("Empanada");
        assertThat(resp.getProductoPrecio()).isEqualByComparingTo(BigDecimal.valueOf(5000));
        assertThat(resp.getStockActual()).isEqualByComparingTo(BigDecimal.valueOf(3));
        assertThat(resp.getProductoEstado()).isEqualTo("ACTIVO");
        assertThat(resp.getProductoCategoria()).isEqualTo(CategoriaProducto.PLATO.name());
    }
}
