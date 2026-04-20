package co.edu.unicauca.backend.modules.usuarios.entity;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.assertThat;

class ClienteEntityTest {

    @Test
    void builder_creaInstanciaConCamposEsperados() {
        Cliente c = Cliente.builder()
                .usuarioId(1L)
                .clienteNombre("Juan Perez")
                .clienteTelefono("3001234567")
                .clientePuntos(10)
                .clientePuntosAcumulados(50)
                .clienteAceptaTerminos(true)
                .clienteFechaAceptacion(LocalDateTime.now())
                .build();

        assertThat(c.getUsuarioId()).isEqualTo(1L);
        assertThat(c.getClienteNombre()).isEqualTo("Juan Perez");
        assertThat(c.getClienteTelefono()).isEqualTo("3001234567");
        assertThat(c.getClientePuntos()).isEqualTo(10);
        assertThat(c.getClientePuntosAcumulados()).isEqualTo(50);
        assertThat(c.getClienteAceptaTerminos()).isTrue();
    }

    @Test
    void setter_actualizaPuntos() {
        Cliente c = new Cliente();
        c.setClientePuntos(5);
        assertThat(c.getClientePuntos()).isEqualTo(5);
    }

    @Test
    void equals_mismoId_retornaTrue() {
        Cliente a = Cliente.builder().usuarioId(1L).build();
        Cliente b = Cliente.builder().usuarioId(1L).build();
        assertThat(a).isEqualTo(b);
    }

    @Test
    void equals_diferenteId_retornaFalse() {
        Cliente a = Cliente.builder().usuarioId(1L).build();
        Cliente b = Cliente.builder().usuarioId(2L).build();
        assertThat(a).isNotEqualTo(b);
    }
}
