package co.edu.unicauca.backend.modules.reservas.service;

import co.edu.unicauca.backend.modules.mesas_comandas.entity.Zona;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ComandaItemRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ComandaRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ZonaRepository;
import co.edu.unicauca.backend.modules.pagos_caja.entity.Abono;
import co.edu.unicauca.backend.modules.pagos_caja.repository.AbonoRepository;
import co.edu.unicauca.backend.modules.reservas.dto.request.ModificarReservaRequest;
import co.edu.unicauca.backend.modules.reservas.dto.request.PreOrdenItemRequest;
import co.edu.unicauca.backend.modules.reservas.dto.response.DecoracionDisponibleResponse;
import co.edu.unicauca.backend.modules.reservas.dto.response.DisponibilidadResponse;
import co.edu.unicauca.backend.modules.reservas.dto.response.ModificarReservaResponse;
import co.edu.unicauca.backend.modules.reservas.dto.response.ZonaDisponibleResponse;
import co.edu.unicauca.backend.modules.reservas.entity.Decoracion;
import co.edu.unicauca.backend.modules.reservas.entity.Reserva;
import co.edu.unicauca.backend.modules.reservas.mapper.ReservaMapper;
import co.edu.unicauca.backend.modules.reservas.repository.DecoracionRepository;
import co.edu.unicauca.backend.modules.reservas.repository.ReservaRepository;
import co.edu.unicauca.backend.modules.usuarios.entity.Cliente;
import co.edu.unicauca.backend.modules.auth.entity.Usuario;
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
@DisplayName("ReservaService.modificarReserva")
class ReservaServiceModificarTest {

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

    @InjectMocks
    ReservaService service;

    private static final String EMAIL = "cliente@test.com";
    private static final Long RESERVA_ID = 1L;

    /** Reserva BÁSICA CONFIRMADA con fecha de llegada mañana a las 19:00. */
    private Reserva reservaBasicaConfirmada;

    /** Reserva ESPECIAL PENDIENTE con fecha de llegada mañana a las 19:00. */
    private Reserva reservaEspecialPendiente;

    /** Request de modificación mínima válida (solo fecha y personas, sin decoración ni zona). */
    private ModificarReservaRequest requestMinima;

