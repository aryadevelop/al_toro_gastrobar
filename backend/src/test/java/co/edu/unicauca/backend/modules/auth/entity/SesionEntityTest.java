package co.edu.unicauca.backend.modules.auth.entity;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class SesionEntityTest {

    @Test
    void builder_creaInstanciaConCamposEsperados() {
        Sesion s = Sesion.builder()
                .sesionId(1L)
                .sesionToken("tok")
                .sesionRefreshToken("ref")
                .sesionActiva(true)
                .build();

        assertThat(s.getSesionId()).isEqualTo(1L);
        assertThat(s.getSesionToken()).isEqualTo("tok");
        assertThat(s.getSesionRefreshToken()).isEqualTo("ref");
        assertThat(s.getSesionActiva()).isTrue();
    }

    @Test
    void setter_actualizaToken() {
        Sesion s = new Sesion();
        s.setSesionToken("nuevo");
        assertThat(s.getSesionToken()).isEqualTo("nuevo");
    }

    @Test
    void equals_mismoId_retornaTrue() {
        Sesion a = Sesion.builder().sesionId(1L).build();
        Sesion b = Sesion.builder().sesionId(1L).build();
        assertThat(a).isEqualTo(b);
    }

    @Test
    void equals_diferenteId_retornaFalse() {
        Sesion a = Sesion.builder().sesionId(1L).build();
        Sesion b = Sesion.builder().sesionId(2L).build();
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void hashCode_esConsistente() {
        Sesion s = Sesion.builder().sesionId(1L).build();
        assertThat(s.hashCode()).isEqualTo(s.hashCode());
    }
}
