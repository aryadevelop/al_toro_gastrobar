package co.edu.unicauca.backend.modules.mesas_comandas.entity;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ComandaMenuModificacionEntityTest {

    @Test
    void builder_creaInstanciaConId() {
        ComandaMenuModificacion c = ComandaMenuModificacion.builder()
                .id(1L)
                .build();

        assertThat(c.getId()).isEqualTo(1L);
    }

    @Test
    void setter_actualizaId() {
        ComandaMenuModificacion c = new ComandaMenuModificacion();
        c.setId(99L);
        assertThat(c.getId()).isEqualTo(99L);
    }
}