    @BeforeEach
    void setUp() {
        Usuario usuario = new Usuario();
        try {
            java.lang.reflect.Field emailField = Usuario.class.getDeclaredField("usuarioEmail");
            emailField.setAccessible(true);
            emailField.set(usuario, EMAIL);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        Cliente cliente = new Cliente();
        try {
            java.lang.reflect.Field usuarioField = Cliente.class.getSuperclass().getDeclaredField("usuario");
            usuarioField.setAccessible(true);
            usuarioField.set(cliente, usuario);
        } catch (Exception ex) {
            try {
                java.lang.reflect.Field usuarioField = Cliente.class.getDeclaredField("usuario");
                usuarioField.setAccessible(true);
                usuarioField.set(cliente, usuario);
            } catch (Exception ex2) {
                throw new RuntimeException(ex2);
            }
        }

        LocalDateTime llegadaManana = LocalDate.now().plusDays(2).atTime(19, 0);

        reservaBasicaConfirmada = Reserva.builder()
                .reservaId(RESERVA_ID)
                .cliente(cliente)
                .reservaFechaHoraLlegada(llegadaManana)
                .reservaNumeroPersonas(2)
                .reservaEstado(EstadoReserva.CONFIRMADA)
                .reservaTipo(TipoReserva.BASICA)
                .build();

        reservaEspecialPendiente = Reserva.builder()
                .reservaId(RESERVA_ID)
                .cliente(cliente)
                .reservaFechaHoraLlegada(llegadaManana)
                .reservaNumeroPersonas(2)
                .reservaEstado(EstadoReserva.PENDIENTE)
                .reservaTipo(TipoReserva.ESPECIAL)
                .build();

        requestMinima = new ModificarReservaRequest();
        try {
            java.lang.reflect.Field f1 = ModificarReservaRequest.class.getDeclaredField("fechaHoraLlegada");
            f1.setAccessible(true);
            f1.set(requestMinima, llegadaManana);
            java.lang.reflect.Field f2 = ModificarReservaRequest.class.getDeclaredField("numeroPersonas");
            f2.setAccessible(true);
            f2.set(requestMinima, 3);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Stubs globales para ReservaValidador: replican la lógica real de los métodos delegados
        when(reservaValidador.esHorarioValido(any(), any(), any())).thenReturn(true);
        when(reservaValidador.estaBloqueda(any())).thenReturn(false);
        when(reservaValidador.tieneDecoracionConCosto(any())).thenAnswer(inv -> {
            Decoracion d = inv.getArgument(0);
            return d != null && d.getDecoracionCostoAdicional() != null
                    && d.getDecoracionCostoAdicional().compareTo(BigDecimal.ZERO) > 0;
        });
        doAnswer(inv -> {
            Reserva r = inv.getArgument(0);
            String email = inv.getArgument(1);
            if (!r.getCliente().getUsuario().getUsuarioEmail().equalsIgnoreCase(email)) {
                throw new BusinessException(ErrorCode.ACCESS_DENIED,
                        "Solo puedes modificar tus propias reservas.", HttpStatus.FORBIDDEN);
            }
            List<EstadoReserva> activos = List.of(EstadoReserva.PENDIENTE, EstadoReserva.CONFIRMADA);
            if (!activos.contains(r.getReservaEstado())) {
                throw new BusinessException(ErrorCode.INVALID_STATE,
                        "No es posible modificar esta reserva.", HttpStatus.UNPROCESSABLE_ENTITY);
            }
            return null;
        }).when(reservaValidador).validarElegibilidadModificacion(any(), any());

        when(mensajeWhatsAppBuilder.construirMensaje(any(), any())).thenReturn("mensaje-wa-test");
    }

    // ─── helpers de stub de disponibilidad ───────────────────────────────────

    /** Configura los mocks para que consultarDisponibilidadParaModificacion devuelva disponible=true. */
    private void stubDisponibilidadLibre() {
        ZonaDisponibleResponse zonaDto = ZonaDisponibleResponse.builder()
                .zonaId(10L).capacidad(20).build();
        when(disponibilidadConsultador.consultarParaModificacion(any(), anyInt(), anyLong(), any(), any()))
                .thenReturn(DisponibilidadResponse.builder()
                        .disponible(true)
                        .zonas(List.of(zonaDto))
                        .decoraciones(List.of())
                        .build());
        when(reservaRepository.sumPersonasByZonaEnDiaExcluyendo(anyLong(), any(), any(), any(), anyLong()))
                .thenReturn(0);
        when(comandaRepository.findByReserva_ReservaIdAndComandaEstado(anyLong(), any()))
                .thenReturn(Optional.empty());
    }

    /** Variante de stubDisponibilidadLibre que incluye decoraciones en la respuesta. */
    private void stubDisponibilidadLibreConDecoraciones(Long... decoracionIds) {
        ZonaDisponibleResponse zonaDto = ZonaDisponibleResponse.builder()
                .zonaId(10L).capacidad(20).build();
        List<DecoracionDisponibleResponse> decDtos = java.util.Arrays.stream(decoracionIds)
                .map(id -> DecoracionDisponibleResponse.builder().decoracionId(id).build())
                .collect(java.util.stream.Collectors.toList());
        when(disponibilidadConsultador.consultarParaModificacion(any(), anyInt(), anyLong(), any(), any()))
                .thenReturn(DisponibilidadResponse.builder()
                        .disponible(true)
                        .zonas(List.of(zonaDto))
                        .decoraciones(decDtos)
                        .build());
        when(reservaRepository.sumPersonasByZonaEnDiaExcluyendo(anyLong(), any(), any(), any(), anyLong()))
                .thenReturn(0);
        when(comandaRepository.findByReserva_ReservaIdAndComandaEstado(anyLong(), any()))
                .thenReturn(Optional.empty());
    }

    // ─── Validaciones previas ─────────────────────────────────────────────────

    @Nested
    @DisplayName("Validaciones previas")
    class ValidacionesPrevias {

        @Test
        @DisplayName("Reserva no encontrada → ResourceNotFoundException")
        void reservaNoEncontrada_lanzaNotFoundException() {
            when(reservaRepository.findById(RESERVA_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.modificarReserva(RESERVA_ID, EMAIL, requestMinima))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Cliente diferente al dueño → BusinessException ACCESS_DENIED")
        void clienteDiferente_lanzaAccessDenied() {
            when(reservaRepository.findById(RESERVA_ID))
                    .thenReturn(Optional.of(reservaBasicaConfirmada));

            assertThatThrownBy(() ->
                    service.modificarReserva(RESERVA_ID, "otro@test.com", requestMinima))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("propias reservas");
        }

        @Test
        @DisplayName("Reserva en estado CANCELADA → BusinessException INVALID_STATE")
        void reservaCancelada_lanzaInvalidState() {
            Reserva cancelada = Reserva.builder()
                    .reservaId(RESERVA_ID)
                    .cliente(reservaBasicaConfirmada.getCliente())
                    .reservaFechaHoraLlegada(LocalDate.now().plusDays(1).atTime(19, 0))
                    .reservaNumeroPersonas(2)
                    .reservaEstado(EstadoReserva.CANCELADA)
                    .reservaTipo(TipoReserva.BASICA)
                    .build();
            when(reservaRepository.findById(RESERVA_ID)).thenReturn(Optional.of(cancelada));

            assertThatThrownBy(() -> service.modificarReserva(RESERVA_ID, EMAIL, requestMinima))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("No es posible modificar esta reserva");
        }

        @Test
        @DisplayName("Hora límite estándar pasada (reserva de ayer) → BusinessException")
        void horaLimitePasada_lanzaInvalidState() {
            // Reserva para ayer: límite estándar es ayer a las 13:00, ya pasó
            LocalDateTime llegadaAyer = LocalDate.now().minusDays(1).atTime(19, 0);
            Reserva reservaAyer = Reserva.builder()
                    .reservaId(RESERVA_ID)
                    .cliente(reservaBasicaConfirmada.getCliente())
                    .reservaFechaHoraLlegada(llegadaAyer)
                    .reservaNumeroPersonas(2)
                    .reservaEstado(EstadoReserva.CONFIRMADA)
                    .reservaTipo(TipoReserva.BASICA)
                    .build();
            when(reservaRepository.findById(RESERVA_ID)).thenReturn(Optional.of(reservaAyer));

            assertThatThrownBy(() -> service.modificarReserva(RESERVA_ID, EMAIL, requestMinima))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Solo cancelarla");
        }

        @Test
        @DisplayName("Reserva ESPECIAL con límite especial pasado (llegada hoy) → BusinessException")
        void especialConLimiteEspecialPasado_lanzaInvalidState() {
            // Reserva ESPECIAL para HOY: límite especial es ayer a las 23:00, siempre pasó
            LocalDateTime llegadaHoy = LocalDate.now().atTime(19, 0);
            Reserva reservaHoy = Reserva.builder()
                    .reservaId(RESERVA_ID)
                    .cliente(reservaBasicaConfirmada.getCliente())
                    .reservaFechaHoraLlegada(llegadaHoy)
                    .reservaNumeroPersonas(2)
                    .reservaEstado(EstadoReserva.PENDIENTE)
                    .reservaTipo(TipoReserva.ESPECIAL)
                    .build();
            when(reservaRepository.findById(RESERVA_ID)).thenReturn(Optional.of(reservaHoy));

            assertThatThrownBy(() -> service.modificarReserva(RESERVA_ID, EMAIL, requestMinima))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Solo cancelarla");
        }
    }

    // ─── Transición BASICA → BASICA ───────────────────────────────────────────

    @Nested
    @DisplayName("Transición BASICA → BASICA")
    class BasicaABasica {

        @Test
        @DisplayName("Modifica campos y mantiene CONFIRMADA; sin abono → no requiere WhatsApp")
        void basicaABasica_actualizaYMantiene() {
            when(reservaRepository.findById(RESERVA_ID))
                    .thenReturn(Optional.of(reservaBasicaConfirmada));
            stubDisponibilidadLibre();
            when(abonoRepository.findByReserva_ReservaIdOrderByAbonoFechaHoraAsc(anyLong()))
                    .thenReturn(List.of());
            when(reservaRepository.save(any())).thenReturn(reservaBasicaConfirmada);

            ModificarReservaResponse respuestaMock = ModificarReservaResponse.builder()
                    .reservaId(RESERVA_ID).estado("CONFIRMADA").tipo("BASICA")
                    .requiereWhatsApp(false).build();
            when(reservaMapper.toModificarResponse(any(), anyBoolean(), any()))
                    .thenReturn(respuestaMock);

            ModificarReservaResponse respuesta =
                    service.modificarReserva(RESERVA_ID, EMAIL, requestMinima);

            assertThat(respuesta.isRequiereWhatsApp()).isFalse();
            assertThat(respuesta.getEstado()).isEqualTo("CONFIRMADA");

            ArgumentCaptor<Reserva> captor = ArgumentCaptor.forClass(Reserva.class);
            verify(reservaRepository).save(captor.capture());
            assertThat(captor.getValue().getReservaEstado()).isEqualTo(EstadoReserva.CONFIRMADA);
        }

        @Test
        @DisplayName("Con abono neto > platos → requiere WhatsApp (MSG_WA_ABONO_AJUSTE)")
        void basicaABasicaConAbonoMayorPlatos_requiereWhatsApp() {
            when(reservaRepository.findById(RESERVA_ID))
                    .thenReturn(Optional.of(reservaBasicaConfirmada));
            stubDisponibilidadLibre();

            // Abono de 50.000 y nueva pre-orden vacía → totalPlatos = 0 < totalAbonos
            Abono anticipo = abonoConMonto(new BigDecimal("50000"), TipoAbono.ANTICIPO);
            when(abonoRepository.findByReserva_ReservaIdOrderByAbonoFechaHoraAsc(anyLong()))
                    .thenReturn(List.of(anticipo));
            when(reservaRepository.save(any())).thenReturn(reservaBasicaConfirmada);

            ModificarReservaResponse respuestaMock = ModificarReservaResponse.builder()
                    .reservaId(RESERVA_ID).estado("CONFIRMADA").tipo("BASICA")
                    .requiereWhatsApp(true).mensajeWhatsApp("msg").build();
            when(reservaMapper.toModificarResponse(any(), eq(true), anyString()))
                    .thenReturn(respuestaMock);

            ModificarReservaResponse respuesta =
                    service.modificarReserva(RESERVA_ID, EMAIL, requestMinima);

            assertThat(respuesta.isRequiereWhatsApp()).isTrue();
        }
    }

    // ─── Transición ESPECIAL → BASICA ─────────────────────────────────────────

    @Nested
    @DisplayName("Transición ESPECIAL → BASICA")
    class EspecialABasica {

        @Test
        @DisplayName("Actualiza in-place a BASICA CONFIRMADA; sin abono → no requiere WhatsApp")
        void especialABasica_actualizaInPlace() {
            when(reservaRepository.findById(RESERVA_ID))
                    .thenReturn(Optional.of(reservaEspecialPendiente));
            stubDisponibilidadLibre();
            when(abonoRepository.findByReserva_ReservaIdOrderByAbonoFechaHoraAsc(anyLong()))
                    .thenReturn(List.of());
            when(reservaRepository.save(any())).thenReturn(reservaEspecialPendiente);

            ModificarReservaResponse respuestaMock = ModificarReservaResponse.builder()
                    .reservaId(RESERVA_ID).estado("CONFIRMADA").tipo("BASICA")
                    .requiereWhatsApp(false).build();
            when(reservaMapper.toModificarResponse(any(), anyBoolean(), any()))
                    .thenReturn(respuestaMock);

            ModificarReservaResponse respuesta =
                    service.modificarReserva(RESERVA_ID, EMAIL, requestMinima);

            // Update-in-place: un solo save, mismo reservaId
            verify(reservaRepository, times(1)).save(any(Reserva.class));
            assertThat(respuesta.getReservaId()).isEqualTo(RESERVA_ID);
            assertThat(respuesta.isRequiereWhatsApp()).isFalse();
        }

        @Test
        @DisplayName("Con abono neto > platos → requiere WhatsApp (MSG_WA_CAMBIO_ESPECIAL)")
        void especialABasicaConAbonoMayorPlatos_requiereWhatsApp() {
            when(reservaRepository.findById(RESERVA_ID))
                    .thenReturn(Optional.of(reservaEspecialPendiente));
            stubDisponibilidadLibre();

            Abono anticipo = abonoConMonto(new BigDecimal("80000"), TipoAbono.ANTICIPO);
            when(abonoRepository.findByReserva_ReservaIdOrderByAbonoFechaHoraAsc(anyLong()))
                    .thenReturn(List.of(anticipo));
            when(reservaRepository.save(any())).thenReturn(reservaEspecialPendiente);

            ModificarReservaResponse respuestaMock = ModificarReservaResponse.builder()
                    .reservaId(RESERVA_ID).estado("CONFIRMADA").tipo("BASICA")
                    .requiereWhatsApp(true).mensajeWhatsApp("msg").build();
            when(reservaMapper.toModificarResponse(any(), eq(true), anyString()))
                    .thenReturn(respuestaMock);

            ModificarReservaResponse respuesta =
                    service.modificarReserva(RESERVA_ID, EMAIL, requestMinima);

            assertThat(respuesta.isRequiereWhatsApp()).isTrue();
        }
    }

    // ─── Transición ESPECIAL → ESPECIAL ──────────────────────────────────────

    @Nested
    @DisplayName("Transición ESPECIAL → ESPECIAL")
    class EspecialAEspecial {

        @Test
        @DisplayName("Sin cambio de valor (misma decoración, mismas personas) → no requiere WhatsApp")
        void sinCambioDeValor_noRequiereWhatsApp() {
            Decoracion decoracion = decoracionConCosto(5L, new BigDecimal("50000"));
            Reserva reservaConDec = Reserva.builder()
                    .reservaId(RESERVA_ID)
                    .cliente(reservaEspecialPendiente.getCliente())
                    .reservaFechaHoraLlegada(LocalDate.now().plusDays(2).atTime(19, 0))
                    .reservaNumeroPersonas(2)
                    .reservaEstado(EstadoReserva.PENDIENTE)
                    .reservaTipo(TipoReserva.ESPECIAL)
                    .decoracion(decoracion)
                    .build();

            when(reservaRepository.findById(RESERVA_ID)).thenReturn(Optional.of(reservaConDec));
            when(decoracionRepository.findById(5L)).thenReturn(Optional.of(decoracion));
            stubDisponibilidadLibreConDecoraciones(5L);
            when(reservaRepository.save(any())).thenReturn(reservaConDec);

            ModificarReservaResponse respuestaMock = ModificarReservaResponse.builder()
                    .reservaId(RESERVA_ID).estado("PENDIENTE").tipo("ESPECIAL")
                    .requiereWhatsApp(false).build();
            when(reservaMapper.toModificarResponse(any(), anyBoolean(), any()))
                    .thenReturn(respuestaMock);

            // Request con la misma decoración y mismas personas → valor anterior == valor nuevo
            ModificarReservaRequest reqMismaDecoracion = buildRequest(
                    LocalDate.now().plusDays(2).atTime(19, 0), 2, 5L);

            ModificarReservaResponse respuesta =
                    service.modificarReserva(RESERVA_ID, EMAIL, reqMismaDecoracion);

            assertThat(respuesta.isRequiereWhatsApp()).isFalse();
        }

        @Test
        @DisplayName("Decoración con costo diferente → valor cambia → requiere WhatsApp")
        void cambioDeValorPorDecoracion_requiereWhatsApp() {
            Decoracion decoracionAnterior = decoracionConCosto(5L, new BigDecimal("50000"));
            Decoracion decoracionNueva    = decoracionConCosto(6L, new BigDecimal("80000"));

            Reserva reservaConDecAnterior = Reserva.builder()
                    .reservaId(RESERVA_ID)
                    .cliente(reservaEspecialPendiente.getCliente())
                    .reservaFechaHoraLlegada(LocalDate.now().plusDays(2).atTime(19, 0))
                    .reservaNumeroPersonas(2)
                    .reservaEstado(EstadoReserva.PENDIENTE)
                    .reservaTipo(TipoReserva.ESPECIAL)
                    .decoracion(decoracionAnterior)
                    .build();

            when(reservaRepository.findById(RESERVA_ID)).thenReturn(Optional.of(reservaConDecAnterior));
            when(decoracionRepository.findById(6L)).thenReturn(Optional.of(decoracionNueva));
            stubDisponibilidadLibreConDecoraciones(5L, 6L);
            when(reservaRepository.save(any())).thenReturn(reservaConDecAnterior);

            ModificarReservaResponse respuestaMock = ModificarReservaResponse.builder()
                    .reservaId(RESERVA_ID).estado("PENDIENTE").tipo("ESPECIAL")
                    .requiereWhatsApp(true).mensajeWhatsApp("msg").build();
            when(reservaMapper.toModificarResponse(any(), eq(true), anyString()))
                    .thenReturn(respuestaMock);

            ModificarReservaRequest reqNuevaDecoracion = buildRequest(
                    LocalDate.now().plusDays(2).atTime(19, 0), 2, 6L);

            ModificarReservaResponse respuesta =
                    service.modificarReserva(RESERVA_ID, EMAIL, reqNuevaDecoracion);

            assertThat(respuesta.isRequiereWhatsApp()).isTrue();
        }
    }

    // ─── Pre-orden ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Pre-orden en modificación")
    class PreOrdenEnModificacion {

        @Test
        @DisplayName("Toda modificación elimina la pre-orden anterior")
        void modificacion_siempreEliminaPreOrdenAnterior() {
            when(reservaRepository.findById(RESERVA_ID))
                    .thenReturn(Optional.of(reservaBasicaConfirmada));
            stubDisponibilidadLibre();
            when(abonoRepository.findByReserva_ReservaIdOrderByAbonoFechaHoraAsc(anyLong()))
                    .thenReturn(List.of());
            when(reservaRepository.save(any())).thenReturn(reservaBasicaConfirmada);
            when(reservaMapper.toModificarResponse(any(), anyBoolean(), any()))
                    .thenReturn(ModificarReservaResponse.builder()
                            .reservaId(RESERVA_ID).estado("CONFIRMADA").tipo("BASICA")
                            .requiereWhatsApp(false).build());

            service.modificarReserva(RESERVA_ID, EMAIL, requestMinima);

            verify(preOrdenGestor).eliminarPreOrdenExistente(RESERVA_ID);
        }

        @Test
        @DisplayName("BASICA → ESPECIAL por menú especial: tipo ESPECIAL, PENDIENTE, requiereWhatsApp=true")
        void basicaAEspecialPorMenuEspecial_requiereWhatsApp() {
            when(reservaRepository.findById(RESERVA_ID))
                    .thenReturn(Optional.of(reservaBasicaConfirmada));
            stubDisponibilidadLibre();
            when(reservaRepository.save(any())).thenReturn(reservaBasicaConfirmada);

            when(reservaMapper.toModificarResponse(any(), eq(true), anyString()))
                    .thenReturn(ModificarReservaResponse.builder()
                            .reservaId(RESERVA_ID).estado("PENDIENTE").tipo("ESPECIAL")
                            .requiereWhatsApp(true).mensajeWhatsApp("msg").build());

            ModificarReservaRequest reqMenuEspecial = buildRequestConMenuEspecial(
                    LocalDate.now().plusDays(2).atTime(19, 0), 2);

            ModificarReservaResponse respuesta =
                    service.modificarReserva(RESERVA_ID, EMAIL, reqMenuEspecial);

            assertThat(respuesta.isRequiereWhatsApp()).isTrue();
            assertThat(respuesta.getTipo()).isEqualTo("ESPECIAL");
            assertThat(respuesta.getEstado()).isEqualTo("PENDIENTE");

            ArgumentCaptor<Reserva> captor = ArgumentCaptor.forClass(Reserva.class);
            verify(reservaRepository).save(captor.capture());
            assertThat(captor.getValue().getReservaTipo()).isEqualTo(TipoReserva.ESPECIAL);
            assertThat(captor.getValue().getReservaEstado()).isEqualTo(EstadoReserva.PENDIENTE);
        }

        @Test
        @DisplayName("Request con pre-orden → persistirPreOrden es invocado con la reserva guardada")
        void requestConPreOrden_persisteNuevaPreOrden() {
            when(reservaRepository.findById(RESERVA_ID))
                    .thenReturn(Optional.of(reservaBasicaConfirmada));
            stubDisponibilidadLibre();
            when(reservaRepository.save(any())).thenReturn(reservaBasicaConfirmada);
            when(reservaMapper.toModificarResponse(any(), eq(true), anyString()))
                    .thenReturn(ModificarReservaResponse.builder()
                            .reservaId(RESERVA_ID).estado("PENDIENTE").tipo("ESPECIAL")
                            .requiereWhatsApp(true).mensajeWhatsApp("msg").build());

            ModificarReservaRequest reqMenuEspecial = buildRequestConMenuEspecial(
                    LocalDate.now().plusDays(2).atTime(19, 0), 2);

            service.modificarReserva(RESERVA_ID, EMAIL, reqMenuEspecial);

            verify(preOrdenGestor).persistirPreOrden(eq(reservaBasicaConfirmada), anyList());
        }
    }

    // ─── Disponibilidad ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("Disponibilidad insuficiente")
    class DisponibilidadCambio {

        @Test
        @DisplayName("Sin zonas libres → BusinessException con mensaje de disponibilidad")
        void sinZonasLibres_lanzaCambioDisponibilidad() {
            when(reservaRepository.findById(RESERVA_ID))
                    .thenReturn(Optional.of(reservaBasicaConfirmada));
            when(disponibilidadConsultador.consultarParaModificacion(any(), anyInt(), anyLong(), any(), any()))
                    .thenReturn(DisponibilidadResponse.builder().disponible(false).build());

            assertThatThrownBy(() -> service.modificarReserva(RESERVA_ID, EMAIL, requestMinima))
                    .isInstanceOf(BusinessException.class);
        }
    }

    // ─── Utilidades de test ───────────────────────────────────────────────────


    private Decoracion decoracionConCosto(Long id, BigDecimal costo) {
        Decoracion d = new Decoracion();
        try {
            java.lang.reflect.Field fId = Decoracion.class.getDeclaredField("decoracionId");
            fId.setAccessible(true);
            fId.set(d, id);
            java.lang.reflect.Field fCosto = Decoracion.class.getDeclaredField("decoracionCostoAdicional");
            fCosto.setAccessible(true);
            fCosto.set(d, costo);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return d;
    }

    private Abono abonoConMonto(BigDecimal monto, TipoAbono tipo) {
        Abono a = new Abono();
        try {
            java.lang.reflect.Field fMonto = Abono.class.getDeclaredField("abonoMonto");
            fMonto.setAccessible(true);
            fMonto.set(a, monto);
            java.lang.reflect.Field fTipo = Abono.class.getDeclaredField("abonoTipo");
            fTipo.setAccessible(true);
            fTipo.set(a, tipo);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return a;
    }

    private ModificarReservaRequest buildRequestConMenuEspecial(LocalDateTime fecha, int personas) {
        PreOrdenItemRequest item = new PreOrdenItemRequest();
        item.setProductoId(99L);
        item.setCantidad(2);
        item.setEsMenuEspecial(true);

        ModificarReservaRequest req = new ModificarReservaRequest();
        try {
            java.lang.reflect.Field f1 = ModificarReservaRequest.class.getDeclaredField("fechaHoraLlegada");
            f1.setAccessible(true);
            f1.set(req, fecha);
            java.lang.reflect.Field f2 = ModificarReservaRequest.class.getDeclaredField("numeroPersonas");
            f2.setAccessible(true);
            f2.set(req, personas);
            java.lang.reflect.Field f3 = ModificarReservaRequest.class.getDeclaredField("preOrden");
            f3.setAccessible(true);
            f3.set(req, List.of(item));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return req;
    }

    private ModificarReservaRequest buildRequest(LocalDateTime fecha, int personas, Long decoracionId) {
        ModificarReservaRequest req = new ModificarReservaRequest();
        try {
            java.lang.reflect.Field f1 = ModificarReservaRequest.class.getDeclaredField("fechaHoraLlegada");
            f1.setAccessible(true);
            f1.set(req, fecha);
            java.lang.reflect.Field f2 = ModificarReservaRequest.class.getDeclaredField("numeroPersonas");
            f2.setAccessible(true);
            f2.set(req, personas);
            if (decoracionId != null) {
                java.lang.reflect.Field f3 = ModificarReservaRequest.class.getDeclaredField("decoracionId");
                f3.setAccessible(true);
                f3.set(req, decoracionId);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return req;
    }
}
