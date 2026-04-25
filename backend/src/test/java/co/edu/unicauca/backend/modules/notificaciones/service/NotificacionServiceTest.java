package co.edu.unicauca.backend.modules.notificaciones.service;

import co.edu.unicauca.backend.modules.auth.entity.Usuario;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Mesa;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Visita;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.MesaRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.VisitaRepository;
import co.edu.unicauca.backend.modules.notificaciones.dto.response.NotificacionAsistenciaResponse;
import co.edu.unicauca.backend.modules.notificaciones.dto.ws.AsistenciaAtendidaWsMessage;
import co.edu.unicauca.backend.modules.notificaciones.dto.ws.AsistenciaSolicitadaWsMessage;
import co.edu.unicauca.backend.modules.notificaciones.entity.Notificacion;
import co.edu.unicauca.backend.modules.notificaciones.repository.NotificacionRepository;
import co.edu.unicauca.backend.modules.usuarios.entity.Cliente;
import co.edu.unicauca.backend.modules.usuarios.entity.Empleado;
import co.edu.unicauca.backend.shared.enums.EstadoNotificacion;
import co.edu.unicauca.backend.shared.enums.TipoNotificacion;
import co.edu.unicauca.backend.shared.exception.BusinessException;
import co.edu.unicauca.backend.shared.exception.ResourceNotFoundException;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("NotificacionService")
class NotificacionServiceTest {

    @Mock VisitaRepository visitaRepository;
    @Mock MesaRepository mesaRepository;
    @Mock NotificacionRepository notificacionRepository;
    @Mock NotificacionWsPublisher wsPublisher;

    @InjectMocks NotificacionService notificacionService;

    private static final Long VISITA_ID = 10L;
    private static final String EMAIL = "cliente@test.com";

    private Visita visitaConCliente() {
        Usuario usuario = new Usuario();
        usuario.setUsuarioEmail(EMAIL);
        Cliente cliente = Cliente.builder().usuarioId(1L).clienteNombre("Juan").build();
        cliente.setUsuario(usuario);
        return Visita.builder().visitaId(VISITA_ID).cliente(cliente).build();
    }

    private Mesa mesaConMesero() {
        Empleado mesero = Empleado.builder().usuarioId(5L).build();
        return Mesa.builder().visitaId(VISITA_ID).mesaIdentificador("T-01").mesero(mesero).build();
    }

    @Nested
    @DisplayName("solicitarAsistencia")
    class SolicitarAsistencia {

        @Test
        @DisplayName("crea notificación ACTIVA y publica broadcast WS")
        void creaNotificacionYPublicaBroadcast() {
            Visita visita = visitaConCliente();
            Mesa mesa = mesaConMesero();

            when(visitaRepository.findById(VISITA_ID)).thenReturn(Optional.of(visita));
            when(mesaRepository.findByVisita_VisitaId(VISITA_ID)).thenReturn(Optional.of(mesa));
            when(notificacionRepository.findFirstByMesa_VisitaIdAndNotificacionTipoAndNotificacionEstado(
                    VISITA_ID, TipoNotificacion.ATENCION, EstadoNotificacion.ACTIVA))
                    .thenReturn(Optional.empty());
            Notificacion saved = Notificacion.builder().notificacionId(50L)
                    .notificacionEstado(EstadoNotificacion.ACTIVA)
                    .notificacionTipo(TipoNotificacion.ATENCION)
                    .mesa(mesa).empleado(mesa.getMesero()).build();
            when(notificacionRepository.save(any())).thenReturn(saved);

            NotificacionAsistenciaResponse res = notificacionService.solicitarAsistencia(VISITA_ID, EMAIL);

            assertThat(res.getNotificacionId()).isEqualTo(50L);
            assertThat(res.getEstado()).isEqualTo("ACTIVA");

            ArgumentCaptor<AsistenciaSolicitadaWsMessage> captor =
                    ArgumentCaptor.forClass(AsistenciaSolicitadaWsMessage.class);
            verify(wsPublisher).publicarAsistenciaSolicitada(captor.capture());
            assertThat(captor.getValue().getVisitaId()).isEqualTo(VISITA_ID);
            assertThat(captor.getValue().getMesaIdentificador()).isEqualTo("T-01");
        }

