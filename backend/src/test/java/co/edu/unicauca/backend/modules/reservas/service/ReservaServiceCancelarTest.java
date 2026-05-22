package co.edu.unicauca.backend.modules.reservas.service;

import co.edu.unicauca.backend.modules.auth.entity.Usuario;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ComandaItemRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ComandaRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ZonaRepository;
import co.edu.unicauca.backend.modules.notificaciones.dto.ws.ReservaActualizadaWsMessage;
import co.edu.unicauca.backend.modules.notificaciones.service.NotificacionWsPublisher;
import co.edu.unicauca.backend.modules.pagos_caja.entity.Abono;
import co.edu.unicauca.backend.modules.pagos_caja.repository.AbonoRepository;
import co.edu.unicauca.backend.modules.reservas.dto.response.CancelarReservaResponse;
import co.edu.unicauca.backend.modules.reservas.entity.Reserva;
import co.edu.unicauca.backend.modules.reservas.mapper.ReservaMapper;
import co.edu.unicauca.backend.modules.reservas.repository.DecoracionRepository;
import co.edu.unicauca.backend.modules.reservas.repository.ReservaRepository;
import co.edu.unicauca.backend.modules.usuarios.entity.Cliente;
import co.edu.unicauca.backend.modules.usuarios.repository.ClienteRepository;
import co.edu.unicauca.backend.shared.enums.EstadoReserva;
import co.edu.unicauca.backend.shared.enums.TipoAbono;
import co.edu.unicauca.backend.shared.enums.TipoReserva;
import co.edu.unicauca.backend.shared.exception.BusinessException;
import co.edu.unicauca.backend.shared.exception.ErrorCode;
import co.edu.unicauca.backend.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias de {@link ReservaService#cancelarReserva}.
 *
 * <p>Cubre los cuatro criterios de aceptación de la HU-05:
 * <ul>
 *   <li>CA-01: BASICA sin abono → no WhatsApp.</li>
 *   <li>CA-02: BASICA con abono → WhatsApp para reembolso.</li>
 *   <li>CA-03: ESPECIAL antes 16:00 del día de la reserva → WhatsApp para reembolso.</li>
 *   <li>CA-04: ESPECIAL después 16:00 del día de la reserva → no WhatsApp.</li>
 * </ul>
 * Además cubre los errores esperados: 404, 403 (ownership) y 422 (estado no cancelable).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ReservaService — cancelarReserva")
class ReservaServiceCancelarTest {

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

    private static final String EMAIL = "cliente@test.com";
    private static final Long RESERVA_ID = 1L;

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Reserva buildReserva(TipoReserva tipo, EstadoReserva estado, LocalDateTime fechaHoraLlegada) {
        Cliente cliente = buildCliente(EMAIL);
        Reserva r = new Reserva();
        setField(r, Reserva.class, "reservaId", RESERVA_ID);
        setField(r, Reserva.class, "cliente", cliente);
        setField(r, Reserva.class, "reservaTipo", tipo);
        setField(r, Reserva.class, "reservaEstado", estado);
        setField(r, Reserva.class, "reservaFechaHoraLlegada", fechaHoraLlegada);
        setField(r, Reserva.class, "reservaNumeroPersonas", 2);
        return r;
    }

    private Cliente buildCliente(String email) {
        Usuario usuario = new Usuario();
        setField(usuario, Usuario.class, "usuarioEmail", email);
        Cliente cliente = new Cliente();
        try {
            java.lang.reflect.Field f = Cliente.class.getSuperclass().getDeclaredField("usuario");
            f.setAccessible(true);
            f.set(cliente, usuario);
        } catch (Exception e1) {
            try {
                java.lang.reflect.Field f = Cliente.class.getDeclaredField("usuario");
                f.setAccessible(true);
                f.set(cliente, usuario);
            } catch (Exception e2) {
                throw new RuntimeException(e2);
            }
        }
        return cliente;
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

    private Abono buildAbono(BigDecimal monto) {
        Abono a = new Abono();
        setField(a, Abono.class, "abonoMonto", monto);
        setField(a, Abono.class, "abonoTipo", TipoAbono.ANTICIPO);
        return a;
    }

    private CancelarReservaResponse stubResponse(boolean requiereWa) {
        return CancelarReservaResponse.builder()
                .reservaId(RESERVA_ID).estado("CANCELADA").tipo("BASICA")
                .requiereWhatsApp(requiereWa).build();
    }

    // ── Pruebas de error ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("Errores previos a la cancelación")
    class Errores {

        @Test
        @DisplayName("Reserva inexistente → ResourceNotFoundException")
        void reservaNoExiste_lanzaNotFound() {
            when(reservaRepository.findById(RESERVA_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.cancelarReserva(RESERVA_ID, EMAIL))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Validador lanza ACCESS_DENIED → BusinessException se propaga")
        void ownershipFalla_propagaBusinessException() {
            Reserva reserva = buildReserva(TipoReserva.BASICA, EstadoReserva.PENDIENTE,
                    LocalDateTime.now().plusDays(2));
            when(reservaRepository.findById(RESERVA_ID)).thenReturn(Optional.of(reserva));
            doThrow(new BusinessException(ErrorCode.ACCESS_DENIED,
                    "Solo puedes cancelar tus propias reservas.", HttpStatus.FORBIDDEN))
                    .when(reservaValidador).validarElegibilidadCancelacion(reserva, EMAIL);

            assertThatThrownBy(() -> service.cancelarReserva(RESERVA_ID, EMAIL))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("Validador lanza INVALID_STATE → BusinessException se propaga")
        void estadoNoActivo_propagaBusinessException() {
            Reserva reserva = buildReserva(TipoReserva.BASICA, EstadoReserva.CANCELADA,
                    LocalDateTime.now().plusDays(2));
            when(reservaRepository.findById(RESERVA_ID)).thenReturn(Optional.of(reserva));
            doThrow(new BusinessException(ErrorCode.INVALID_STATE,
                    "No es posible cancelar esta reserva.", HttpStatus.UNPROCESSABLE_ENTITY))
                    .when(reservaValidador).validarElegibilidadCancelacion(reserva, EMAIL);

            assertThatThrownBy(() -> service.cancelarReserva(RESERVA_ID, EMAIL))
                    .isInstanceOf(BusinessException.class);
        }
    }

    // ── CA-01: BASICA sin abono ───────────────────────────────────────────────

    @Nested
    @DisplayName("CA-01: BASICA sin abono → CANCELADA sin WhatsApp")
    class BasicaSinAbono {

        private Reserva reserva;

        @BeforeEach
        void setUp() {
            reserva = buildReserva(TipoReserva.BASICA, EstadoReserva.CONFIRMADA,
                    LocalDateTime.now().plusDays(2));
            when(reservaRepository.findById(RESERVA_ID)).thenReturn(Optional.of(reserva));
            when(reservaRepository.save(any(Reserva.class))).thenAnswer(inv -> inv.getArgument(0));
            // Sin abonos
            when(abonoRepository.findByReserva_ReservaIdOrderByAbonoFechaHoraAsc(RESERVA_ID))
                    .thenReturn(List.of());
            when(reservaMapper.toCancelarResponse(any(), eq(false), eq(null)))
                    .thenReturn(stubResponse(false));
        }

        @Test
        @DisplayName("El estado de la reserva guardada es CANCELADA")
        void estadoEsCancelada() {
            service.cancelarReserva(RESERVA_ID, EMAIL);

            ArgumentCaptor<Reserva> captor = ArgumentCaptor.forClass(Reserva.class);
            verify(reservaRepository).save(captor.capture());
            assertThat(captor.getValue().getReservaEstado()).isEqualTo(EstadoReserva.CANCELADA);
        }

        @Test
        @DisplayName("No se construye mensaje de WhatsApp")
        void noSeConstruyeMensajeWhatsApp() {
            service.cancelarReserva(RESERVA_ID, EMAIL);

            verify(mensajeWhatsAppBuilder, never()).construirMensaje(any(), any());
        }

        @Test
        @DisplayName("El mapper es llamado con requiereWhatsApp=false y mensajeWhatsApp=null")
        void mapperLlamadoConFalseNull() {
            service.cancelarReserva(RESERVA_ID, EMAIL);

            verify(reservaMapper).toCancelarResponse(any(Reserva.class), eq(false), eq(null));
        }
    }

    // ── WebSocket ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Notificación WebSocket de cancelación")
    class NotificacionWebSocket {

        @BeforeEach
        void setUp() {
            Reserva reserva = buildReserva(TipoReserva.BASICA, EstadoReserva.CONFIRMADA,
                    LocalDateTime.now().plusDays(2));
            when(reservaRepository.findById(RESERVA_ID)).thenReturn(Optional.of(reserva));
            when(reservaRepository.save(any(Reserva.class))).thenAnswer(inv -> inv.getArgument(0));
            when(abonoRepository.findByReserva_ReservaIdOrderByAbonoFechaHoraAsc(RESERVA_ID))
                    .thenReturn(List.of());
            when(reservaMapper.toCancelarResponse(any(), eq(false), eq(null)))
                    .thenReturn(stubResponse(false));
        }

        @Test
        @DisplayName("Cancelar exitosa publica evento WS con tipoEvento CANCELADA")
        void cancelarExitosa_publicaEventoCancelada() {
            service.cancelarReserva(RESERVA_ID, EMAIL);

            ArgumentCaptor<ReservaActualizadaWsMessage> captor =
                    ArgumentCaptor.forClass(ReservaActualizadaWsMessage.class);
            verify(wsPublisher).publicarReservaActualizada(captor.capture());
            assertThat(captor.getValue().getTipoEvento()).isEqualTo("CANCELADA");
            assertThat(captor.getValue().getReservaId()).isEqualTo(RESERVA_ID);
        }
    }

    // ── CA-02: BASICA con abono ───────────────────────────────────────────────

    @Nested
    @DisplayName("CA-02: BASICA con abono → CANCELADA con WhatsApp de reembolso")
    class BasicaConAbono {

        private static final String MSG = "Comunícate para gestionar el reembolso de tu abono.";
        private Reserva reserva;

        @BeforeEach
        void setUp() {
            reserva = buildReserva(TipoReserva.BASICA, EstadoReserva.CONFIRMADA,
                    LocalDateTime.now().plusDays(2));
            when(reservaRepository.findById(RESERVA_ID)).thenReturn(Optional.of(reserva));
            when(reservaRepository.save(any(Reserva.class))).thenAnswer(inv -> inv.getArgument(0));
            // Con abono de $50
            when(abonoRepository.findByReserva_ReservaIdOrderByAbonoFechaHoraAsc(RESERVA_ID))
                    .thenReturn(List.of(buildAbono(new BigDecimal("50.00"))));
            when(mensajeWhatsAppBuilder.construirMensaje(any(),
                    eq(MensajeWhatsAppBuilder.MSG_WA_CANCELACION_REEMBOLSO)))
                    .thenReturn(MSG);
            when(reservaMapper.toCancelarResponse(any(), eq(true), eq(MSG)))
                    .thenReturn(stubResponse(true));
        }

        @Test
        @DisplayName("El estado de la reserva guardada es CANCELADA")
        void estadoEsCancelada() {
            service.cancelarReserva(RESERVA_ID, EMAIL);

            ArgumentCaptor<Reserva> captor = ArgumentCaptor.forClass(Reserva.class);
            verify(reservaRepository).save(captor.capture());
            assertThat(captor.getValue().getReservaEstado()).isEqualTo(EstadoReserva.CANCELADA);
        }

        @Test
        @DisplayName("Se construye mensaje de WhatsApp con constante de reembolso")
        void seConstruyeMensajeWhatsApp() {
            service.cancelarReserva(RESERVA_ID, EMAIL);

            verify(mensajeWhatsAppBuilder).construirMensaje(any(Reserva.class),
                    eq(MensajeWhatsAppBuilder.MSG_WA_CANCELACION_REEMBOLSO));
        }

        @Test
        @DisplayName("El mapper es llamado con requiereWhatsApp=true y el mensaje")
        void mapperLlamadoConTrueYMensaje() {
            service.cancelarReserva(RESERVA_ID, EMAIL);

            verify(reservaMapper).toCancelarResponse(any(Reserva.class), eq(true), eq(MSG));
        }
    }

    // ── CA-03: ESPECIAL antes 16:00 ──────────────────────────────────────────

    @Nested
    @DisplayName("CA-03: ESPECIAL antes 16:00 del día de la reserva → CANCELADA con WhatsApp")
    class EspecialAntes16h {

        private static final String MSG = "Comunícate para gestionar el reembolso de tu abono.";
        private Reserva reserva;

        @BeforeEach
        void setUp() {
            // Fecha 2 años en el futuro: siempre antes de las 16:00 de ese día futuro
            LocalDateTime fechaFutura = LocalDateTime.now().plusDays(730).withHour(19).withMinute(0);
            reserva = buildReserva(TipoReserva.ESPECIAL, EstadoReserva.PENDIENTE, fechaFutura);
            when(reservaRepository.findById(RESERVA_ID)).thenReturn(Optional.of(reserva));
            when(reservaRepository.save(any(Reserva.class))).thenAnswer(inv -> inv.getArgument(0));
            when(mensajeWhatsAppBuilder.construirMensaje(any(),
                    eq(MensajeWhatsAppBuilder.MSG_WA_CANCELACION_REEMBOLSO)))
                    .thenReturn(MSG);
            when(reservaMapper.toCancelarResponse(any(), eq(true), eq(MSG)))
                    .thenReturn(stubResponse(true));
        }

        @Test
        @DisplayName("El estado de la reserva guardada es CANCELADA")
        void estadoEsCancelada() {
            service.cancelarReserva(RESERVA_ID, EMAIL);

            ArgumentCaptor<Reserva> captor = ArgumentCaptor.forClass(Reserva.class);
            verify(reservaRepository).save(captor.capture());
            assertThat(captor.getValue().getReservaEstado()).isEqualTo(EstadoReserva.CANCELADA);
        }

        @Test
        @DisplayName("Se construye mensaje de WhatsApp con constante de reembolso")
        void seConstruyeMensajeWhatsApp() {
            service.cancelarReserva(RESERVA_ID, EMAIL);

            verify(mensajeWhatsAppBuilder).construirMensaje(any(Reserva.class),
                    eq(MensajeWhatsAppBuilder.MSG_WA_CANCELACION_REEMBOLSO));
        }

        @Test
        @DisplayName("No se consultan abonos (ESPECIAL no depende del abono para la política)")
        void noSeConsultanAbonos() {
            service.cancelarReserva(RESERVA_ID, EMAIL);

            verify(abonoRepository, never())
                    .findByReserva_ReservaIdOrderByAbonoFechaHoraAsc(RESERVA_ID);
        }
    }

    // ── CA-04: ESPECIAL después/igual 16:00 ──────────────────────────────────

    @Nested
    @DisplayName("CA-04: ESPECIAL después 16:00 del día de la reserva → CANCELADA sin WhatsApp")
    class EspecialDespues16h {

        private Reserva reserva;

        @BeforeEach
        void setUp() {
            // Fecha en el pasado: la hora 16:00 de ese día ya pasó (ahora > 16:00 de ayer)
            LocalDateTime fechaPasada = LocalDateTime.now().minusDays(1).withHour(19).withMinute(0);
            reserva = buildReserva(TipoReserva.ESPECIAL, EstadoReserva.PENDIENTE, fechaPasada);
            when(reservaRepository.findById(RESERVA_ID)).thenReturn(Optional.of(reserva));
            when(reservaRepository.save(any(Reserva.class))).thenAnswer(inv -> inv.getArgument(0));
            when(reservaMapper.toCancelarResponse(any(), eq(false), eq(null)))
                    .thenReturn(stubResponse(false));
        }

        @Test
        @DisplayName("El estado de la reserva guardada es CANCELADA")
        void estadoEsCancelada() {
            service.cancelarReserva(RESERVA_ID, EMAIL);

            ArgumentCaptor<Reserva> captor = ArgumentCaptor.forClass(Reserva.class);
            verify(reservaRepository).save(captor.capture());
            assertThat(captor.getValue().getReservaEstado()).isEqualTo(EstadoReserva.CANCELADA);
        }

        @Test
        @DisplayName("No se construye mensaje de WhatsApp (sin reembolso)")
        void noSeConstruyeMensajeWhatsApp() {
            service.cancelarReserva(RESERVA_ID, EMAIL);

            verify(mensajeWhatsAppBuilder, never()).construirMensaje(any(), any());
        }

        @Test
        @DisplayName("El mapper es llamado con requiereWhatsApp=false y mensajeWhatsApp=null")
        void mapperLlamadoConFalseNull() {
            service.cancelarReserva(RESERVA_ID, EMAIL);

            verify(reservaMapper).toCancelarResponse(any(Reserva.class), eq(false), eq(null));
        }
    }
}
