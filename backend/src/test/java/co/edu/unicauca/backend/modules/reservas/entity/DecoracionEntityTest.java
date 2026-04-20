package co.edu.unicauca.backend.modules.reservas.entity;

import co.edu.unicauca.backend.shared.enums.EstadoGenerico;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.assertThat;

class DecoracionEntityTest {

    @Test
    void builder_creaInstanciaConCamposEsperados() {
        Decoracion d = Decoracion.builder()
                .decoracionId(1L)
                .decoracionNombre("Globos")
                .decoracionEstado(EstadoGenerico.ACTIVO)
                .decoracionCostoAdicional(BigDecimal.valueOf(30000))
                .build();

        assertThat(d.getDecoracionId()).isEqualTo(1L);
        assertThat(d.getDecoracionNombre()).isEqualTo("Globos");
        assertThat(d.getDecoracionEstado()).isEqualTo(EstadoGenerico.ACTIVO);
        assertThat(d.getDecoracionCostoAdicional()).isEqualByComparingTo(BigDecimal.valueOf(30000));
    }

    @Test
    void setter_actualizaNombre() {
        Decoracion d = new Decoracion();
        d.setDecoracionNombre("Flores");
        assertThat(d.getDecoracionNombre()).isEqualTo("Flores");
    }

    @Test
    void equals_mismoId_retornaTrue() {
        Decoracion a = Decoracion.builder().decoracionId(1L).build();
        Decoracion b = Decoracion.builder().decoracionId(1L).build();
        assertThat(a).isEqualTo(b);
    }

    @Test
    void equals_diferenteId_retornaFalse() {
        Decoracion a = Decoracion.builder().decoracionId(1L).build();
        Decoracion b = Decoracion.builder().decoracionId(2L).build();
        assertThat(a).isNotEqualTo(b);
    }
}
