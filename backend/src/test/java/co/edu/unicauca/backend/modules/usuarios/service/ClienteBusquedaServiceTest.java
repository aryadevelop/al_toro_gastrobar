package co.edu.unicauca.backend.modules.usuarios.service;

import co.edu.unicauca.backend.modules.usuarios.dto.response.ClienteBusquedaResponse;
import co.edu.unicauca.backend.modules.usuarios.entity.Cliente;
import co.edu.unicauca.backend.modules.usuarios.mapper.ClienteBusquedaMapper;
import co.edu.unicauca.backend.modules.usuarios.repository.ClienteRepository;
import co.edu.unicauca.backend.shared.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ClienteBusquedaService")
class ClienteBusquedaServiceTest {

    @Mock ClienteRepository clienteRepository;
    @Mock ClienteBusquedaMapper mapper;
    @InjectMocks ClienteBusquedaService service;

    @Test
    @DisplayName("normaliza el fragmento (trim + minúsculas) antes de consultar")
    void buscarPorEmail_normalizaEntrada() {
        when(clienteRepository.findByUsuario_UsuarioEmailContainingIgnoreCase("ana"))
                .thenReturn(List.of());

        service.buscarPorEmail("  ANA  ");

        verify(clienteRepository).findByUsuario_UsuarioEmailContainingIgnoreCase("ana");
    }

    @Test
    @DisplayName("sin coincidencias retorna lista vacía (no lanza)")
    void buscarPorEmail_sinCoincidencias_retornaVacia() {
        when(clienteRepository.findByUsuario_UsuarioEmailContainingIgnoreCase(anyString()))
                .thenReturn(List.of());

        assertThat(service.buscarPorEmail("zzz")).isEmpty();
    }

    @Test
    @DisplayName("con coincidencias mapea cada cliente")
    void buscarPorEmail_conCoincidencias_mapea() {
        Cliente c = Cliente.builder().usuarioId(1L).build();
        when(clienteRepository.findByUsuario_UsuarioEmailContainingIgnoreCase("ana")).thenReturn(List.of(c));
        when(mapper.toBusqueda(c)).thenReturn(ClienteBusquedaResponse.builder().clienteId(1L).build());

        List<ClienteBusquedaResponse> r = service.buscarPorEmail("ana");

        assertThat(r).hasSize(1);
        assertThat(r.get(0).getClienteId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("fragmento en blanco lanza VALIDATION_ERROR")
    void buscarPorEmail_blank_lanza() {
        assertThatThrownBy(() -> service.buscarPorEmail("   "))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("obligatorio");
    }

    @Test
    @DisplayName("fragmento null lanza VALIDATION_ERROR")
    void buscarPorEmail_null_lanza() {
        assertThatThrownBy(() -> service.buscarPorEmail(null))
                .isInstanceOf(BusinessException.class);
    }
}
