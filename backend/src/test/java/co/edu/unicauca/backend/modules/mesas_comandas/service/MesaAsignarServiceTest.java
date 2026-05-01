package co.edu.unicauca.backend.modules.mesas_comandas.service;

import co.edu.unicauca.backend.modules.mesas_comandas.dto.request.AsignarMesaRequest;
import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.MesaAsignadaResponse;
import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.ZonaDisponibleMesaResponse;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Comanda;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Mesa;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Visita;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Zona;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ComandaRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.MesaRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.VisitaRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ZonaRepository;
import co.edu.unicauca.backend.modules.notificaciones.dto.ws.ReservaActualizadaWsMessage;
import co.edu.unicauca.backend.modules.notificaciones.dto.ws.VisitaActualizadaWsMessage;
import co.edu.unicauca.backend.modules.notificaciones.service.NotificacionWsPublisher;
import co.edu.unicauca.backend.modules.reservas.entity.Reserva;
import co.edu.unicauca.backend.modules.reservas.repository.ReservaRepository;
import co.edu.unicauca.backend.modules.auth.entity.Usuario;
import co.edu.unicauca.backend.modules.usuarios.entity.Cliente;
import co.edu.unicauca.backend.modules.usuarios.entity.Empleado;
import co.edu.unicauca.backend.modules.usuarios.repository.EmpleadoRepository;
import co.edu.unicauca.backend.shared.enums.EstadoComanda;
import co.edu.unicauca.backend.shared.enums.EstadoMesa;
import co.edu.unicauca.backend.shared.enums.EstadoReserva;
import co.edu.unicauca.backend.shared.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para {@link MesaAsignarService}.
 *
 * <p>Cubre:
 * <ul>
 *   <li>{@link MesaAsignarService#asignarMesa}: validaciones, flujo walk-in, flujo reserva, WebSocket</li>
 *   <li>{@link MesaAsignarService#listarZonasDisponibles}: cálculo de disponibilidad</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MesaAsignarService")
class MesaAsignarServiceTest {

    @Mock MesaValidador mesaValidador;
    @Mock MesaRepository mesaRepository;
    @Mock VisitaRepository visitaRepository;
    @Mock ZonaRepository zonaRepository;
    @Mock ReservaRepository reservaRepository;
    @Mock ComandaRepository comandaRepository;
    @Mock EmpleadoRepository empleadoRepository;
    @Mock MesaWsPublisher mesaWsPublisher;
    @Mock NotificacionWsPublisher notificacionWsPublisher;

    @InjectMocks MesaAsignarService mesaAsignarService;

    private static final String EMAIL_MESERO = "mesero@altoro.com";
    private static final Long ZONA_ID = 1L;
    private static final Long RESERVA_ID = 10L;
    private static final Long VISITA_ID = 100L;
    private static final Long COMANDA_ID = 200L;

    private AsignarMesaRequest requestWalkIn;
    private AsignarMesaRequest requestConReserva;
    private Zona zona;
    private Empleado mesero;
    private Reserva reserva;
    private Cliente cliente;
    private Visita visitaGuardada;
    private Mesa mesaGuardada;

    @BeforeEach
    void setUp() {
        // Request walk-in (sin reserva, sin cliente)
        requestWalkIn = new AsignarMesaRequest(
                "T-01",
                ZONA_ID,
                4,
                null,  // sin reserva
                "Notas de prueba"
        );

        // Request con reserva
        requestConReserva = new AsignarMesaRequest(
                "T-02",
                ZONA_ID,
                2,
                RESERVA_ID,
                null
        );

        // Zona
        zona = Zona.builder()
                .zonaId(ZONA_ID)
                .zonaNombre("Terraza")
                .zonaCapacidadPersonas(20)
                .build();

        // Mesero
        Usuario usuarioMesero = Usuario.builder()
                .usuarioId(50L)
                .usuarioEmail(EMAIL_MESERO)
                .build();
        mesero = Empleado.builder()
                .usuarioId(50L)
                .usuario(usuarioMesero)
                .empleadoNombre("Mesero Prueba")
                .build();

        // Cliente
        cliente = Cliente.builder()
                .usuarioId(60L)
                .clienteNombre("Cliente Prueba")
                .clientePuntos(5)
                .clientePuntosAcumulados(10)
                .build();

        // Reserva
        reserva = Reserva.builder()
                .reservaId(RESERVA_ID)
                .cliente(cliente)
                .zona(zona)
                .reservaEstado(EstadoReserva.CONFIRMADA)
                .reservaFechaHoraLlegada(LocalDateTime.now())
                .build();

        // Visita guardada
        visitaGuardada = Visita.builder()
                .visitaId(VISITA_ID)
                .cliente(null)  // por defecto walk-in
                .reserva(null)
                .visitaFechaHoraInicio(LocalDateTime.now())
                .build();

        // Mesa guardada
        mesaGuardada = Mesa.builder()
                .visitaId(VISITA_ID)
                .visita(visitaGuardada)
                .zona(zona)
                .mesero(mesero)
                .mesaIdentificador("T-01")
                .mesaNumeroPersonas(4)
                .mesaEstado(EstadoMesa.ESPERA)
                .mesaNotas("Notas de prueba")
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // asignarMesa() - Happy paths
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("asignarMesa() - Happy paths")
    class AsignarMesaHappyPaths {

        @Test
        @DisplayName("walk-in (sin reserva, sin cliente) → mesa creada, solo mesa map WS publicado")
        void walkIn_creaMesaYPublicaSoloMapaWs() {
            // Arrange
            doNothing().when(mesaValidador).validarHorarioAtencion();
            doNothing().when(mesaValidador).validarIdentificadorNoOcupado(anyString());
            doNothing().when(mesaValidador).validarZonaExiste(anyLong());
            when(zonaRepository.findById(ZONA_ID)).thenReturn(Optional.of(zona));
            when(empleadoRepository.findByUsuario_UsuarioEmail(EMAIL_MESERO)).thenReturn(Optional.of(mesero));
            when(visitaRepository.save(any(Visita.class))).thenReturn(visitaGuardada);
            when(mesaRepository.save(any(Mesa.class))).thenReturn(mesaGuardada);

            // Act
            MesaAsignadaResponse response = mesaAsignarService.asignarMesa(requestWalkIn, EMAIL_MESERO);

            // Assert - Respuesta
            assertThat(response).isNotNull();
            assertThat(response.getVisitaId()).isEqualTo(VISITA_ID);
            assertThat(response.getMesaIdentificador()).isEqualTo("T-01");
            assertThat(response.getZonaId()).isEqualTo(ZONA_ID);
            assertThat(response.getZonaNombre()).isEqualTo("Terraza");
            assertThat(response.getNumeroPersonas()).isEqualTo(4);
            assertThat(response.getEstadoMesa()).isEqualTo("ESPERA");
            assertThat(response.getEmailMesero()).isEqualTo(EMAIL_MESERO);
            assertThat(response.getReservaId()).isNull();

            // Assert - WebSocket: solo mesa map
            verify(mesaWsPublisher).publicarActualizacionMesa(VISITA_ID, MesaWsPublisher.TipoEventoMesa.CREAR);
            verify(notificacionWsPublisher, never()).publicarVisitaActualizada(anyLong(), any());
            verify(notificacionWsPublisher, never()).publicarReservaActualizada(any());
        }

        @Test
        @DisplayName("walk-in llama todas las validaciones en orden correcto")
        void walkIn_llamaValidacionesEnOrden() {
            // Arrange
            doNothing().when(mesaValidador).validarHorarioAtencion();
            doNothing().when(mesaValidador).validarIdentificadorNoOcupado(anyString());
            doNothing().when(mesaValidador).validarZonaExiste(anyLong());
            when(zonaRepository.findById(ZONA_ID)).thenReturn(Optional.of(zona));
            when(empleadoRepository.findByUsuario_UsuarioEmail(EMAIL_MESERO)).thenReturn(Optional.of(mesero));
            when(visitaRepository.save(any(Visita.class))).thenReturn(visitaGuardada);
            when(mesaRepository.save(any(Mesa.class))).thenReturn(mesaGuardada);

            // Act
            mesaAsignarService.asignarMesa(requestWalkIn, EMAIL_MESERO);

            // Assert - Verificar orden de validaciones
            var inOrder = inOrder(mesaValidador, zonaRepository, empleadoRepository);
            inOrder.verify(mesaValidador).validarHorarioAtencion();
            inOrder.verify(mesaValidador).validarIdentificadorNoOcupado("T-01");
            inOrder.verify(mesaValidador).validarZonaExiste(ZONA_ID);
            inOrder.verify(zonaRepository).findById(ZONA_ID);
            inOrder.verify(empleadoRepository).findByUsuario_UsuarioEmail(EMAIL_MESERO);
        }

        @Test
        @DisplayName("reserva happy path → mesa creada, comanda→BORRADOR, reserva→ATENDIDA, 3 WS publicados")
        void reserva_creaMesaProcesaReservaYPublica3Ws() {
            // Arrange - Setup con reserva
            Visita visitaConCliente = Visita.builder()
                    .visitaId(VISITA_ID)
                    .cliente(cliente)
                    .reserva(reserva)
                    .visitaFechaHoraInicio(LocalDateTime.now())
                    .build();

            Comanda comandaPreReserva = Comanda.builder()
                    .comandaId(COMANDA_ID)
                    .reserva(reserva)
                    .comandaEstado(EstadoComanda.PRE_RESERVA)
                    .build();

            doNothing().when(mesaValidador).validarHorarioAtencion();
            doNothing().when(mesaValidador).validarIdentificadorNoOcupado(anyString());
            doNothing().when(mesaValidador).validarZonaExiste(anyLong());
            when(mesaValidador.validarReservaParaAsignacion(RESERVA_ID)).thenReturn(reserva);
            when(zonaRepository.findById(ZONA_ID)).thenReturn(Optional.of(zona));
            when(empleadoRepository.findByUsuario_UsuarioEmail(EMAIL_MESERO)).thenReturn(Optional.of(mesero));
            when(visitaRepository.save(any(Visita.class))).thenReturn(visitaConCliente);
            when(mesaRepository.save(any(Mesa.class))).thenReturn(mesaGuardada);
            when(comandaRepository.findByReserva_ReservaIdAndComandaEstado(RESERVA_ID, EstadoComanda.PRE_RESERVA))
                    .thenReturn(Optional.of(comandaPreReserva));

            // Act
            MesaAsignadaResponse response = mesaAsignarService.asignarMesa(requestConReserva, EMAIL_MESERO);

            // Assert - Respuesta
            assertThat(response.getReservaId()).isEqualTo(RESERVA_ID);

            // Assert - Comanda procesada
            ArgumentCaptor<Comanda> comandaCaptor = ArgumentCaptor.forClass(Comanda.class);
            verify(comandaRepository).save(comandaCaptor.capture());
            assertThat(comandaCaptor.getValue().getComandaEstado()).isEqualTo(EstadoComanda.BORRADOR);
            assertThat(comandaCaptor.getValue().getVisita()).isEqualTo(visitaConCliente);

            // Assert - Reserva actualizada
            ArgumentCaptor<Reserva> reservaCaptor = ArgumentCaptor.forClass(Reserva.class);
            verify(reservaRepository).save(reservaCaptor.capture());
            assertThat(reservaCaptor.getValue().getReservaEstado()).isEqualTo(EstadoReserva.ATENDIDA);

            // Assert - WebSocket: 3 publicaciones
            verify(mesaWsPublisher).publicarActualizacionMesa(VISITA_ID, MesaWsPublisher.TipoEventoMesa.CREAR);
            verify(notificacionWsPublisher).publicarVisitaActualizada(eq(VISITA_ID), any(VisitaActualizadaWsMessage.class));
            verify(notificacionWsPublisher).publicarReservaActualizada(any(ReservaActualizadaWsMessage.class));
        }

        @Test
        @DisplayName("reserva procesa comanda PRE_RESERVA → BORRADOR y vincula a visita")
        void reserva_procesaComandaPreReserva() {
            // Arrange
            Visita visitaConCliente = Visita.builder()
                    .visitaId(VISITA_ID)
                    .cliente(cliente)
                    .reserva(reserva)
                    .visitaFechaHoraInicio(LocalDateTime.now())
                    .build();

            Comanda comandaPreReserva = Comanda.builder()
                    .comandaId(COMANDA_ID)
                    .reserva(reserva)
                    .comandaEstado(EstadoComanda.PRE_RESERVA)
                    .visita(null)  // Antes: sin visita
                    .build();

            setupMocksForReserva(visitaConCliente);
            when(comandaRepository.findByReserva_ReservaIdAndComandaEstado(RESERVA_ID, EstadoComanda.PRE_RESERVA))
                    .thenReturn(Optional.of(comandaPreReserva));

            // Act
            mesaAsignarService.asignarMesa(requestConReserva, EMAIL_MESERO);

            // Assert
            ArgumentCaptor<Comanda> captor = ArgumentCaptor.forClass(Comanda.class);
            verify(comandaRepository).save(captor.capture());
            Comanda comandaGuardada = captor.getValue();

            assertThat(comandaGuardada.getComandaEstado()).isEqualTo(EstadoComanda.BORRADOR);
            assertThat(comandaGuardada.getVisita()).isEqualTo(visitaConCliente);
        }

        @Test
        @DisplayName("reserva sin comanda (no comanda found) → reserva aún actualizada a ATENDIDA")
        void reserva_sinComanda_actualizaReserva() {
            // Arrange
            Visita visitaConCliente = Visita.builder()
                    .visitaId(VISITA_ID)
                    .cliente(cliente)
                    .reserva(reserva)
                    .visitaFechaHoraInicio(LocalDateTime.now())
                    .build();

            setupMocksForReserva(visitaConCliente);
            when(comandaRepository.findByReserva_ReservaIdAndComandaEstado(RESERVA_ID, EstadoComanda.PRE_RESERVA))
                    .thenReturn(Optional.empty());  // Sin comanda

            // Act
            mesaAsignarService.asignarMesa(requestConReserva, EMAIL_MESERO);

            // Assert - No se guarda comanda
            verify(comandaRepository, never()).save(any(Comanda.class));

            // Assert - Reserva SÍ se actualiza a ATENDIDA
            ArgumentCaptor<Reserva> captor = ArgumentCaptor.forClass(Reserva.class);
            verify(reservaRepository).save(captor.capture());
            assertThat(captor.getValue().getReservaEstado()).isEqualTo(EstadoReserva.ATENDIDA);
        }

        @Test
        @DisplayName("reserva cambia estado a ATENDIDA")
        void reserva_cambiaEstadoAAtendida() {
            // Arrange
            Visita visitaConCliente = Visita.builder()
                    .visitaId(VISITA_ID)
                    .cliente(cliente)
                    .reserva(reserva)
                    .visitaFechaHoraInicio(LocalDateTime.now())
                    .build();

            setupMocksForReserva(visitaConCliente);
            when(comandaRepository.findByReserva_ReservaIdAndComandaEstado(RESERVA_ID, EstadoComanda.PRE_RESERVA))
                    .thenReturn(Optional.empty());

            // Estado inicial
            assertThat(reserva.getReservaEstado()).isEqualTo(EstadoReserva.CONFIRMADA);

            // Act
            mesaAsignarService.asignarMesa(requestConReserva, EMAIL_MESERO);

            // Assert
            ArgumentCaptor<Reserva> captor = ArgumentCaptor.forClass(Reserva.class);
            verify(reservaRepository).save(captor.capture());
            assertThat(captor.getValue().getReservaEstado()).isEqualTo(EstadoReserva.ATENDIDA);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // asignarMesa() - WebSocket publications
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("asignarMesa() - WebSocket publications")
    class AsignarMesaWebSocket {

        @Test
        @DisplayName("SIEMPRE publica mesa map (walk-in y reserva)")
        void siemprePublicaMesaMap() {
            // Arrange - Walk-in
            setupMocksForWalkIn();

            // Act
            mesaAsignarService.asignarMesa(requestWalkIn, EMAIL_MESERO);

            // Assert
            verify(mesaWsPublisher).publicarActualizacionMesa(VISITA_ID, MesaWsPublisher.TipoEventoMesa.CREAR);
        }

        @Test
        @DisplayName("publica visit state si cliente existe (NEW TEST)")
        void publicaVisitStateConCliente() {
            // Arrange - Reserva con cliente
            Visita visitaConCliente = Visita.builder()
                    .visitaId(VISITA_ID)
                    .cliente(cliente)
                    .reserva(reserva)
                    .visitaFechaHoraInicio(LocalDateTime.now())
                    .build();

            setupMocksForReserva(visitaConCliente);
            when(comandaRepository.findByReserva_ReservaIdAndComandaEstado(RESERVA_ID, EstadoComanda.PRE_RESERVA))
                    .thenReturn(Optional.empty());

            // Act
            mesaAsignarService.asignarMesa(requestConReserva, EMAIL_MESERO);

            // Assert - Verificar que se publicó visit state
            ArgumentCaptor<VisitaActualizadaWsMessage> captor = ArgumentCaptor.forClass(VisitaActualizadaWsMessage.class);
            verify(notificacionWsPublisher).publicarVisitaActualizada(eq(VISITA_ID), captor.capture());

            VisitaActualizadaWsMessage mensaje = captor.getValue();
            assertThat(mensaje.getVisitaId()).isEqualTo(VISITA_ID);
            assertThat(mensaje.getItems()).isEmpty();  // Lista vacía - aún no hay items
            assertThat(mensaje.getTotal()).isEqualByComparingTo(BigDecimal.ZERO);  // Total 0
        }

        @Test
        @DisplayName("NO publica visit state si cliente es null (walk-in)")
        void noPublicaVisitStateSinCliente() {
            // Arrange - Walk-in sin cliente
            setupMocksForWalkIn();

            // Act
            mesaAsignarService.asignarMesa(requestWalkIn, EMAIL_MESERO);

            // Assert
            verify(notificacionWsPublisher, never()).publicarVisitaActualizada(anyLong(), any());
        }

        @Test
        @DisplayName("publica reservation update si reserva existe")
        void publicaReservationUpdateConReserva() {
            // Arrange
            Visita visitaConCliente = Visita.builder()
                    .visitaId(VISITA_ID)
                    .cliente(cliente)
                    .reserva(reserva)
                    .visitaFechaHoraInicio(LocalDateTime.now())
                    .build();

            setupMocksForReserva(visitaConCliente);
            when(comandaRepository.findByReserva_ReservaIdAndComandaEstado(RESERVA_ID, EstadoComanda.PRE_RESERVA))
                    .thenReturn(Optional.empty());

            // Act
            mesaAsignarService.asignarMesa(requestConReserva, EMAIL_MESERO);

            // Assert
            ArgumentCaptor<ReservaActualizadaWsMessage> captor = ArgumentCaptor.forClass(ReservaActualizadaWsMessage.class);
            verify(notificacionWsPublisher).publicarReservaActualizada(captor.capture());

            ReservaActualizadaWsMessage mensaje = captor.getValue();
            assertThat(mensaje.getReservaId()).isEqualTo(RESERVA_ID);
            assertThat(mensaje.getTipoEvento()).isEqualTo("ATENDIDA");
            assertThat(mensaje.getClienteNombre()).isEqualTo("Cliente Prueba");
            assertThat(mensaje.getHoraLlegada()).isNotNull();
            assertThat(mensaje.getZonaNombre()).isEqualTo("Terraza");
        }

        @Test
        @DisplayName("NO publica reservation update si reserva es null (walk-in)")
        void noPublicaReservationUpdateSinReserva() {
            // Arrange
            setupMocksForWalkIn();

            // Act
            mesaAsignarService.asignarMesa(requestWalkIn, EMAIL_MESERO);

            // Assert
            verify(notificacionWsPublisher, never()).publicarReservaActualizada(any());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // asignarMesa() - Validaciones y excepciones
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("asignarMesa() - Validaciones y excepciones")
    class AsignarMesaValidaciones {

        @Test
        @DisplayName("zona no encontrada → lanza ENTITY_NOT_FOUND")
        void zonaNoEncontrada_lanzaException() {
            // Arrange
            doNothing().when(mesaValidador).validarHorarioAtencion();
            doNothing().when(mesaValidador).validarIdentificadorNoOcupado(anyString());
            doNothing().when(mesaValidador).validarZonaExiste(anyLong());
            when(zonaRepository.findById(ZONA_ID)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> mesaAsignarService.asignarMesa(requestWalkIn, EMAIL_MESERO))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Zona no encontrada");
        }

        @Test
        @DisplayName("mesero no encontrado → lanza ENTITY_NOT_FOUND")
        void meseroNoEncontrado_lanzaException() {
            // Arrange
            doNothing().when(mesaValidador).validarHorarioAtencion();
            doNothing().when(mesaValidador).validarIdentificadorNoOcupado(anyString());
            doNothing().when(mesaValidador).validarZonaExiste(anyLong());
            when(zonaRepository.findById(ZONA_ID)).thenReturn(Optional.of(zona));
            when(empleadoRepository.findByUsuario_UsuarioEmail(EMAIL_MESERO)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> mesaAsignarService.asignarMesa(requestWalkIn, EMAIL_MESERO))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Mesero no encontrado");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // listarZonasDisponibles()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("listarZonasDisponibles()")
    class ListarZonasDisponibles {

        @Test
        @DisplayName("retorna todas las zonas con disponibilidad > 0")
        void retornaZonasConDisponibilidad() {
            // Arrange
            Zona terraza = Zona.builder()
                    .zonaId(1L)
                    .zonaNombre("Terraza")
                    .zonaCapacidadPersonas(20)
                    .build();

            Zona interior = Zona.builder()
                    .zonaId(2L)
                    .zonaNombre("Interior")
                    .zonaCapacidadPersonas(30)
                    .build();

            Zona vip = Zona.builder()
                    .zonaId(3L)
                    .zonaNombre("VIP")
                    .zonaCapacidadPersonas(10)
                    .build();

            when(zonaRepository.findAll()).thenReturn(List.of(terraza, interior, vip));

            // Ocupación: Terraza=8, Interior=30 (lleno), VIP=0
            Object[] ocupacionTerraza = new Object[]{1L, 8};
            Object[] ocupacionInterior = new Object[]{2L, 30};
            when(mesaRepository.sumPersonasPorZonaActiva()).thenReturn(List.<Object[]>of(ocupacionTerraza, ocupacionInterior));

            // Act
            List<ZonaDisponibleMesaResponse> result = mesaAsignarService.listarZonasDisponibles();

            // Assert - Solo Terraza y VIP (disponibilidad > 0)
            assertThat(result).hasSize(2);

            ZonaDisponibleMesaResponse terrazaResp = result.stream()
                    .filter(z -> z.getZonaId().equals(1L))
                    .findFirst()
                    .orElseThrow();
            assertThat(terrazaResp.getZonaNombre()).isEqualTo("Terraza");
            assertThat(terrazaResp.getCapacidadTotal()).isEqualTo(20);
            assertThat(terrazaResp.getPersonasOcupadas()).isEqualTo(8);
            assertThat(terrazaResp.getDisponibilidad()).isEqualTo(12);

            ZonaDisponibleMesaResponse vipResp = result.stream()
                    .filter(z -> z.getZonaId().equals(3L))
                    .findFirst()
                    .orElseThrow();
            assertThat(vipResp.getZonaNombre()).isEqualTo("VIP");
            assertThat(vipResp.getCapacidadTotal()).isEqualTo(10);
            assertThat(vipResp.getPersonasOcupadas()).isEqualTo(0);
            assertThat(vipResp.getDisponibilidad()).isEqualTo(10);

            // Interior NO debe estar en el resultado
            assertThat(result).noneMatch(z -> z.getZonaId().equals(2L));
        }

        @Test
        @DisplayName("calcula disponibilidad correctamente (capacidad - ocupadas)")
        void calculaDisponibilidadCorrecta() {
            // Arrange
            Zona zona = Zona.builder()
                    .zonaId(1L)
                    .zonaNombre("Test")
                    .zonaCapacidadPersonas(50)
                    .build();

            when(zonaRepository.findAll()).thenReturn(List.of(zona));

            // Ocupación: 35 personas
            Object[] ocupacion = new Object[]{1L, 35};
            when(mesaRepository.sumPersonasPorZonaActiva()).thenReturn(List.<Object[]>of(ocupacion));

            // Act
            List<ZonaDisponibleMesaResponse> result = mesaAsignarService.listarZonasDisponibles();

            // Assert
            assertThat(result).hasSize(1);
            ZonaDisponibleMesaResponse zona1 = result.get(0);
            assertThat(zona1.getCapacidadTotal()).isEqualTo(50);
            assertThat(zona1.getPersonasOcupadas()).isEqualTo(35);
            assertThat(zona1.getDisponibilidad()).isEqualTo(15);  // 50 - 35
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helper methods
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Configura mocks para flujo walk-in estándar.
     */
    private void setupMocksForWalkIn() {
        doNothing().when(mesaValidador).validarHorarioAtencion();
        doNothing().when(mesaValidador).validarIdentificadorNoOcupado(anyString());
        doNothing().when(mesaValidador).validarZonaExiste(anyLong());
        when(zonaRepository.findById(ZONA_ID)).thenReturn(Optional.of(zona));
        when(empleadoRepository.findByUsuario_UsuarioEmail(EMAIL_MESERO)).thenReturn(Optional.of(mesero));
        when(visitaRepository.save(any(Visita.class))).thenReturn(visitaGuardada);
        when(mesaRepository.save(any(Mesa.class))).thenReturn(mesaGuardada);
    }

    /**
     * Configura mocks para flujo de reserva.
     *
     * @param visitaConCliente visita con cliente y reserva asociados
     */
    private void setupMocksForReserva(Visita visitaConCliente) {
        doNothing().when(mesaValidador).validarHorarioAtencion();
        doNothing().when(mesaValidador).validarIdentificadorNoOcupado(anyString());
        doNothing().when(mesaValidador).validarZonaExiste(anyLong());
        when(mesaValidador.validarReservaParaAsignacion(RESERVA_ID)).thenReturn(reserva);
        when(zonaRepository.findById(ZONA_ID)).thenReturn(Optional.of(zona));
        when(empleadoRepository.findByUsuario_UsuarioEmail(EMAIL_MESERO)).thenReturn(Optional.of(mesero));
        when(visitaRepository.save(any(Visita.class))).thenReturn(visitaConCliente);
        when(mesaRepository.save(any(Mesa.class))).thenReturn(mesaGuardada);
    }
}
