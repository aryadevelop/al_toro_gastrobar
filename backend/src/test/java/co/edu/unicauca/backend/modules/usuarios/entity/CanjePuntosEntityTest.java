package co.edu.unicauca.backend.modules.usuarios.entity;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class CanjePuntosEntityTest {

    @Test
    void builder_creaInstanciaConCamposEsperados() {
        CanjePuntos c = CanjePuntos.builder()
                .canjeId(1L)
                .canjePuntosCanjeados(5)
                .build();

        assertThat(c.getCanjeId()).isEqualTo(1L);
        assertThat(c.getCanjePuntosCanjeados()).isEqualTo(5);
    }

    @Test
    void setter_actualizaPuntos() {
        CanjePuntos c = new CanjePuntos();
        c.setCanjePuntosCanjeados(10);
        assertThat(c.getCanjePuntosCanjeados()).isEqualTo(10);
    }

    @Test
    void builderDefault_setsCanjeFechaHora() {
        CanjePuntos c = CanjePuntos.builder().canjeId(1L).build();
        assertThat(c.getCanjeFechaHora()).isNotNull();
    }
}
