package co.edu.unicauca.backend.modules.reservas.service;

import co.edu.unicauca.backend.modules.auth.entity.Usuario;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ComandaItemRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ComandaRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ZonaRepository;
import co.edu.unicauca.backend.modules.notificaciones.service.NotificacionWsPublisher;
import co.edu.unicauca.backend.modules.pagos_caja.repository.AbonoRepository;
import co.edu.unicauca.backend.modules.reservas.dto.request.CrearReservaRequest;
import co.edu.unicauca.backend.modules.reservas.dto.request.PreOrdenItemRequest;
import co.edu.unicauca.backend.modules.reservas.dto.response.DecoracionDisponibleResponse;
import co.edu.unicauca.backend.modules.reservas.dto.response.DisponibilidadResponse;
import co.edu.unicauca.backend.modules.reservas.dto.response.ReservaDetalleResponse;
import co.edu.unicauca.backend.modules.reservas.dto.response.ReservaResponse;
import co.edu.unicauca.backend.modules.reservas.dto.response.ZonaDisponibleResponse;
import co.edu.unicauca.backend.modules.reservas.entity.Decoracion;
import co.edu.unicauca.backend.modules.reservas.entity.Reserva;
import co.edu.unicauca.backend.modules.reservas.mapper.ReservaMapper;
import co.edu.unicauca.backend.modules.reservas.repository.DecoracionRepository;
import co.edu.unicauca.backend.modules.reservas.repository.ReservaRepository;
import co.edu.unicauca.backend.modules.usuarios.entity.Cliente;
import co.edu.unicauca.backend.modules.usuarios.repository.ClienteRepository;
import co.edu.unicauca.backend.shared.enums.EstadoReserva;
import co.edu.unicauca.backend.shared.enums.TipoReserva;
import co.edu.unicauca.backend.shared.exception.BusinessException;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ReservaService - CRUD (crear, obtenerFuturas, obtenerDetalle, obtenerCanceladas)")
class ReservaServiceCrudTest {

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
    private static final Long CLIENTE_ID = 10L;
    private static final Long RESERVA_ID = 1L;

    // Shared objects built in setUp helpers
    private Cliente clienteMock;
    private DisponibilidadResponse dispDisponible;
    private ReservaResponse reservaResponseMock;
    private Reserva reservaMock;

    // -----------------------------------------------------------------------
    // Shared setup helpers
    // -----------------------------------------------------------------------

    /**
     * Creates a Cliente with a Usuario whose email equals the given address.
     * Uses reflection to set private fields — same pattern as ReservaServiceModificarTest.
     */
    private Cliente buildCliente(Long id, String email) {
        Usuario usuario = new Usuario();
        setField(usuario, Usuario.class, "usuarioEmail", email);

        Cliente cliente = new Cliente();
        // Try superclass first (in case usuario is declared there), then own class
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
        setField(cliente, Cliente.class, "usuarioId", id);
        return cliente;
    }

