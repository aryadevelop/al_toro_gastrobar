package co.edu.unicauca.backend.modules.reservas.service;

import co.edu.unicauca.backend.modules.auth.entity.Usuario;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ComandaItemRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ComandaRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ZonaRepository;
import co.edu.unicauca.backend.modules.notificaciones.dto.ws.ReservaActualizadaWsMessage;
import co.edu.unicauca.backend.modules.notificaciones.service.NotificacionWsPublisher;
import co.edu.unicauca.backend.modules.pagos_caja.repository.AbonoRepository;
import co.edu.unicauca.backend.modules.reservas.dto.response.ConfirmarReservaResponse;
import co.edu.unicauca.backend.modules.reservas.entity.Reserva;
import co.edu.unicauca.backend.modules.reservas.mapper.ReservaMapper;
import co.edu.unicauca.backend.modules.reservas.repository.DecoracionRepository;
import co.edu.unicauca.backend.modules.reservas.repository.ReservaRepository;
import co.edu.unicauca.backend.modules.usuarios.entity.Cliente;
import co.edu.unicauca.backend.modules.usuarios.repository.ClienteRepository;
import co.edu.unicauca.backend.shared.enums.EstadoReserva;
import co.edu.unicauca.backend.shared.enums.TipoReserva;
import co.edu.unicauca.backend.shared.exception.BusinessException;
import co.edu.unicauca.backend.shared.exception.ErrorCode;
import co.edu.unicauca.backend.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias de {@link ReservaService#confirmarReserva}.
 *
 * <p>Cubre el cambio de estado de {@code PENDIENTE} a {@code CONFIRMADA} para reservas
 * especiales, los errores esperados (404 y 422 propagado desde el validador) y la emisión
 * del evento WebSocket de confirmación.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ReservaService — confirmarReserva")
class ReservaServiceConfirmarTest {

    @Mock ReservaRepository reservaRepository;
    @Mock DecoracionRepository decoracionRepository;
    @Mock ZonaRepository zonaRepository;
    @Mock ClienteRepository clienteRepository;
    @Mock ComandaRepository comandaRepository;
    @Mock ComandaItemRepository comandaItemRepository;
    @Mock ReservaMapper reservaMapper;
    @Mock AbonoRepository abonoRepository;
    @Mock ReservaValidador reservaValidador;
    @Mock DisponibilidadConsultador disponibilidadConsultador;
    @Mock PreOrdenGestor preOrdenGestor;
    @Mock MensajeWhatsAppBuilder mensajeWhatsAppBuilder;
    @Mock NotificacionWsPublisher wsPublisher;

    @InjectMocks
    ReservaService service;

    private static final Long RESERVA_ID = 1L;

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Reserva buildReserva(TipoReserva tipo, EstadoReserva estado) {
        Cliente cliente = new Cliente();
        setField(cliente, Cliente.class, "usuario", new Usuario());
        Reserva r = new Reserva();
        setField(r, Reserva.class, "reservaId", RESERVA_ID);
        setField(r, Reserva.class, "cliente", cliente);
        setField(r, Reserva.class, "reservaTipo", tipo);
        setField(r, Reserva.class, "reservaEstado", estado);
        setField(r, Reserva.class, "reservaFechaHoraLlegada", LocalDateTime.now().plusDays(2));
        setField(r, Reserva.class, "reservaNumeroPersonas", 2);
        return r;
    }

    private void setField(Object target, Class<?> clazz, String fieldName, Object value) {
        try {
            java.lang.reflect.Field f = clazz.getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(target, value);
        } catch (NoSuchFieldException e) {
            Class<?> parent = clazz.getSuperclass();
            if (parent != null && parent != Object.class) {
                setField(target, parent, fieldName, value);
            } else {
                throw new RuntimeException("Field not found: " + fieldName, e);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private ConfirmarReservaResponse stubResponse() {
        return ConfirmarReservaResponse.builder()
                .reservaId(RESERVA_ID).estado("CONFIRMADA").tipo("ESPECIAL").build();
    }

    // ── Casos ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("ESPECIAL PENDIENTE → guarda con estado CONFIRMADA")
    void especialPendiente_guardaConfirmada() {
        Reserva reserva = buildReserva(TipoReserva.ESPECIAL, EstadoReserva.PENDIENTE);
        when(reservaRepository.findById(RESERVA_ID)).thenReturn(Optional.of(reserva));
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(inv -> inv.getArgument(0));
        when(reservaMapper.toConfirmarResponse(any())).thenReturn(stubResponse());

        service.confirmarReserva(RESERVA_ID);

        ArgumentCaptor<Reserva> captor = ArgumentCaptor.forClass(Reserva.class);
        verify(reservaRepository).save(captor.capture());
        assertThat(captor.getValue().getReservaEstado()).isEqualTo(EstadoReserva.CONFIRMADA);
    }

    @Test
    @DisplayName("Reserva inexistente → ResourceNotFoundException")
    void reservaNoExiste_lanzaNotFound() {
        when(reservaRepository.findById(RESERVA_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.confirmarReserva(RESERVA_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Validador lanza INVALID_STATE → BusinessException se propaga")
    void estadoInvalido_propagaBusinessException() {
        Reserva reserva = buildReserva(TipoReserva.BASICA, EstadoReserva.PENDIENTE);
        when(reservaRepository.findById(RESERVA_ID)).thenReturn(Optional.of(reserva));
        doThrow(new BusinessException(ErrorCode.INVALID_STATE,
                "Solo se pueden confirmar reservas especiales en estado pendiente.",
                HttpStatus.UNPROCESSABLE_ENTITY))
                .when(reservaValidador).validarElegibilidadConfirmacion(reserva);

        assertThatThrownBy(() -> service.confirmarReserva(RESERVA_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus())
                        .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));
    }

    @Test
    @DisplayName("Confirmación exitosa publica evento WS con tipoEvento CONFIRMADA")
    void confirmarExitosa_publicaEventoConfirmada() {
        Reserva reserva = buildReserva(TipoReserva.ESPECIAL, EstadoReserva.PENDIENTE);
        when(reservaRepository.findById(RESERVA_ID)).thenReturn(Optional.of(reserva));
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(inv -> inv.getArgument(0));
        when(reservaMapper.toConfirmarResponse(any())).thenReturn(stubResponse());

        service.confirmarReserva(RESERVA_ID);

        ArgumentCaptor<ReservaActualizadaWsMessage> captor =
                ArgumentCaptor.forClass(ReservaActualizadaWsMessage.class);
        verify(wsPublisher).publicarReservaActualizada(captor.capture());
        assertThat(captor.getValue().getTipoEvento()).isEqualTo("CONFIRMADA");
        assertThat(captor.getValue().getReservaId()).isEqualTo(RESERVA_ID);
    }

    @Test
    @DisplayName("Estado inválido → no publica evento WS")
    void estadoInvalido_noPublicaWs() {
        Reserva reserva = buildReserva(TipoReserva.BASICA, EstadoReserva.PENDIENTE);
        when(reservaRepository.findById(RESERVA_ID)).thenReturn(Optional.of(reserva));
        doThrow(new BusinessException(ErrorCode.INVALID_STATE, "x", HttpStatus.UNPROCESSABLE_ENTITY))
                .when(reservaValidador).validarElegibilidadConfirmacion(reserva);

        assertThatThrownBy(() -> service.confirmarReserva(RESERVA_ID))
                .isInstanceOf(BusinessException.class);

        verify(wsPublisher, never()).publicarReservaActualizada(any());
    }
}
