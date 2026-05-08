package co.edu.unicauca.backend.modules.reservas.mapper;

import co.edu.unicauca.backend.modules.inventario.entity.Producto;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Comanda;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.ComandaItem;
import co.edu.unicauca.backend.modules.reservas.dto.response.PreOrdenItemResponse;
import co.edu.unicauca.backend.shared.enums.CategoriaProducto;
import co.edu.unicauca.backend.shared.enums.EstacionComanda;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios de {@link PreOrdenMapper}.
 *
 * <p>Verifica que la fusión de pares COCINA+BARRA (menú especial) y los ítems
 * de carta (sin grupo) se convierten correctamente en {@link PreOrdenItemResponse}.
 */
class PreOrdenMapperTest {

    private PreOrdenMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new PreOrdenMapper();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Producto buildProducto(long id, String nombre, CategoriaProducto cat, BigDecimal precio) {
        return Producto.builder()
                .productoId(id)
                .productoNombre(nombre)
                .productoCategoria(cat)
                .productoPrecio(precio)
                .build();
    }

    private ComandaItem buildItem(long id, Comanda comanda, Producto producto,
                                  BigDecimal precio, String grupo) {
        return ComandaItem.builder()
                .comandaItemId(id)
                .comanda(comanda)
                .producto(producto)
                .comandaItemCantidad(1)
                .comandaItemPrecio(precio)
                .comandaItemMenuGrupo(grupo)
                .build();
    }

    // ========================================================================
    // toDetalleResponse
    // ========================================================================

    @Test
    @DisplayName("toDetalleResponse fusiona COCINA+BARRA con mismo grupo en un response con bebida")
    void toDetalleResponse_groupsMenuItems() {
        String grupo = "uuid-test-1";

        Comanda cocinaMock = mock(Comanda.class);
        when(cocinaMock.getComandaEstacion()).thenReturn(EstacionComanda.COCINA);

        Comanda barraMock = mock(Comanda.class);
        when(barraMock.getComandaEstacion()).thenReturn(EstacionComanda.BARRA);

        Producto menuProd = buildProducto(1L, "Menú 8b", CategoriaProducto.PLATO, BigDecimal.valueOf(35000));
        Producto jugoProd = buildProducto(2L, "Jugo de Lulo", CategoriaProducto.BEBIDA, BigDecimal.valueOf(8000));

        ComandaItem itemPlato = buildItem(10L, cocinaMock, menuProd, BigDecimal.valueOf(35000), grupo);
        ComandaItem itemBebida = buildItem(11L, barraMock, jugoProd, BigDecimal.ZERO, grupo);

        List<PreOrdenItemResponse> resp = mapper.toDetalleResponse(List.of(itemPlato, itemBebida));

        assertThat(resp).hasSize(1);
        PreOrdenItemResponse r = resp.get(0);
        assertThat(r.getProductoNombre()).isEqualTo("Menú 8b");
        assertThat(r.getPrecioUnitario()).isEqualByComparingTo("35000");
        // El campo bebida debe estar poblado con los datos de la bebida BARRA
        assertThat(r.getBebida()).isNotNull();
        assertThat(r.getBebida().productoNombre()).isEqualTo("Jugo de Lulo");
    }

    @Test
    @DisplayName("toDetalleResponse mantiene items sin grupo como entradas separadas")
    void toDetalleResponse_keepsCarteItemsSeparate() {
        Comanda cocinaMock = mock(Comanda.class);
        when(cocinaMock.getComandaEstacion()).thenReturn(EstacionComanda.COCINA);
        Comanda barraMock = mock(Comanda.class);
        when(barraMock.getComandaEstacion()).thenReturn(EstacionComanda.BARRA);

        Producto plato = buildProducto(1L, "Picanha", CategoriaProducto.PLATO, BigDecimal.valueOf(42000));
        Producto bebida = buildProducto(2L, "Coca-Cola", CategoriaProducto.BEBIDA, BigDecimal.valueOf(6000));

        // Sin grupo → ítems de carta
        ComandaItem itemPlato = buildItem(1L, cocinaMock, plato, BigDecimal.valueOf(42000), null);
        ComandaItem itemBebida = buildItem(2L, barraMock, bebida, BigDecimal.valueOf(6000), null);

        List<PreOrdenItemResponse> resp = mapper.toDetalleResponse(List.of(itemPlato, itemBebida));

        assertThat(resp).hasSize(2);
        // Los ítems de carta no deben tener el campo bebida
        assertThat(resp).allSatisfy(r -> assertThat(r.getBebida()).isNull());
    }

    @Test
    @DisplayName("toDetalleResponse con lista vacía → resultado vacío")
    void toDetalleResponse_listaVacia_retornaVacio() {
        List<PreOrdenItemResponse> resp = mapper.toDetalleResponse(List.of());

        assertThat(resp).isEmpty();
    }

    @Test
    @DisplayName("toDetalleResponse ordena ítems de carta por categoría: PLATO antes de BEBIDA")
    void toDetalleResponse_ordenaCarta_platoPrimero() {
        Comanda cocinaMock = mock(Comanda.class);
        when(cocinaMock.getComandaEstacion()).thenReturn(EstacionComanda.COCINA);
        Comanda barraMock = mock(Comanda.class);
        when(barraMock.getComandaEstacion()).thenReturn(EstacionComanda.BARRA);

        Producto bebida = buildProducto(1L, "Agua", CategoriaProducto.BEBIDA, BigDecimal.valueOf(3000));
        Producto plato = buildProducto(2L, "Lomo", CategoriaProducto.PLATO, BigDecimal.valueOf(45000));

        // La bebida se agrega primero en la lista; el mapper debe ordenar PLATO→BEBIDA
        ComandaItem itemBebida = buildItem(1L, barraMock, bebida, BigDecimal.valueOf(3000), null);
        ComandaItem itemPlato = buildItem(2L, cocinaMock, plato, BigDecimal.valueOf(45000), null);

        List<PreOrdenItemResponse> resp = mapper.toDetalleResponse(List.of(itemBebida, itemPlato));

        assertThat(resp).hasSize(2);
        assertThat(resp.get(0).getCategoriaProducto()).isEqualTo("PLATO");
        assertThat(resp.get(1).getCategoriaProducto()).isEqualTo("BEBIDA");
    }
}
