package co.edu.unicauca.backend.modules.reservas.entity;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class DecoracionZonaEntityTest {

    @Test
    void builder_creaInstanciaConCamposEsperados() {
        DecoracionZona dz = DecoracionZona.builder()
                .decoracionId(1L)
                .zonaId(2L)
                .build();

        assertThat(dz.getDecoracionId()).isEqualTo(1L);
        assertThat(dz.getZonaId()).isEqualTo(2L);
    }

    @Test
    void setter_actualizaZonaId() {
        DecoracionZona dz = new DecoracionZona();
        dz.setZonaId(5L);
        assertThat(dz.getZonaId()).isEqualTo(5L);
    }

    @Test
    void onCreate_setsCreatedAtSiEsNull() {
        DecoracionZona dz = new DecoracionZona();
        dz.onCreate();
        assertThat(dz.getCreatedAt()).isNotNull();
    }
}
