package co.edu.unicauca.backend.modules.inventario.entity;

import co.edu.unicauca.backend.shared.enums.EstadoGenerico;
import co.edu.unicauca.backend.shared.enums.TipoInsumo;
import co.edu.unicauca.backend.shared.enums.UnidadMedida;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.assertThat;

class InsumoEntityTest {

    @Test
    void builder_creaInstanciaConCamposEsperados() {
        Insumo i = Insumo.builder()
                .insumoId(1L)
                .insumoNombre("Arroz")
                .insumoUnidad(UnidadMedida.KG)
                .insumoStockActual(BigDecimal.TEN)
                .insumoEstado(EstadoGenerico.ACTIVO)
                .tipoInsumo(TipoInsumo.MATERIA_PRIMA)
                .build();

        assertThat(i.getInsumoId()).isEqualTo(1L);
        assertThat(i.getInsumoNombre()).isEqualTo("Arroz");
        assertThat(i.getInsumoUnidad()).isEqualTo(UnidadMedida.KG);
        assertThat(i.getInsumoStockActual()).isEqualByComparingTo(BigDecimal.TEN);
        assertThat(i.getInsumoEstado()).isEqualTo(EstadoGenerico.ACTIVO);
    }

    @Test
    void setter_actualizaNombre() {
        Insumo i = new Insumo();
        i.setInsumoNombre("Sal");
        assertThat(i.getInsumoNombre()).isEqualTo("Sal");
    }

    @Test
    void equals_mismoId_retornaTrue() {
        Insumo a = Insumo.builder().insumoId(1L).build();
        Insumo b = Insumo.builder().insumoId(1L).build();
        assertThat(a).isEqualTo(b);
    }

    @Test
    void equals_diferenteId_retornaFalse() {
        Insumo a = Insumo.builder().insumoId(1L).build();
        Insumo b = Insumo.builder().insumoId(2L).build();
        assertThat(a).isNotEqualTo(b);
    }
}