        @Test
        @DisplayName("lanza BusinessException si ya hay solicitud activa")
        void lanzaExcepcionSiYaHaySolicitudActiva() {
            Visita visita = visitaConCliente();
            Mesa mesa = mesaConMesero();
            Notificacion activa = Notificacion.builder().notificacionId(1L)
                    .notificacionEstado(EstadoNotificacion.ACTIVA).build();

            when(visitaRepository.findById(VISITA_ID)).thenReturn(Optional.of(visita));
            when(mesaRepository.findByVisita_VisitaId(VISITA_ID)).thenReturn(Optional.of(mesa));
            when(notificacionRepository.findFirstByMesa_VisitaIdAndNotificacionTipoAndNotificacionEstado(
                    VISITA_ID, TipoNotificacion.ATENCION, EstadoNotificacion.ACTIVA))
                    .thenReturn(Optional.of(activa));

            assertThatThrownBy(() -> notificacionService.solicitarAsistencia(VISITA_ID, EMAIL))
                    .isInstanceOf(BusinessException.class);
            verify(notificacionRepository, never()).save(any());
        }

        @Test
        @DisplayName("lanza BusinessException si el cliente no es dueño de la visita")
        void lanzaExcepcionSiClienteNoEsDueno() {
            Visita visita = visitaConCliente();
            when(visitaRepository.findById(VISITA_ID)).thenReturn(Optional.of(visita));

            assertThatThrownBy(() -> notificacionService.solicitarAsistencia(VISITA_ID, "otro@test.com"))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("lanza BusinessException si la visita no tiene mesa asignada")
        void lanzaExcepcionSinMesaAsignada() {
            Visita visita = visitaConCliente();

            when(visitaRepository.findById(VISITA_ID)).thenReturn(Optional.of(visita));
            when(mesaRepository.findByVisita_VisitaId(VISITA_ID)).thenReturn(Optional.empty());
            when(notificacionRepository.findFirstByMesa_VisitaIdAndNotificacionTipoAndNotificacionEstado(
                    VISITA_ID, TipoNotificacion.ATENCION, EstadoNotificacion.ACTIVA))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> notificacionService.solicitarAsistencia(VISITA_ID, EMAIL))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("atenderAsistencia")
    class AtenderAsistencia {

        @Test
        @DisplayName("marca notificación como ATENDIDA y publica WS al cliente")
        void marcaAtendidaYPublicaWs() {
            Mesa mesa = mesaConMesero();
            Notificacion notif = Notificacion.builder()
                    .notificacionId(50L)
                    .notificacionEstado(EstadoNotificacion.ACTIVA)
                    .notificacionTipo(TipoNotificacion.ATENCION)
                    .mesa(mesa).empleado(mesa.getMesero()).build();

            when(notificacionRepository.findById(50L)).thenReturn(Optional.of(notif));
            when(notificacionRepository.save(any())).thenReturn(notif);

            notificacionService.atenderAsistencia(50L, "mesero@test.com");

            assertThat(notif.getNotificacionEstado()).isEqualTo(EstadoNotificacion.ATENDIDA);

            ArgumentCaptor<AsistenciaAtendidaWsMessage> captor =
                    ArgumentCaptor.forClass(AsistenciaAtendidaWsMessage.class);
            verify(wsPublisher).publicarAsistenciaAtendida(eq(VISITA_ID), captor.capture());
            assertThat(captor.getValue().isAsistenciaAtendida()).isTrue();
        }

        @Test
        @DisplayName("lanza BusinessException si la notificación ya fue atendida")
        void lanzaExcepcionSiYaAtendida() {
            Mesa mesa = mesaConMesero();
            Notificacion notif = Notificacion.builder()
                    .notificacionId(50L)
                    .notificacionEstado(EstadoNotificacion.ATENDIDA)
                    .mesa(mesa).empleado(mesa.getMesero()).build();

            when(notificacionRepository.findById(50L)).thenReturn(Optional.of(notif));

            assertThatThrownBy(() -> notificacionService.atenderAsistencia(50L, "mesero@test.com"))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("lanza ResourceNotFoundException si la notificación no existe")
        void lanzaNotFoundSiNotificacionNoExiste() {
            when(notificacionRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> notificacionService.atenderAsistencia(99L, "mesero@test.com"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
