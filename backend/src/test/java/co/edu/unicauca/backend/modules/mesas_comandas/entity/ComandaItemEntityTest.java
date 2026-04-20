package co.edu.unicauca.backend.modules.mesas_comandas.entity;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.assertThat;

class ComandaItemEntityTest {

    @Test
    void builder_creaInstanciaConCamposEsperados() {
        ComandaItem item = ComandaItem.builder()
                .comandaItemId(1L)
                .comandaItemCantidad(2)
                .comandaItemPrecio(BigDecimal.valueOf(15000))
                .comandaItemDescripcion("Sin cebolla")
                .build();

        assertThat(item.getComandaItemId()).isEqualTo(1L);
        assertThat(item.getComandaItemCantidad()).isEqualTo(2);
        assertThat(item.getComandaItemPrecio()).isEqualByComparingTo(BigDecimal.valueOf(15000));
        assertThat(item.getComandaItemDescripcion()).isEqualTo("Sin cebolla");
    }

    @Test
    void setter_actualizaCantidad() {
        ComandaItem item = new ComandaItem();
        item.setComandaItemCantidad(3);
        assertThat(item.getComandaItemCantidad()).isEqualTo(3);
    }

    @Test
    void onCreate_setsCreatedAtSiEsNull() {
        ComandaItem item = new ComandaItem();
        item.onCreate();
        assertThat(item.getCreatedAt()).isNotNull();
    }
}
