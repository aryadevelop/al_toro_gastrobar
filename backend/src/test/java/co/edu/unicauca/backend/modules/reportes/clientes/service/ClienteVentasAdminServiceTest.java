package co.edu.unicauca.backend.modules.reportes.clientes.service;

import co.edu.unicauca.backend.modules.auth.repository.UsuarioRolRepository;
import co.edu.unicauca.backend.modules.reportes.clientes.dto.response.ClienteListadoResponse;
import co.edu.unicauca.backend.modules.reportes.clientes.repository.ClienteAdminRepository;
import co.edu.unicauca.backend.modules.reportes.clientes.repository.VentaAdminRepository;
import co.edu.unicauca.backend.modules.reservas.entity.Reserva;
import co.edu.unicauca.backend.modules.reservas.repository.ReservaRepository;
import co.edu.unicauca.backend.modules.usuarios.entity.Cliente;
import co.edu.unicauca.backend.modules.auth.entity.Usuario;
import co.edu.unicauca.backend.modules.usuarios.entity.UsuarioRol;
import co.edu.unicauca.backend.shared.enums.RolEstado;
import co.edu.unicauca.backend.shared.enums.RolNombre;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClienteVentasAdminServiceTest {

    @Mock
    private ClienteAdminRepository clienteRepository;

    @Mock
    private VentaAdminRepository ventaRepository;

    @Mock
    private ReservaRepository reservaRepository;

    @Mock
    private UsuarioRolRepository usuarioRolRepository;

    @Mock
    private ClienteRecordatorioService recordatorioService;

    @InjectMocks
    private ClienteVentasAdminService service;

    @Test
    void listarClientes_excludeClientsWithRecentReservations() {
        Usuario usuario1 = Usuario.builder()
                .usuarioId(1L)
                .usuarioEmail("cliente1@altoro.com")
                .usuarioPassword("password")
                .build();
        Usuario usuario2 = Usuario.builder()
                .usuarioId(2L)
                .usuarioEmail("cliente2@altoro.com")
                .usuarioPassword("password")
                .build();

        Cliente cliente1 = Cliente.builder()
                .usuarioId(1L)
                .usuario(usuario1)
                .clienteNombre("Cliente Uno")
                .clienteTelefono("3001111111")
                .clientePuntosAcumulados(10)
                .clienteAceptaTerminos(true)
                .clienteFechaAceptacion(LocalDateTime.now().minusDays(10))
                .build();
        cliente1.setCreatedAt(LocalDateTime.now().minusMonths(5));

        Cliente cliente2 = Cliente.builder()
                .usuarioId(2L)
                .usuario(usuario2)
                .clienteNombre("Cliente Dos")
                .clienteTelefono("3002222222")
                .clientePuntosAcumulados(5)
                .clienteAceptaTerminos(true)
                .clienteFechaAceptacion(LocalDateTime.now().minusDays(20))
                .build();
        cliente2.setCreatedAt(LocalDateTime.now().minusMonths(5));

        when(clienteRepository.findAll()).thenReturn(List.of(cliente1, cliente2));
        when(reservaRepository.findByCliente_UsuarioIdInAndReservaFechaCreacionAfter(anyList(), any(LocalDateTime.class)))
                .thenReturn(List.of(Reserva.builder()
                        .reservaId(10L)
                        .cliente(cliente1)
                        .reservaFechaHoraLlegada(LocalDateTime.now().plusDays(1))
                        .reservaNumeroPersonas(2)
                        .reservaEstado(null)
                        .reservaTipo(null)
                        .reservaFechaCreacion(LocalDateTime.now().minusDays(15))
                        .build()));
        when(usuarioRolRepository.findByUsuarioIdIn(anyList()))
                .thenReturn(List.of(UsuarioRol.builder()
                        .usuarioId(2L)
                        .rolNombre(RolNombre.CLIENTE)
                        .rolEstado(RolEstado.ACTIVO)
                        .build()));
        when(ventaRepository.findByVisita_Cliente_UsuarioIdIn(anyList()))
                .thenReturn(List.of());

        List<ClienteListadoResponse> resultados = service.listarClientes(null, null, null, null, null, null, null, null, 2);

        assertThat(resultados).hasSize(1);
        assertThat(resultados.get(0).getClienteId()).isEqualTo(2L);
    }
}
