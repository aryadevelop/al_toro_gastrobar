package co.edu.unicauca.backend.modules.mesas_comandas.entity;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ZonaEntityTest {

    @Test
    void builder_creaInstanciaConCamposEsperados() {
        Zona z = Zona.builder()
                .zonaId(1L)
                .zonaNombre("Terraza")
                .zonaCapacidadPersonas(20)
                .zonaImagenUrl("https://img.com/z.jpg")
                .build();

        assertThat(z.getZonaId()).isEqualTo(1L);
        assertThat(z.getZonaNombre()).isEqualTo("Terraza");
        assertThat(z.getZonaCapacidadPersonas()).isEqualTo(20);
        assertThat(z.getZonaImagenUrl()).isEqualTo("https://img.com/z.jpg");
    }

    @Test
    void setter_actualizaNombre() {
        Zona z = new Zona();
        z.setZonaNombre("Interior");
        assertThat(z.getZonaNombre()).isEqualTo("Interior");
    }

    @Test
    void equals_mismoId_retornaTrue() {
        Zona a = Zona.builder().zonaId(1L).build();
        Zona b = Zona.builder().zonaId(1L).build();
        assertThat(a).isEqualTo(b);
    }

    @Test
    void equals_diferenteId_retornaFalse() {
        Zona a = Zona.builder().zonaId(1L).build();
        Zona b = Zona.builder().zonaId(2L).build();
        assertThat(a).isNotEqualTo(b);
    }
}
