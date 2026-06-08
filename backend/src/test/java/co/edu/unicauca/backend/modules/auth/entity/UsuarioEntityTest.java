package co.edu.unicauca.backend.modules.auth.entity;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class UsuarioEntityTest {

    @Test
    void builder_creaInstanciaConCamposEsperados() {
        Usuario u = Usuario.builder()
                .usuarioId(1L)
                .usuarioEmail("a@b.com")
                .usuarioPassword("hash")
                .build();

        assertThat(u.getUsuarioId()).isEqualTo(1L);
        assertThat(u.getUsuarioEmail()).isEqualTo("a@b.com");
        assertThat(u.getUsuarioPassword()).isEqualTo("hash");
    }

    @Test
    void setter_actualizaEmail() {
        Usuario u = new Usuario();
        u.setUsuarioEmail("nuevo@b.com");
        assertThat(u.getUsuarioEmail()).isEqualTo("nuevo@b.com");
    }

    @Test
    void equals_mismoId_retornaTrue() {
        Usuario a = Usuario.builder().usuarioId(5L).build();
        Usuario b = Usuario.builder().usuarioId(5L).build();
        assertThat(a).isEqualTo(b);
    }

    @Test
    void equals_diferenteId_retornaFalse() {
        Usuario a = Usuario.builder().usuarioId(1L).build();
        Usuario b = Usuario.builder().usuarioId(2L).build();
        assertThat(a).isNotEqualTo(b);
    }
}
