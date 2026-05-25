package co.edu.unicauca.backend.modules.usuarios.mapper;

import co.edu.unicauca.backend.modules.auth.entity.Usuario;
import co.edu.unicauca.backend.modules.usuarios.dto.response.ClienteBusquedaResponse;
import co.edu.unicauca.backend.modules.usuarios.entity.Cliente;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ClienteBusquedaMapper")
class ClienteBusquedaMapperTest {

    private final ClienteBusquedaMapper mapper = new ClienteBusquedaMapper();

    @Test
    @DisplayName("mapea cliente a ClienteBusquedaResponse")
    void toBusqueda_clienteCompleto_mapeaCampos() {
        Usuario u = Usuario.builder().usuarioEmail("ana@mail.com").build();
        Cliente c = Cliente.builder()
                .usuarioId(7L).usuario(u).clienteNombre("Ana")
                .clientePuntosAcumulados(12).build();

        ClienteBusquedaResponse r = mapper.toBusqueda(c);

        assertThat(r.getClienteId()).isEqualTo(7L);
        assertThat(r.getNombre()).isEqualTo("Ana");
        assertThat(r.getEmail()).isEqualTo("ana@mail.com");
        assertThat(r.getPuntosAcumulados()).isEqualTo(12);
    }
}