    /** Generic reflection helper to set any declared field on the given object. */
    private void setField(Object target, Class<?> clazz, String fieldName, Object value) {
        try {
            java.lang.reflect.Field f = clazz.getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(target, value);
        } catch (NoSuchFieldException e) {
            // Walk up the hierarchy
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

    /** Creates a Decoracion with costo > 0. */
    private Decoracion decoracionConCosto(Long id, BigDecimal costo) {
        Decoracion d = new Decoracion();
        setField(d, Decoracion.class, "decoracionId", id);
        setField(d, Decoracion.class, "decoracionCostoAdicional", costo);
        return d;
    }

    /** Builds a minimal valid CrearReservaRequest. */
    private CrearReservaRequest buildRequest(LocalDateTime fechaHora, int personas,
                                              Long decoracionId, Long zonaId,
                                              List<PreOrdenItemRequest> preOrden) {
        CrearReservaRequest req = new CrearReservaRequest();
        setField(req, CrearReservaRequest.class, "fechaHoraLlegada", fechaHora);
        setField(req, CrearReservaRequest.class, "numeroPersonas", personas);
        if (decoracionId != null) {
            setField(req, CrearReservaRequest.class, "decoracionId", decoracionId);
        }
        if (zonaId != null) {
            setField(req, CrearReservaRequest.class, "zonaId", zonaId);
        }
        if (preOrden != null) {
            setField(req, CrearReservaRequest.class, "preOrden", preOrden);
        }
        return req;
    }

    /** Default happy-path request (no zona, no decoracion, no preOrden). */
    private CrearReservaRequest buildDefaultRequest() {
        return buildRequest(LocalDateTime.now().plusDays(2), 2, null, null, null);
    }

    // -----------------------------------------------------------------------
    // crearReserva — tests
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("crearReserva")
    class CrearReserva {

        @BeforeEach
        void setupHappyPath() {
            clienteMock = buildCliente(CLIENTE_ID, EMAIL);

            // Zona disponible with enough capacity
            ZonaDisponibleResponse zonaDto = ZonaDisponibleResponse.builder()
                    .zonaId(1L).capacidad(100).build();

            dispDisponible = DisponibilidadResponse.builder()
                    .disponible(true)
                    .zonas(List.of(zonaDto))
                    .decoraciones(List.of())
                    .build();

            reservaMock = Reserva.builder()
                    .reservaId(RESERVA_ID)
                    .cliente(clienteMock)
                    .reservaFechaHoraLlegada(LocalDateTime.now().plusDays(2))
                    .reservaNumeroPersonas(2)
                    .reservaEstado(EstadoReserva.CONFIRMADA)
                    .reservaTipo(TipoReserva.BASICA)
                    .build();

            reservaResponseMock = ReservaResponse.builder()
                    .reservaId(RESERVA_ID)
                    .estado("CONFIRMADA")
                    .tipo("BASICA")
                    .requiereWhatsApp(false)
                    .build();

            // Default stubs — all lenient so individual tests can override
            when(clienteRepository.findByUsuario_UsuarioEmail(any()))
                    .thenReturn(Optional.of(clienteMock));
            when(reservaValidador.esHorarioValido(any(), any(), any())).thenReturn(true);
            when(reservaValidador.estaBloqueda(any())).thenReturn(false);
            when(reservaValidador.tieneDecoracionConCosto(any())).thenReturn(false);
            when(disponibilidadConsultador.consultarParaNuevaReserva(any(), anyInt(), any(), any()))
                    .thenReturn(dispDisponible);
            // For the no-zona branch: any zone capacity check returns 0 occupied seats
            when(reservaRepository.sumPersonasByZonaEnDia(any(), any(), any(), any()))
                    .thenReturn(0);
            when(reservaRepository.save(any())).thenReturn(reservaMock);
            when(reservaMapper.toResponse(any(), anyBoolean(), any()))
                    .thenReturn(reservaResponseMock);
            // No pre-existing comanda
            when(comandaRepository.findByReserva_ReservaIdAndComandaEstado(any(), any()))
                    .thenReturn(Optional.empty());
        }

        @Test
        @DisplayName("Cliente no existe → ResourceNotFoundException")
        void clienteNoExiste_lanzaResourceNotFoundException() {
            when(clienteRepository.findByUsuario_UsuarioEmail(any()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.crearReserva(EMAIL, buildDefaultRequest()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Horario inválido → BusinessException")
        void horarioInvalido_lanzaBusinessException() {
            when(reservaValidador.esHorarioValido(any(), any(), any())).thenReturn(false);

            assertThatThrownBy(() -> service.crearReserva(EMAIL, buildDefaultRequest()))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("Hora bloqueada → BusinessException")
        void horaBloqueada_lanzaBusinessException() {
            when(reservaValidador.estaBloqueda(any())).thenReturn(true);

            assertThatThrownBy(() -> service.crearReserva(EMAIL, buildDefaultRequest()))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("Decoración no existe → ResourceNotFoundException")
        void decoracionNoExiste_lanzaResourceNotFoundException() {
            when(decoracionRepository.findById(99L)).thenReturn(Optional.empty());

            CrearReservaRequest req = buildRequest(
                    LocalDateTime.now().plusDays(2), 2, 99L, null, null);

            assertThatThrownBy(() -> service.crearReserva(EMAIL, req))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Zona no existe → ResourceNotFoundException")
        void zonaNoExiste_lanzaResourceNotFoundException() {
            when(zonaRepository.findById(99L)).thenReturn(Optional.empty());

            CrearReservaRequest req = buildRequest(
                    LocalDateTime.now().plusDays(2), 2, null, 99L, null);

            assertThatThrownBy(() -> service.crearReserva(EMAIL, req))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Disponibilidad cambió → BusinessException")
        void disponibilidadCambio_lanzaBusinessException() {
            DisponibilidadResponse noDisp = DisponibilidadResponse.builder()
                    .disponible(false)
                    .zonas(List.of())
                    .decoraciones(List.of())
                    .build();
            when(disponibilidadConsultador.consultarParaNuevaReserva(any(), anyInt(), any(), any()))
                    .thenReturn(noDisp);

            assertThatThrownBy(() -> service.crearReserva(EMAIL, buildDefaultRequest()))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("Reserva básica sin decoración → estado CONFIRMADA, tipo BASICA")
        void reservaBasicaSinDecoracion_estadoCONFIRMADA_tipoBasica() {
            service.crearReserva(EMAIL, buildDefaultRequest());

            ArgumentCaptor<Reserva> captor = ArgumentCaptor.forClass(Reserva.class);
            verify(reservaRepository).save(captor.capture());

            Reserva saved = captor.getValue();
            assertThat(saved.getReservaEstado()).isEqualTo(EstadoReserva.CONFIRMADA);
            assertThat(saved.getReservaTipo()).isEqualTo(TipoReserva.BASICA);
        }

        @Test
        @DisplayName("Decoración con costo → estado PENDIENTE, tipo ESPECIAL")
        void reservaConDecoracionConCosto_estadoPENDIENTE_tipoEspecial() {
            Long decId = 5L;
            Decoracion dec = decoracionConCosto(decId, new BigDecimal("50000"));

            when(decoracionRepository.findById(decId)).thenReturn(Optional.of(dec));
            when(reservaValidador.tieneDecoracionConCosto(dec)).thenReturn(true);

            // Disponibilidad must include this decoracion
            DecoracionDisponibleResponse decDto = DecoracionDisponibleResponse.builder()
                    .decoracionId(decId).build();
            ZonaDisponibleResponse zonaDto = ZonaDisponibleResponse.builder()
                    .zonaId(1L).capacidad(100).build();
            DisponibilidadResponse dispConDec = DisponibilidadResponse.builder()
                    .disponible(true)
                    .zonas(List.of(zonaDto))
                    .decoraciones(List.of(decDto))
                    .build();
            when(disponibilidadConsultador.consultarParaNuevaReserva(any(), anyInt(), any(), any()))
                    .thenReturn(dispConDec);

            // Mock WhatsApp builder for ESPECIAL path
            when(mensajeWhatsAppBuilder.construirMensaje(any(), any())).thenReturn("wa-msg");
            when(reservaMapper.toResponse(any(), eq(true), anyString()))
                    .thenReturn(ReservaResponse.builder()
                            .reservaId(RESERVA_ID).estado("PENDIENTE").tipo("ESPECIAL")
                            .requiereWhatsApp(true).build());

            // Date must be strictly after today
            LocalDateTime futureDate = LocalDateTime.now().plusDays(2);
            CrearReservaRequest req = buildRequest(futureDate, 2, decId, null, null);

            service.crearReserva(EMAIL, req);

            ArgumentCaptor<Reserva> captor = ArgumentCaptor.forClass(Reserva.class);
            verify(reservaRepository).save(captor.capture());

            Reserva saved = captor.getValue();
            assertThat(saved.getReservaEstado()).isEqualTo(EstadoReserva.PENDIENTE);
            assertThat(saved.getReservaTipo()).isEqualTo(TipoReserva.ESPECIAL);
        }

        @Test
        @DisplayName("Reserva ESPECIAL con fecha = hoy → BusinessException (anticipación mínima)")
        void reservaEspecialHoyMismo_lanzaBusinessException() {
            Long decId = 5L;
            Decoracion dec = decoracionConCosto(decId, new BigDecimal("50000"));

            when(decoracionRepository.findById(decId)).thenReturn(Optional.of(dec));
            when(reservaValidador.tieneDecoracionConCosto(dec)).thenReturn(true);

            DecoracionDisponibleResponse decDto = DecoracionDisponibleResponse.builder()
                    .decoracionId(decId).build();
            ZonaDisponibleResponse zonaDto = ZonaDisponibleResponse.builder()
                    .zonaId(1L).capacidad(100).build();
            DisponibilidadResponse dispConDec = DisponibilidadResponse.builder()
                    .disponible(true)
                    .zonas(List.of(zonaDto))
                    .decoraciones(List.of(decDto))
                    .build();
            when(disponibilidadConsultador.consultarParaNuevaReserva(any(), anyInt(), any(), any()))
                    .thenReturn(dispConDec);

            // Today's date — NOT strictly after today → should trigger anticipation check
            LocalDateTime hoy = LocalDate.now().atTime(20, 0);
            CrearReservaRequest req = buildRequest(hoy, 2, decId, null, null);

            assertThatThrownBy(() -> service.crearReserva(EMAIL, req))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("anticipación");
        }

        @Test
        @DisplayName("Reserva ESPECIAL con fecha +2 días → sin excepción de anticipación")
        void reservaEspecialManana_sinExcepcion() {
            Long decId = 5L;
            Decoracion dec = decoracionConCosto(decId, new BigDecimal("50000"));

            when(decoracionRepository.findById(decId)).thenReturn(Optional.of(dec));
            when(reservaValidador.tieneDecoracionConCosto(dec)).thenReturn(true);

            DecoracionDisponibleResponse decDto = DecoracionDisponibleResponse.builder()
                    .decoracionId(decId).build();
            ZonaDisponibleResponse zonaDto = ZonaDisponibleResponse.builder()
                    .zonaId(1L).capacidad(100).build();
            DisponibilidadResponse dispConDec = DisponibilidadResponse.builder()
                    .disponible(true)
                    .zonas(List.of(zonaDto))
                    .decoraciones(List.of(decDto))
                    .build();
            when(disponibilidadConsultador.consultarParaNuevaReserva(any(), anyInt(), any(), any()))
                    .thenReturn(dispConDec);
            when(mensajeWhatsAppBuilder.construirMensaje(any(), any())).thenReturn("wa-msg");
            when(reservaMapper.toResponse(any(), eq(true), anyString()))
                    .thenReturn(ReservaResponse.builder()
                            .reservaId(RESERVA_ID).estado("PENDIENTE").tipo("ESPECIAL")
                            .requiereWhatsApp(true).build());

            LocalDateTime dosManana = LocalDateTime.now().plusDays(2);
            CrearReservaRequest req = buildRequest(dosManana, 2, decId, null, null);

            // Should NOT throw
            service.crearReserva(EMAIL, req);
        }

        @Test
        @DisplayName("Con pre-orden → delega validación a PreOrdenGestor")
        void conPreOrden_delegaValidarAPreOrdenGestor() {
            PreOrdenItemRequest item = new PreOrdenItemRequest();
            item.setProductoId(99L);
            item.setCantidad(2);
            item.setEsMenuEspecial(true);

            List<PreOrdenItemRequest> items = List.of(item);

            // preOrden with esMenuEspecial=true makes it ESPECIAL
            when(reservaValidador.tieneDecoracionConCosto(null)).thenReturn(false);
            when(mensajeWhatsAppBuilder.construirMensaje(any(), any())).thenReturn("wa-msg");
            when(reservaMapper.toResponse(any(), eq(true), anyString()))
                    .thenReturn(ReservaResponse.builder()
                            .reservaId(RESERVA_ID).estado("PENDIENTE").tipo("ESPECIAL")
                            .requiereWhatsApp(true).build());

            LocalDateTime futureDate = LocalDateTime.now().plusDays(2);
            CrearReservaRequest req = buildRequest(futureDate, 2, null, null, items);

            service.crearReserva(EMAIL, req);

            verify(preOrdenGestor).validarPreOrden(items, 2);
        }

        @Test
        @DisplayName("Con pre-orden → delega persistencia a PreOrdenGestor")
        void conPreOrden_delegaPersistirAPreOrdenGestor() {
            PreOrdenItemRequest item = new PreOrdenItemRequest();
            item.setProductoId(99L);
            item.setCantidad(2);
            item.setEsMenuEspecial(true);

            List<PreOrdenItemRequest> items = List.of(item);

            when(reservaValidador.tieneDecoracionConCosto(null)).thenReturn(false);
            when(mensajeWhatsAppBuilder.construirMensaje(any(), any())).thenReturn("wa-msg");
            when(reservaMapper.toResponse(any(), eq(true), anyString()))
                    .thenReturn(ReservaResponse.builder()
                            .reservaId(RESERVA_ID).estado("PENDIENTE").tipo("ESPECIAL")
                            .requiereWhatsApp(true).build());

            LocalDateTime futureDate = LocalDateTime.now().plusDays(2);
            CrearReservaRequest req = buildRequest(futureDate, 2, null, null, items);

            service.crearReserva(EMAIL, req);

            verify(preOrdenGestor).persistirPreOrden(any(Reserva.class), eq(items));
        }
    }

    // -----------------------------------------------------------------------
    // obtenerReservasFuturas — tests
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("obtenerReservasFuturas")
    class ObtenerReservasFuturas {

        private Cliente cliente;

        @BeforeEach
        void setup() {
            cliente = buildCliente(CLIENTE_ID, EMAIL);
        }

        @Test
        @DisplayName("Cliente no existe → ResourceNotFoundException")
        void clienteNoExiste_lanzaResourceNotFoundException() {
            when(clienteRepository.findByUsuario_UsuarioEmail(any()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.obtenerReservasFuturas(EMAIL))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Solo retorna reservas activas y futuras (PENDIENTE/CONFIRMADA y fecha futura)")
        void soloRetornaReservasActivasYFuturas() {
            when(clienteRepository.findByUsuario_UsuarioEmail(EMAIL))
                    .thenReturn(Optional.of(cliente));

            LocalDateTime futuro = LocalDateTime.now().plusDays(3);
            LocalDateTime pasado = LocalDateTime.now().minusDays(1);

            Reserva rPendienteFutura = Reserva.builder()
                    .reservaId(1L).cliente(cliente)
                    .reservaFechaHoraLlegada(futuro)
                    .reservaNumeroPersonas(2)
                    .reservaEstado(EstadoReserva.PENDIENTE)
                    .reservaTipo(TipoReserva.BASICA)
                    .build();

            Reserva rConfirmadaFutura = Reserva.builder()
                    .reservaId(2L).cliente(cliente)
                    .reservaFechaHoraLlegada(futuro)
                    .reservaNumeroPersonas(2)
                    .reservaEstado(EstadoReserva.CONFIRMADA)
                    .reservaTipo(TipoReserva.BASICA)
                    .build();

            Reserva rCancelada = Reserva.builder()
                    .reservaId(3L).cliente(cliente)
                    .reservaFechaHoraLlegada(futuro)
                    .reservaNumeroPersonas(2)
                    .reservaEstado(EstadoReserva.CANCELADA)
                    .reservaTipo(TipoReserva.BASICA)
                    .build();

            Reserva rPendientePasada = Reserva.builder()
                    .reservaId(4L).cliente(cliente)
                    .reservaFechaHoraLlegada(pasado)
                    .reservaNumeroPersonas(2)
                    .reservaEstado(EstadoReserva.PENDIENTE)
                    .reservaTipo(TipoReserva.BASICA)
                    .build();

            when(reservaRepository.findByCliente_UsuarioIdOrderByReservaFechaHoraLlegadaDesc(CLIENTE_ID))
                    .thenReturn(List.of(rPendienteFutura, rConfirmadaFutura, rCancelada, rPendientePasada));

            ReservaDetalleResponse resumen1 = ReservaDetalleResponse.builder()
                    .reservaId(1L).estado("PENDIENTE").build();
            ReservaDetalleResponse resumen2 = ReservaDetalleResponse.builder()
                    .reservaId(2L).estado("CONFIRMADA").build();

            when(reservaMapper.toResumen(rPendienteFutura)).thenReturn(resumen1);
            when(reservaMapper.toResumen(rConfirmadaFutura)).thenReturn(resumen2);

            List<ReservaDetalleResponse> result = service.obtenerReservasFuturas(EMAIL);

            assertThat(result).hasSize(2);
            assertThat(result).extracting(ReservaDetalleResponse::getReservaId)
                    .containsExactlyInAnyOrder(1L, 2L);
        }

        @Test
        @DisplayName("Retorna ordenada ascendentemente por fecha de llegada")
        void retornaOrdenadaAscendentePorFecha() {
            when(clienteRepository.findByUsuario_UsuarioEmail(EMAIL))
                    .thenReturn(Optional.of(cliente));

            LocalDateTime fechaLejana = LocalDateTime.now().plusDays(4);
            LocalDateTime fechaCercana = LocalDateTime.now().plusDays(2);

            Reserva rLejana = Reserva.builder()
                    .reservaId(10L).cliente(cliente)
                    .reservaFechaHoraLlegada(fechaLejana)
                    .reservaNumeroPersonas(2)
                    .reservaEstado(EstadoReserva.PENDIENTE)
                    .reservaTipo(TipoReserva.BASICA)
                    .build();

            Reserva rCercana = Reserva.builder()
                    .reservaId(11L).cliente(cliente)
                    .reservaFechaHoraLlegada(fechaCercana)
                    .reservaNumeroPersonas(2)
                    .reservaEstado(EstadoReserva.PENDIENTE)
                    .reservaTipo(TipoReserva.BASICA)
                    .build();

            // Repository returns desc order — service must sort ascending
            when(reservaRepository.findByCliente_UsuarioIdOrderByReservaFechaHoraLlegadaDesc(CLIENTE_ID))
                    .thenReturn(List.of(rLejana, rCercana));

            ReservaDetalleResponse resumenLejana = ReservaDetalleResponse.builder()
                    .reservaId(10L).fechaHoraLlegada(fechaLejana.toString()).build();
            ReservaDetalleResponse resumenCercana = ReservaDetalleResponse.builder()
                    .reservaId(11L).fechaHoraLlegada(fechaCercana.toString()).build();

            when(reservaMapper.toResumen(rLejana)).thenReturn(resumenLejana);
            when(reservaMapper.toResumen(rCercana)).thenReturn(resumenCercana);

            List<ReservaDetalleResponse> result = service.obtenerReservasFuturas(EMAIL);

            // Closer date (id=11) should come first
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getReservaId()).isEqualTo(11L);
            assertThat(result.get(1).getReservaId()).isEqualTo(10L);
        }
    }

    // -----------------------------------------------------------------------
    // obtenerDetalleReserva — tests
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("obtenerDetalleReserva")
    class ObtenerDetalleReserva {

        private static final String OWNER_EMAIL = "mio@test.com";
        private static final String OTHER_EMAIL  = "otro@test.com";

        private Reserva reservaDeOwner;

        @BeforeEach
        void setup() {
            Cliente owner = buildCliente(CLIENTE_ID, OWNER_EMAIL);
            reservaDeOwner = Reserva.builder()
                    .reservaId(RESERVA_ID)
                    .cliente(owner)
                    .reservaFechaHoraLlegada(LocalDateTime.now().plusDays(1))
                    .reservaNumeroPersonas(2)
                    .reservaEstado(EstadoReserva.CONFIRMADA)
                    .reservaTipo(TipoReserva.BASICA)
                    .build();
        }

        @Test
        @DisplayName("Reserva no existe → ResourceNotFoundException")
        void reservaNoExiste_lanzaResourceNotFoundException() {
            when(reservaRepository.findById(RESERVA_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.obtenerDetalleReserva(RESERVA_ID, OWNER_EMAIL))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Cliente accede a reserva ajena → BusinessException FORBIDDEN")
        void clienteAccedeReservaAjena_lanzaBusinessException_403() {
            when(reservaRepository.findById(RESERVA_ID))
                    .thenReturn(Optional.of(reservaDeOwner));

            assertThatThrownBy(() -> service.obtenerDetalleReserva(RESERVA_ID, OTHER_EMAIL))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("Admin (emailAutenticado=null) accede a reserva ajena → retorna detalle")
        void adminAccedeReservaDeOtroCliente_retornaDetalle() {
            when(reservaRepository.findById(RESERVA_ID))
                    .thenReturn(Optional.of(reservaDeOwner));
            when(abonoRepository.findByReserva_ReservaIdOrderByAbonoFechaHoraAsc(RESERVA_ID))
                    .thenReturn(List.of());
            when(comandaRepository.findByReserva_ReservaIdAndComandaEstado(any(), any()))
                    .thenReturn(Optional.empty());

            ReservaDetalleResponse detalleMock = ReservaDetalleResponse.builder()
                    .reservaId(RESERVA_ID).build();
            when(reservaMapper.toDetalleResponse(any(), any(), any())).thenReturn(detalleMock);

            ReservaDetalleResponse result = service.obtenerDetalleReserva(RESERVA_ID, null);

            assertThat(result).isNotNull();
            assertThat(result.getReservaId()).isEqualTo(RESERVA_ID);
        }

        @Test
        @DisplayName("Cliente propietario accede a su propia reserva → retorna detalle")
        void clientePropietario_retornaDetalle() {
            when(reservaRepository.findById(RESERVA_ID))
                    .thenReturn(Optional.of(reservaDeOwner));
            when(abonoRepository.findByReserva_ReservaIdOrderByAbonoFechaHoraAsc(RESERVA_ID))
                    .thenReturn(List.of());
            when(comandaRepository.findByReserva_ReservaIdAndComandaEstado(any(), any()))
                    .thenReturn(Optional.empty());

            ReservaDetalleResponse detalleMock = ReservaDetalleResponse.builder()
                    .reservaId(RESERVA_ID).build();
            when(reservaMapper.toDetalleResponse(any(), any(), any())).thenReturn(detalleMock);

            ReservaDetalleResponse result = service.obtenerDetalleReserva(RESERVA_ID, OWNER_EMAIL);

            assertThat(result).isNotNull();
            assertThat(result.getReservaId()).isEqualTo(RESERVA_ID);
        }
    }

    // -----------------------------------------------------------------------
    // obtenerReservasCanceladasODevueltas — tests
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("obtenerReservasCanceladasODevueltas")
    class ObtenerReservasCanceladas {

        private Cliente cliente;

        @BeforeEach
        void setup() {
            cliente = buildCliente(CLIENTE_ID, EMAIL);
        }

        @Test
        @DisplayName("Cliente no existe → ResourceNotFoundException")
        void clienteNoExiste_lanzaResourceNotFoundException() {
            when(clienteRepository.findByUsuario_UsuarioEmail(any()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.obtenerReservasCanceladasODevueltas(EMAIL))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Solo retorna estados terminales (CANCELADA y DEVUELTA)")
        void soloRetornaEstadosTerminales() {
            when(clienteRepository.findByUsuario_UsuarioEmail(EMAIL))
                    .thenReturn(Optional.of(cliente));

            LocalDateTime fecha = LocalDateTime.now().minusDays(5);

            Reserva rCancelada = Reserva.builder()
                    .reservaId(1L).cliente(cliente)
                    .reservaFechaHoraLlegada(fecha)
                    .reservaNumeroPersonas(2)
                    .reservaEstado(EstadoReserva.CANCELADA)
                    .reservaTipo(TipoReserva.BASICA)
                    .build();

            Reserva rDevuelta = Reserva.builder()
                    .reservaId(2L).cliente(cliente)
                    .reservaFechaHoraLlegada(fecha)
                    .reservaNumeroPersonas(2)
                    .reservaEstado(EstadoReserva.DEVUELTA)
                    .reservaTipo(TipoReserva.BASICA)
                    .build();

            Reserva rPendiente = Reserva.builder()
                    .reservaId(3L).cliente(cliente)
                    .reservaFechaHoraLlegada(LocalDateTime.now().plusDays(1))
                    .reservaNumeroPersonas(2)
                    .reservaEstado(EstadoReserva.PENDIENTE)
                    .reservaTipo(TipoReserva.BASICA)
                    .build();

            Reserva rConfirmada = Reserva.builder()
                    .reservaId(4L).cliente(cliente)
                    .reservaFechaHoraLlegada(LocalDateTime.now().plusDays(1))
                    .reservaNumeroPersonas(2)
                    .reservaEstado(EstadoReserva.CONFIRMADA)
                    .reservaTipo(TipoReserva.BASICA)
                    .build();

            when(reservaRepository.findByCliente_UsuarioIdOrderByReservaFechaHoraLlegadaDesc(CLIENTE_ID))
                    .thenReturn(List.of(rCancelada, rDevuelta, rPendiente, rConfirmada));

            ReservaDetalleResponse resumenCancelada = ReservaDetalleResponse.builder()
                    .reservaId(1L).estado("CANCELADA").build();
            ReservaDetalleResponse resumenDevuelta = ReservaDetalleResponse.builder()
                    .reservaId(2L).estado("DEVUELTA").build();

            when(reservaMapper.toResumen(rCancelada)).thenReturn(resumenCancelada);
            when(reservaMapper.toResumen(rDevuelta)).thenReturn(resumenDevuelta);

            List<ReservaDetalleResponse> result = service.obtenerReservasCanceladasODevueltas(EMAIL);

            assertThat(result).hasSize(2);
            assertThat(result).extracting(ReservaDetalleResponse::getReservaId)
                    .containsExactlyInAnyOrder(1L, 2L);
        }
    }
}
