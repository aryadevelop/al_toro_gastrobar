package co.edu.unicauca.backend.modules.reservas.mapper;

import co.edu.unicauca.backend.modules.inventario.entity.OpcionModificacion;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.ComandaItem;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.ComandaMenuModificacion;
import co.edu.unicauca.backend.modules.produccion.entity.Producto;
import co.edu.unicauca.backend.modules.reservas.dto.response.PreOrdenItemResponse;
import co.edu.unicauca.backend.shared.enums.TipoComponenteMenu;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class PreOrdenMapperTest {

    private PreOrdenMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new PreOrdenMapper();
    }

    @Test
    @DisplayName("toDetalleResponse → mapea item sin modificaciones")
    void toDetalleResponse_sinModificaciones() {
        Producto p = Producto.builder()
                .productoId(1L)
                .productoNombre("Pollo al ajillo")
                .productoPrecio(BigDecimal.valueOf(28000))
                .build();
        ComandaItem item = ComandaItem.builder()
                .comandaItemId(1L)
                .producto(p)
                .comandaItemCantidad(2)
                .comandaItemPrecio(BigDecimal.valueOf(28000))
                .build();

        PreOrdenItemResponse resp = mapper.toDetalleResponse(item, List.of());

        assertThat(resp.getProductoId()).isEqualTo(1L);
        assertThat(resp.getProductoNombre()).isEqualTo("Pollo al ajillo");
        assertThat(resp.getCantidad()).isEqualTo(2);
        // mapper returns null for empty modifications list
        assertThat(resp.getModificaciones()).isNull();
    }

    @Test
    @DisplayName("toDetalleResponse → mapea modificaciones del item")
    void toDetalleResponse_conModificaciones() {
        Producto p = Producto.builder().productoId(2L).productoNombre("Menú especial").build();
        ComandaItem item = ComandaItem.builder()
                .comandaItemId(2L)
                .producto(p)
                .comandaItemCantidad(1)
                .comandaItemPrecio(BigDecimal.valueOf(35000))
                .build();
        OpcionModificacion opcion = OpcionModificacion.builder()
                .opcionId(10L)
                .opcionNombre("Arroz blanco")
                .tipoComponente(TipoComponenteMenu.ARROZ)
                .build();
        // The entity field is 'opcion', not 'opcionSeleccionada'
        ComandaMenuModificacion mod = ComandaMenuModificacion.builder()
                .opcion(opcion)
                .build();

        PreOrdenItemResponse resp = mapper.toDetalleResponse(item, List.of(mod));

        assertThat(resp.getModificaciones()).hasSize(1);
        assertThat(resp.getModificaciones().get(0).getOpcionId()).isEqualTo(10L);
        assertThat(resp.getModificaciones().get(0).getOpcionNombre()).isEqualTo("Arroz blanco");
    }
}
