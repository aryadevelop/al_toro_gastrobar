package co.edu.unicauca.backend.modules.inventario.entity;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.assertThat;

class RecetaEntityTest {

    @Test
    void builder_creaInstanciaConCamposEsperados() {
        Receta r = Receta.builder()
                .insumoId(1L)
                .productoId(2L)
                .recetaCantidad(BigDecimal.valueOf(0.5))
                .build();

        assertThat(r.getInsumoId()).isEqualTo(1L);
        assertThat(r.getProductoId()).isEqualTo(2L);
        assertThat(r.getRecetaCantidad()).isEqualByComparingTo(BigDecimal.valueOf(0.5));
    }

    @Test
    void setter_actualizaCantidad() {
        Receta r = new Receta();
        r.setRecetaCantidad(BigDecimal.valueOf(1.25));
        assertThat(r.getRecetaCantidad()).isEqualByComparingTo(BigDecimal.valueOf(1.25));
    }

    @Test
    void equals_mismoInsumoYProducto_retornaTrue() {
        Receta a = Receta.builder().insumoId(1L).productoId(2L).build();
        Receta b = Receta.builder().insumoId(1L).productoId(2L).build();
        assertThat(a).isEqualTo(b);
    }

    @Test
    void equals_diferenteInsumo_retornaFalse() {
        Receta a = Receta.builder().insumoId(1L).productoId(2L).build();
        Receta b = Receta.builder().insumoId(9L).productoId(2L).build();
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void recetaId_builderYGetters() {
        Receta.RecetaId id = new Receta.RecetaId(1L, 2L);
        assertThat(id.getInsumoId()).isEqualTo(1L);
        assertThat(id.getProductoId()).isEqualTo(2L);
    }
}
