package co.edu.unicauca.backend.modules.mesas_comandas.service;

import co.edu.unicauca.backend.modules.auth.entity.Usuario;
import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.VisitaDetalleResponse;
import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.VisitaResumenResponse;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Comanda;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.ComandaItem;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Mesa;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Visita;
import co.edu.unicauca.backend.modules.mesas_comandas.mapper.VisitaMapper;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ComandaItemRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ComandaRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.MesaRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.VisitaRepository;
import co.edu.unicauca.backend.modules.pagos_caja.entity.Abono;
import co.edu.unicauca.backend.modules.pagos_caja.entity.Venta;
import co.edu.unicauca.backend.modules.pagos_caja.repository.AbonoRepository;
import co.edu.unicauca.backend.modules.pagos_caja.repository.VentaRepository;
import co.edu.unicauca.backend.modules.reservas.entity.Reserva;
import co.edu.unicauca.backend.modules.usuarios.entity.Cliente;
import co.edu.unicauca.backend.modules.usuarios.repository.ClienteRepository;
import co.edu.unicauca.backend.shared.exception.BusinessException;
import co.edu.unicauca.backend.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios de {@link VisitaService}.
 *
 * <p>Cubre los métodos:
 * <ul>
 *   <li>{@link VisitaService#obtenerHistorialVisitas(String)}</li>
 *   <li>{@link VisitaService#obtenerDetalleVisita(Long, String)}</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("VisitaService")
class VisitaServiceTest {

    @Mock ClienteRepository clienteRepository;
    @Mock VisitaRepository visitaRepository;
    @Mock MesaRepository mesaRepository;
    @Mock ComandaRepository comandaRepository;
    @Mock ComandaItemRepository comandaItemRepository;
    @Mock VentaRepository ventaRepository;
    @Mock AbonoRepository abonoRepository;
    @Mock VisitaMapper visitaMapper;

    @InjectMocks
    VisitaService visitaService;

    private static final String EMAIL_CLIENTE = "cliente@test.com";
    private static final Long CLIENTE_ID = 1L;
    private static final Long VISITA_ID = 10L;

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Cliente clienteConId(Long id) {
        return Cliente.builder()
                .usuarioId(id)
                .clienteNombre("Test Cliente")
                .clienteTelefono("3001234567")
                .build();
    }

    // =========================================================================
    //  obtenerHistorialVisitas
    // =========================================================================

    @Nested
    @DisplayName("obtenerHistorialVisitas")
    class ObtenerHistorialVisitasTests {

        @Test
        @DisplayName("cliente no existe lanza ResourceNotFoundException")
        void clienteNoExiste_lanzaResourceNotFoundException() {
            when(clienteRepository.findByUsuario_UsuarioEmail(EMAIL_CLIENTE))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> visitaService.obtenerHistorialVisitas(EMAIL_CLIENTE))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("retorna lista ordenada descendente con tamanio correcto")
        void retornaListaOrdenadaDescendente() {
            Cliente cliente = clienteConId(CLIENTE_ID);
            Visita v1 = mock(Visita.class);
            Visita v2 = mock(Visita.class);
            when(v1.getVisitaId()).thenReturn(1L);
            when(v2.getVisitaId()).thenReturn(2L);

            when(clienteRepository.findByUsuario_UsuarioEmail(EMAIL_CLIENTE))
                    .thenReturn(Optional.of(cliente));
            when(visitaRepository.findByCliente_UsuarioIdOrderByVisitaFechaHoraInicioDesc(CLIENTE_ID))
                    .thenReturn(List.of(v1, v2));
            when(mesaRepository.findByVisita_VisitaId(anyLong())).thenReturn(Optional.empty());
            when(ventaRepository.findByVisita_VisitaId(anyLong())).thenReturn(Optional.empty());
            when(visitaMapper.toResumen(any(), any(), any()))
                    .thenReturn(mock(VisitaResumenResponse.class));

            List<VisitaResumenResponse> result = visitaService.obtenerHistorialVisitas(EMAIL_CLIENTE);

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("cada visita consulta mesa y venta")
        void cadaVisita_consultaMesaYVenta() {
            Cliente cliente = clienteConId(CLIENTE_ID);
            Visita v1 = mock(Visita.class);
            Visita v2 = mock(Visita.class);
            when(v1.getVisitaId()).thenReturn(1L);
            when(v2.getVisitaId()).thenReturn(2L);

            when(clienteRepository.findByUsuario_UsuarioEmail(EMAIL_CLIENTE))
                    .thenReturn(Optional.of(cliente));
            when(visitaRepository.findByCliente_UsuarioIdOrderByVisitaFechaHoraInicioDesc(CLIENTE_ID))
                    .thenReturn(List.of(v1, v2));
            when(mesaRepository.findByVisita_VisitaId(anyLong())).thenReturn(Optional.empty());
            when(ventaRepository.findByVisita_VisitaId(anyLong())).thenReturn(Optional.empty());
            when(visitaMapper.toResumen(any(), any(), any()))
                    .thenReturn(mock(VisitaResumenResponse.class));

            visitaService.obtenerHistorialVisitas(EMAIL_CLIENTE);

            verify(mesaRepository, times(2)).findByVisita_VisitaId(anyLong());
            verify(ventaRepository, times(2)).findByVisita_VisitaId(anyLong());
        }

        @Test
        @DisplayName("visitas sin mesa ni venta llama toResumen con ambos Optional.empty()")
        void visitasSinMesaNiVenta_retornaResumenConOpcionalesVacios() {
            Cliente cliente = clienteConId(CLIENTE_ID);
            Visita v1 = mock(Visita.class);
            when(v1.getVisitaId()).thenReturn(1L);

            when(clienteRepository.findByUsuario_UsuarioEmail(EMAIL_CLIENTE))
                    .thenReturn(Optional.of(cliente));
            when(visitaRepository.findByCliente_UsuarioIdOrderByVisitaFechaHoraInicioDesc(CLIENTE_ID))
                    .thenReturn(List.of(v1));
            when(mesaRepository.findByVisita_VisitaId(anyLong())).thenReturn(Optional.empty());
            when(ventaRepository.findByVisita_VisitaId(anyLong())).thenReturn(Optional.empty());
            when(visitaMapper.toResumen(any(), any(), any()))
                    .thenReturn(mock(VisitaResumenResponse.class));

            visitaService.obtenerHistorialVisitas(EMAIL_CLIENTE);

            verify(visitaMapper).toResumen(eq(v1), eq(Optional.empty()), eq(Optional.empty()));
        }
    }

    // =========================================================================
    //  obtenerDetalleVisita
    // =========================================================================

    @Nested
    @DisplayName("obtenerDetalleVisita")
    class ObtenerDetalleVisitaTests {

        @Test
        @DisplayName("visita no existe lanza ResourceNotFoundException")
        void visitaNoExiste_lanzaResourceNotFoundException() {
            when(visitaRepository.findById(VISITA_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> visitaService.obtenerDetalleVisita(VISITA_ID, null))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("cliente accede a visita ajena lanza BusinessException 403")
        void clienteAccedeVisitaAjena_lanzaBusinessException_403() {
            Visita visita = mock(Visita.class);
            Cliente clienteMock = mock(Cliente.class);
            Usuario usuarioMock = mock(Usuario.class);

            when(visita.getCliente()).thenReturn(clienteMock);
            when(clienteMock.getUsuario()).thenReturn(usuarioMock);
            when(usuarioMock.getUsuarioEmail()).thenReturn("mio@test.com");

            when(visitaRepository.findById(VISITA_ID)).thenReturn(Optional.of(visita));

            assertThatThrownBy(() -> visitaService.obtenerDetalleVisita(VISITA_ID, "otro@test.com"))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getStatus())
                            .isEqualTo(HttpStatus.FORBIDDEN));
        }

        @Test
        @DisplayName("cliente nulo con email autenticado lanza BusinessException 403")
        void clienteNuloConEmailAutenticado_lanzaBusinessException_403() {
            Visita visita = mock(Visita.class);
            when(visita.getCliente()).thenReturn(null);

            when(visitaRepository.findById(VISITA_ID)).thenReturn(Optional.of(visita));

            assertThatThrownBy(() -> visitaService.obtenerDetalleVisita(VISITA_ID, "alguien@test.com"))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getStatus())
                            .isEqualTo(HttpStatus.FORBIDDEN));
        }

        @Test
        @DisplayName("admin (emailAutenticado null) accede visita de otro cliente y retorna detalle")
        void adminAccedeVisitaDeOtroCliente_retornaDetalle() {
            Visita visita = mock(Visita.class);
            when(visita.getVisitaId()).thenReturn(VISITA_ID);
            when(visita.getReserva()).thenReturn(null);

            VisitaDetalleResponse detalleMock = mock(VisitaDetalleResponse.class);

            when(visitaRepository.findById(VISITA_ID)).thenReturn(Optional.of(visita));
            when(comandaRepository.findByVisita_VisitaId(VISITA_ID)).thenReturn(List.of());
            when(ventaRepository.findByVisita_VisitaId(VISITA_ID)).thenReturn(Optional.empty());
            when(mesaRepository.findByVisita_VisitaId(VISITA_ID)).thenReturn(Optional.empty());
            when(visitaMapper.toDetalle(any(), any(), any(), any(), any())).thenReturn(detalleMock);

            VisitaDetalleResponse result = visitaService.obtenerDetalleVisita(VISITA_ID, null);

            assertThat(result).isEqualTo(detalleMock);
        }

        @Test
        @DisplayName("cliente propietario retorna detalle")
        void clientePropietario_retornaDetalle() {
            Visita visita = mock(Visita.class);
            Cliente clienteMock = mock(Cliente.class);
            Usuario usuarioMock = mock(Usuario.class);

            when(visita.getVisitaId()).thenReturn(VISITA_ID);
            when(visita.getCliente()).thenReturn(clienteMock);
            when(visita.getReserva()).thenReturn(null);
            when(clienteMock.getUsuario()).thenReturn(usuarioMock);
            when(usuarioMock.getUsuarioEmail()).thenReturn(EMAIL_CLIENTE);

            VisitaDetalleResponse detalleMock = mock(VisitaDetalleResponse.class);

            when(visitaRepository.findById(VISITA_ID)).thenReturn(Optional.of(visita));
            when(comandaRepository.findByVisita_VisitaId(VISITA_ID)).thenReturn(List.of());
            when(ventaRepository.findByVisita_VisitaId(VISITA_ID)).thenReturn(Optional.empty());
            when(mesaRepository.findByVisita_VisitaId(VISITA_ID)).thenReturn(Optional.empty());
            when(visitaMapper.toDetalle(any(), any(), any(), any(), any())).thenReturn(detalleMock);

            VisitaDetalleResponse result = visitaService.obtenerDetalleVisita(VISITA_ID, EMAIL_CLIENTE);

            assertThat(result).isEqualTo(detalleMock);
        }

        @Test
        @DisplayName("visita con reserva consulta abonos y llama toDetalle con Optional.of(abonos)")
        void visitaConReserva_consultaAbonos() {
            Reserva reserva = mock(Reserva.class);
            when(reserva.getReservaId()).thenReturn(5L);

            Visita visita = mock(Visita.class);
            when(visita.getVisitaId()).thenReturn(VISITA_ID);
            when(visita.getReserva()).thenReturn(reserva);
            when(visita.getCliente()).thenReturn(null);

            Abono abono = mock(Abono.class);
            List<Abono> abonos = List.of(abono);

            when(visitaRepository.findById(VISITA_ID)).thenReturn(Optional.of(visita));
            when(comandaRepository.findByVisita_VisitaId(VISITA_ID)).thenReturn(List.of());
            when(ventaRepository.findByVisita_VisitaId(VISITA_ID)).thenReturn(Optional.empty());
            when(mesaRepository.findByVisita_VisitaId(VISITA_ID)).thenReturn(Optional.empty());
            when(abonoRepository.findByReserva_ReservaIdOrderByAbonoFechaHoraAsc(5L)).thenReturn(abonos);
            when(visitaMapper.toDetalle(any(), any(), any(), any(), any()))
                    .thenReturn(mock(VisitaDetalleResponse.class));

            visitaService.obtenerDetalleVisita(VISITA_ID, null);

            verify(abonoRepository).findByReserva_ReservaIdOrderByAbonoFechaHoraAsc(5L);
            verify(visitaMapper).toDetalle(
                    eq(visita),
                    any(),
                    eq(Optional.of(abonos)),
                    any(),
                    any()
            );
        }

        @Test
        @DisplayName("visita sin reserva no consulta abonos y llama toDetalle con Optional.empty()")
        void visitaSinReserva_noConsultaAbonos() {
            Visita visita = mock(Visita.class);
            when(visita.getVisitaId()).thenReturn(VISITA_ID);
            when(visita.getReserva()).thenReturn(null);
            when(visita.getCliente()).thenReturn(null);

            when(visitaRepository.findById(VISITA_ID)).thenReturn(Optional.of(visita));
            when(comandaRepository.findByVisita_VisitaId(VISITA_ID)).thenReturn(List.of());
            when(ventaRepository.findByVisita_VisitaId(VISITA_ID)).thenReturn(Optional.empty());
            when(mesaRepository.findByVisita_VisitaId(VISITA_ID)).thenReturn(Optional.empty());
            when(visitaMapper.toDetalle(any(), any(), any(), any(), any()))
                    .thenReturn(mock(VisitaDetalleResponse.class));

            visitaService.obtenerDetalleVisita(VISITA_ID, null);

            verify(abonoRepository, never()).findByReserva_ReservaIdOrderByAbonoFechaHoraAsc(anyLong());
            verify(visitaMapper).toDetalle(
                    eq(visita),
                    any(),
                    eq(Optional.empty()),
                    any(),
                    any()
            );
        }

        @Test
        @DisplayName("consolida todos los items de todas las comandas")
        void consolidaTodosLosItemsDeTodosLasComandas() {
            Visita visita = mock(Visita.class);
            when(visita.getVisitaId()).thenReturn(VISITA_ID);
            when(visita.getReserva()).thenReturn(null);
            when(visita.getCliente()).thenReturn(null);

            Comanda comanda1 = mock(Comanda.class);
            Comanda comanda2 = mock(Comanda.class);
            when(comanda1.getComandaId()).thenReturn(100L);
            when(comanda2.getComandaId()).thenReturn(200L);

            ComandaItem item1 = mock(ComandaItem.class);
            ComandaItem item2 = mock(ComandaItem.class);

            when(visitaRepository.findById(VISITA_ID)).thenReturn(Optional.of(visita));
            when(comandaRepository.findByVisita_VisitaId(VISITA_ID))
                    .thenReturn(List.of(comanda1, comanda2));
            when(comandaItemRepository.findByComanda_ComandaId(100L)).thenReturn(List.of(item1));
            when(comandaItemRepository.findByComanda_ComandaId(200L)).thenReturn(List.of(item2));
            when(ventaRepository.findByVisita_VisitaId(VISITA_ID)).thenReturn(Optional.empty());
            when(mesaRepository.findByVisita_VisitaId(VISITA_ID)).thenReturn(Optional.empty());
            when(visitaMapper.toDetalle(any(), any(), any(), any(), any()))
                    .thenReturn(mock(VisitaDetalleResponse.class));

            visitaService.obtenerDetalleVisita(VISITA_ID, null);

            // Verify mapper received a list with both items
            verify(visitaMapper).toDetalle(
                    eq(visita),
                    argThat(items -> ((List<?>) items).size() == 2),
                    any(),
                    any(),
                    any()
            );
        }
    }
}
