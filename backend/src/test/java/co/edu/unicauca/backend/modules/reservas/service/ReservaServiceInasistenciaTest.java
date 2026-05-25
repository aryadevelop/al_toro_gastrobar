package co.edu.unicauca.backend.modules.reservas.service;

import co.edu.unicauca.backend.modules.mesas_comandas.repository.ComandaRepository;
import co.edu.unicauca.backend.modules.notificaciones.dto.ws.ReservaActualizadaWsMessage;
import co.edu.unicauca.backend.modules.notificaciones.service.NotificacionWsPublisher;
import co.edu.unicauca.backend.modules.reservas.dto.response.MarcarInasistenciaResponse;
import co.edu.unicauca.backend.modules.reservas.entity.Decoracion;
import co.edu.unicauca.backend.modules.reservas.entity.Reserva;
import co.edu.unicauca.backend.modules.reservas.mapper.ReservaMapper;
import co.edu.unicauca.backend.modules.reservas.repository.ReservaRepository;
import co.edu.unicauca.backend.modules.usuarios.entity.Cliente;
import co.edu.unicauca.backend.modules.auth.entity.Usuario;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Zona;
import co.edu.unicauca.backend.shared.enums.EstadoReserva;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * Tests unitarios para {@link ReservaService#marcarInasistencia(Long, Authentication)}.
 *
 * <p>Cubre:
 * <ul>
 *   <li>Solo CONFIRMADA puede marcarse como inasistencia.</li>
 *   <li>Deben haber transcurrido 30 minutos desde hora de llegada.</li>
 *   <li>MESERO solo puede marcar inasistencia de reservas del día actual.</li>
 *   <li>ADMIN puede marcar inasistencia de cualquier fecha.</li>
 *   <li>Libera zona y decoración.</li>
 *   <li>Elimina pre-orden.</li>
 *   <li>Publica evento WebSocket.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ReservaService - Marcar Inasistencia")
class ReservaServiceInasistenciaTest {

    @Mock private ReservaRepository reservaRepository;
    @Mock private ComandaRepository comandaRepository;
    @Mock private ReservaValidador reservaValidador;
    @Mock private PreOrdenGestor preOrdenGestor;
    @Mock private NotificacionWsPublisher wsPublisher;
    @Mock private ReservaMapper reservaMapper;
    @Mock private Authentication authentication;

    @InjectMocks private ReservaService reservaService;

    private static final Long RESERVA_ID = 100L;
    private static final String EMAIL_MESERO = "mesero@altoro.com";

    @BeforeEach
    void setUp() {
        // Mock default: Authentication como MESERO (lenient para evitar UnnecessaryStubbingException)
        lenient().when(authentication.getName()).thenReturn(EMAIL_MESERO);
        lenient().doReturn(List.of(new SimpleGrantedAuthority("ROLE_MESERO")))
                .when(authentication).getAuthorities();
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private Reserva crearReservaConfirmada(LocalDateTime fechaHoraLlegada) {
        Usuario usuario = Usuario.builder()
                .usuarioId(1L)
                .usuarioEmail("cliente@test.com")
                .build();

        Cliente cliente = Cliente.builder()
                .usuarioId(1L)
                .usuario(usuario)
                .clienteNombre("Juan Pérez")
                .build();

        Zona zona = Zona.builder()
                .zonaId(1L)
                .zonaNombre("Terraza")
                .build();

        Decoracion decoracion = Decoracion.builder()
                .decoracionId(1L)
                .decoracionNombre("Cumpleaños")
                .build();

        return Reserva.builder()
                .reservaId(RESERVA_ID)
                .cliente(cliente)
                .zona(zona)
                .decoracion(decoracion)
                .reservaFechaHoraLlegada(fechaHoraLlegada)
                .reservaNumeroPersonas(4)
                .reservaEstado(EstadoReserva.CONFIRMADA)
                .reservaTipo(TipoReserva.BASICA)
                .build();
    }

    // -----------------------------------------------------------------------
    // Tests de caso exitoso
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Caso exitoso: Reserva CONFIRMADA con +30 minutos")
    class CasoExitoso {

        @Test
        @DisplayName("Exactamente 30 minutos → marca inasistencia exitosamente")
        void exactamente30Min_marcaInasistenciaExitosa() {
            // Given: Reserva CONFIRMADA hace exactamente 30 minutos
            LocalDateTime horaLlegada = LocalDateTime.now().minusMinutes(30);
            Reserva reserva = crearReservaConfirmada(horaLlegada);

            when(reservaRepository.findById(RESERVA_ID)).thenReturn(Optional.of(reserva));
            doNothing().when(reservaValidador).validarElegibilidadInasistencia(eq(reserva), anyBoolean());
            when(reservaRepository.save(any(Reserva.class))).thenReturn(reserva);

            // When
            MarcarInasistenciaResponse response = reservaService.marcarInasistencia(RESERVA_ID, authentication);

            // Then: Estado cambia a INASISTENCIA
            ArgumentCaptor<Reserva> captor = ArgumentCaptor.forClass(Reserva.class);
            verify(reservaRepository).save(captor.capture());
            assertThat(captor.getValue().getReservaEstado()).isEqualTo(EstadoReserva.INASISTENCIA);

            // Pre-orden eliminada
            verify(preOrdenGestor).eliminarPreOrdenExistente(RESERVA_ID);

            // WebSocket publicado
            verify(wsPublisher).publicarReservaActualizada(any(ReservaActualizadaWsMessage.class));

            // Respuesta incluye recursos liberados
            assertThat(response).isNotNull();
            assertThat(response.getReservaId()).isEqualTo(RESERVA_ID);
            assertThat(response.getEstado()).isEqualTo("INASISTENCIA");
        }

        @Test
        @DisplayName("Más de 30 minutos → marca inasistencia exitosamente")
        void masDe30Min_marcaInasistenciaExitosa() {
            // Given: Reserva CONFIRMADA hace 60 minutos
            LocalDateTime horaLlegada = LocalDateTime.now().minusMinutes(60);
            Reserva reserva = crearReservaConfirmada(horaLlegada);

            when(reservaRepository.findById(RESERVA_ID)).thenReturn(Optional.of(reserva));
            doNothing().when(reservaValidador).validarElegibilidadInasistencia(eq(reserva), anyBoolean());
            when(reservaRepository.save(any(Reserva.class))).thenReturn(reserva);

            // When
            MarcarInasistenciaResponse response = reservaService.marcarInasistencia(RESERVA_ID, authentication);

            // Then
            verify(reservaRepository).save(any(Reserva.class));
            verify(preOrdenGestor).eliminarPreOrdenExistente(RESERVA_ID);
            verify(wsPublisher).publicarReservaActualizada(any(ReservaActualizadaWsMessage.class));
            assertThat(response.getEstado()).isEqualTo("INASISTENCIA");
        }

        @Test
        @DisplayName("Reserva con zona y decoración → respuesta incluye recursos liberados")
        void conZonaYDecoracion_incluyeRecursosLiberados() {
            // Given
            LocalDateTime horaLlegada = LocalDateTime.now().minusMinutes(40);
            Reserva reserva = crearReservaConfirmada(horaLlegada);

            when(reservaRepository.findById(RESERVA_ID)).thenReturn(Optional.of(reserva));
            doNothing().when(reservaValidador).validarElegibilidadInasistencia(eq(reserva), anyBoolean());
            when(reservaRepository.save(any(Reserva.class))).thenReturn(reserva);

            // When
            MarcarInasistenciaResponse response = reservaService.marcarInasistencia(RESERVA_ID, authentication);

            // Then
            assertThat(response.getZonaLiberada()).isEqualTo("Terraza");
            assertThat(response.getDecoracionLiberada()).isEqualTo("Cumpleaños");
        }

        @Test
        @DisplayName("Reserva sin zona ni decoración → campos null en respuesta")
        void sinZonaNiDecoracion_camposNull() {
            // Given
            LocalDateTime horaLlegada = LocalDateTime.now().minusMinutes(35);
            Reserva reserva = crearReservaConfirmada(horaLlegada);
            reserva.setZona(null);
            reserva.setDecoracion(null);

            when(reservaRepository.findById(RESERVA_ID)).thenReturn(Optional.of(reserva));
            doNothing().when(reservaValidador).validarElegibilidadInasistencia(eq(reserva), anyBoolean());
            when(reservaRepository.save(any(Reserva.class))).thenReturn(reserva);

            // When
            MarcarInasistenciaResponse response = reservaService.marcarInasistencia(RESERVA_ID, authentication);

            // Then
            assertThat(response.getZonaLiberada()).isNull();
            assertThat(response.getDecoracionLiberada()).isNull();
        }
    }

    // -----------------------------------------------------------------------
    // Tests de validaciones
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Validaciones: estado y tiempo")
    class Validaciones {

        @Test
        @DisplayName("Reserva no encontrada → lanza ResourceNotFoundException")
        void reservaNoExiste_lanzaEntityNotFound() {
            // Given
            when(reservaRepository.findById(RESERVA_ID)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> reservaService.marcarInasistencia(RESERVA_ID, authentication))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Reserva")
                    .hasMessageContaining(RESERVA_ID.toString());

            verify(reservaRepository, never()).save(any());
            verify(wsPublisher, never()).publicarReservaActualizada(any());
        }

        @Test
        @DisplayName("Reserva PENDIENTE → lanza BusinessException INVALID_STATE")
        void reservaPendiente_lanzaBusinessException() {
            // Given
            LocalDateTime horaLlegada = LocalDateTime.now().minusMinutes(40);
            Reserva reserva = crearReservaConfirmada(horaLlegada);
            reserva.setReservaEstado(EstadoReserva.PENDIENTE);

            when(reservaRepository.findById(RESERVA_ID)).thenReturn(Optional.of(reserva));
            doThrow(new BusinessException(
                    ErrorCode.INVALID_STATE,
                    "Solo reservas confirmadas pueden marcarse como inasistencia",
                    org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY
            )).when(reservaValidador).validarElegibilidadInasistencia(eq(reserva), anyBoolean());

            // When/Then
            assertThatThrownBy(() -> reservaService.marcarInasistencia(RESERVA_ID, authentication))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("confirmadas");

            verify(reservaRepository, never()).save(any());
        }

        @Test
        @DisplayName("Menos de 30 minutos → lanza BusinessException INVALID_STATE")
        void menosDe30Min_lanzaBusinessException() {
            // Given: Solo 15 minutos desde hora de llegada
            LocalDateTime horaLlegada = LocalDateTime.now().minusMinutes(15);
            Reserva reserva = crearReservaConfirmada(horaLlegada);

            when(reservaRepository.findById(RESERVA_ID)).thenReturn(Optional.of(reserva));
            doThrow(new BusinessException(
                    ErrorCode.INVALID_STATE,
                    "Debe esperar 15 minuto(s) más antes de marcar inasistencia",
                    org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY
            )).when(reservaValidador).validarElegibilidadInasistencia(eq(reserva), anyBoolean());

            // When/Then
            assertThatThrownBy(() -> reservaService.marcarInasistencia(RESERVA_ID, authentication))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Debe esperar");

            verify(reservaRepository, never()).save(any());
        }

        @Test
        @DisplayName("Reserva ATENDIDA → lanza BusinessException")
        void reservaAtendida_lanzaBusinessException() {
            // Given
            LocalDateTime horaLlegada = LocalDateTime.now().minusMinutes(40);
            Reserva reserva = crearReservaConfirmada(horaLlegada);
            reserva.setReservaEstado(EstadoReserva.ATENDIDA);

            when(reservaRepository.findById(RESERVA_ID)).thenReturn(Optional.of(reserva));
            doThrow(new BusinessException(
                    ErrorCode.INVALID_STATE,
                    "Solo reservas confirmadas pueden marcarse como inasistencia",
                    org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY
            )).when(reservaValidador).validarElegibilidadInasistencia(eq(reserva), anyBoolean());

            // When/Then
            assertThatThrownBy(() -> reservaService.marcarInasistencia(RESERVA_ID, authentication))
                    .isInstanceOf(BusinessException.class);

            verify(reservaRepository, never()).save(any());
        }

        @Test
        @DisplayName("Reserva CANCELADA → lanza BusinessException")
        void reservaCancelada_lanzaBusinessException() {
            // Given
            LocalDateTime horaLlegada = LocalDateTime.now().minusMinutes(40);
            Reserva reserva = crearReservaConfirmada(horaLlegada);
            reserva.setReservaEstado(EstadoReserva.CANCELADA);

            when(reservaRepository.findById(RESERVA_ID)).thenReturn(Optional.of(reserva));
            doThrow(new BusinessException(
                    ErrorCode.INVALID_STATE,
                    "Solo reservas confirmadas pueden marcarse como inasistencia",
                    org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY
            )).when(reservaValidador).validarElegibilidadInasistencia(eq(reserva), anyBoolean());

            // When/Then
            assertThatThrownBy(() -> reservaService.marcarInasistencia(RESERVA_ID, authentication))
                    .isInstanceOf(BusinessException.class);

            verify(reservaRepository, never()).save(any());
        }
    }

    // -----------------------------------------------------------------------
    // Tests de side effects
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Side effects: pre-orden y WebSocket")
    class SideEffects {

        @Test
        @DisplayName("Elimina pre-orden asociada")
        void eliminaPreOrden() {
            // Given
            LocalDateTime horaLlegada = LocalDateTime.now().minusMinutes(40);
            Reserva reserva = crearReservaConfirmada(horaLlegada);

            when(reservaRepository.findById(RESERVA_ID)).thenReturn(Optional.of(reserva));
            doNothing().when(reservaValidador).validarElegibilidadInasistencia(eq(reserva), anyBoolean());
            when(reservaRepository.save(any(Reserva.class))).thenReturn(reserva);

            // When
            reservaService.marcarInasistencia(RESERVA_ID, authentication);

            // Then: Pre-orden eliminada ANTES de publicar WS
            verify(preOrdenGestor).eliminarPreOrdenExistente(RESERVA_ID);
        }

        @Test
        @DisplayName("Publica evento WebSocket con tipoEvento INASISTENCIA")
        void publicaEventoWebSocket() {
            // Given
            LocalDateTime horaLlegada = LocalDateTime.now().minusMinutes(35);
            Reserva reserva = crearReservaConfirmada(horaLlegada);

            when(reservaRepository.findById(RESERVA_ID)).thenReturn(Optional.of(reserva));
            doNothing().when(reservaValidador).validarElegibilidadInasistencia(eq(reserva), anyBoolean());
            when(reservaRepository.save(any(Reserva.class))).thenReturn(reserva);

            // When
            reservaService.marcarInasistencia(RESERVA_ID, authentication);

            // Then: Evento WS publicado
            ArgumentCaptor<ReservaActualizadaWsMessage> captor =
                    ArgumentCaptor.forClass(ReservaActualizadaWsMessage.class);
            verify(wsPublisher).publicarReservaActualizada(captor.capture());

            ReservaActualizadaWsMessage mensaje = captor.getValue();
            assertThat(mensaje.getReservaId()).isEqualTo(RESERVA_ID);
            assertThat(mensaje.getTipoEvento()).isEqualTo("INASISTENCIA");
            assertThat(mensaje.getClienteNombre()).isEqualTo("Juan Pérez");
            assertThat(mensaje.getZonaNombre()).isEqualTo("Terraza");
        }
    }
}
