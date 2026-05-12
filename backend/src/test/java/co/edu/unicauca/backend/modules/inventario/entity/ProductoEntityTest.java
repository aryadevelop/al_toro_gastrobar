package co.edu.unicauca.backend.modules.inventario.entity;

import co.edu.unicauca.backend.shared.enums.CategoriaProducto;
import co.edu.unicauca.backend.shared.enums.EstadoGenerico;
import co.edu.unicauca.backend.shared.enums.TipoProducto;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.assertThat;

class ProductoEntityTest {

    @Test
    void builder_creaInstanciaConCamposEsperados() {
        Producto p = Producto.builder()
                .productoId(1L)
                .productoNombre("Empanada")
                .productoEstado(EstadoGenerico.ACTIVO)
                .productoPrecio(BigDecimal.valueOf(5000))
                .productoTipo(TipoProducto.VENTA_DIRECTA)
                .productoCategoria(CategoriaProducto.PLATO)
                .menuEspecial(false)
                .build();

        assertThat(p.getProductoId()).isEqualTo(1L);
        assertThat(p.getProductoNombre()).isEqualTo("Empanada");
        assertThat(p.getProductoEstado()).isEqualTo(EstadoGenerico.ACTIVO);
        assertThat(p.getProductoPrecio()).isEqualByComparingTo(BigDecimal.valueOf(5000));
        assertThat(p.getMenuEspecial()).isFalse();
    }

    @Test
    void setter_actualizaPrecio() {
        Producto p = new Producto();
        p.setProductoPrecio(BigDecimal.valueOf(12000));
        assertThat(p.getProductoPrecio()).isEqualByComparingTo(BigDecimal.valueOf(12000));
    }

    @Test
    void equals_mismoId_retornaTrue() {
        Producto a = Producto.builder().productoId(1L).build();
        Producto b = Producto.builder().productoId(1L).build();
        assertThat(a).isEqualTo(b);
    }

    @Test
    void equals_diferenteId_retornaFalse() {
        Producto a = Producto.builder().productoId(1L).build();
        Producto b = Producto.builder().productoId(2L).build();
        assertThat(a).isNotEqualTo(b);
    }
}
