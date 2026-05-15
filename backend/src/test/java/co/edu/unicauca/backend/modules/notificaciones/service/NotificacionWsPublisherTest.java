package co.edu.unicauca.backend.modules.notificaciones.service;

import co.edu.unicauca.backend.modules.notificaciones.dto.ws.AsistenciaAtendidaWsMessage;
import co.edu.unicauca.backend.modules.notificaciones.dto.ws.AsistenciaSolicitadaWsMessage;
import co.edu.unicauca.backend.modules.notificaciones.dto.ws.ComandaProduccionEventoWsMessage;
import co.edu.unicauca.backend.modules.notificaciones.dto.ws.CuentaCerradaWsMessage;
import co.edu.unicauca.backend.modules.notificaciones.dto.ws.ReservaActualizadaWsMessage;
import co.edu.unicauca.backend.modules.notificaciones.dto.ws.TipoEventoProduccion;
import co.edu.unicauca.backend.modules.notificaciones.dto.ws.VisitaActualizadaWsMessage;
import co.edu.unicauca.backend.shared.enums.EstacionComanda;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificacionWsPublisher — tests unitarios")
class NotificacionWsPublisherTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private NotificacionWsPublisher publisher;

    @Test
    @DisplayName("publicarVisitaActualizada envía al tópico /topic/visita/{id}/orden con el payload")
    void publicarVisitaActualizada_enviaTopicYPayload() {
        VisitaActualizadaWsMessage mensaje = VisitaActualizadaWsMessage.builder()
                .visitaId(42L)
                .items(List.of())
                .total(BigDecimal.ZERO)
                .build();

        publisher.publicarVisitaActualizada(42L, mensaje);

        verify(messagingTemplate).convertAndSend("/topic/visita/42/orden", (Object) mensaje);
        verifyNoMoreInteractions(messagingTemplate);
    }

    @Test
    @DisplayName("publicarCuentaCerrada envía al tópico /topic/visita/{id}/cuenta con el payload")
    void publicarCuentaCerrada_enviaTopicYPayload() {
        CuentaCerradaWsMessage mensaje = CuentaCerradaWsMessage.builder()
                .visitaId(7L)
                .mensaje("Cuenta cerrada")
                .puntosActuales(100)
                .build();

        publisher.publicarCuentaCerrada(7L, mensaje);

        verify(messagingTemplate).convertAndSend("/topic/visita/7/cuenta", (Object) mensaje);
        verifyNoMoreInteractions(messagingTemplate);
    }

    @Test
    @DisplayName("publicarAsistenciaAtendida envía al tópico /topic/visita/{id}/asistencia")
    void publicarAsistenciaAtendida_enviaTopicYPayload() {
        AsistenciaAtendidaWsMessage mensaje = AsistenciaAtendidaWsMessage.builder()
                .visitaId(99L)
                .asistenciaAtendida(true)
                .build();

        publisher.publicarAsistenciaAtendida(99L, mensaje);

        verify(messagingTemplate).convertAndSend("/topic/visita/99/asistencia", (Object) mensaje);
        verifyNoMoreInteractions(messagingTemplate);
    }

    @Test
    @DisplayName("publicarAsistenciaSolicitada hace broadcast a /topic/mesas/asistencia")
    void publicarAsistenciaSolicitada_broadcast() {
        AsistenciaSolicitadaWsMessage mensaje = AsistenciaSolicitadaWsMessage.builder()
                .visitaId(1L)
                .notificacionId(2L)
                .mesaIdentificador("Mesa 5")
                .clienteNombre("Cliente")
                .fechaHora(LocalDateTime.now())
                .build();

        publisher.publicarAsistenciaSolicitada(mensaje);

        verify(messagingTemplate).convertAndSend("/topic/mesas/asistencia", (Object) mensaje);
        verifyNoMoreInteractions(messagingTemplate);
    }

    @Test
    @DisplayName("publicarReservaActualizada hace broadcast a /topic/reservas/cambios")
    void publicarReservaActualizada_broadcast() {
        ReservaActualizadaWsMessage mensaje = ReservaActualizadaWsMessage.builder()
                .reservaId(10L)
                .tipoEvento("CREADA")
                .clienteNombre("Cliente")
                .horaLlegada("19:00")
                .zonaNombre("Zona A")
                .build();

        publisher.publicarReservaActualizada(mensaje);

        verify(messagingTemplate).convertAndSend("/topic/reservas/cambios", (Object) mensaje);
        verifyNoMoreInteractions(messagingTemplate);
    }

    @Test
    @DisplayName("publicarEventoProduccion COCINA: envía a /topic/produccion/cocina con payload")
    void publicarEventoProduccion_cocina() {
        ArgumentCaptor<ComandaProduccionEventoWsMessage> captor =
                ArgumentCaptor.forClass(ComandaProduccionEventoWsMessage.class);
        ComandaProduccionEventoWsMessage mensaje = new ComandaProduccionEventoWsMessage(
                TipoEventoProduccion.COMPLETADA, "COCINA", 15L, null, null);

        publisher.publicarEventoProduccion(EstacionComanda.COCINA, mensaje);

        verify(messagingTemplate).convertAndSend(eq("/topic/produccion/cocina"), captor.capture());
        assertThat(captor.getValue().tipo()).isEqualTo(TipoEventoProduccion.COMPLETADA);
        assertThat(captor.getValue().comandaId()).isEqualTo(15L);
        verifyNoMoreInteractions(messagingTemplate);
    }

    @Test
    @DisplayName("publicarEventoProduccion BARRA: envía a /topic/produccion/barra")
    void publicarEventoProduccion_barra() {
        ComandaProduccionEventoWsMessage mensaje = new ComandaProduccionEventoWsMessage(
                TipoEventoProduccion.CREADA, "BARRA", 20L, null, null);

        publisher.publicarEventoProduccion(EstacionComanda.BARRA, mensaje);

        verify(messagingTemplate).convertAndSend("/topic/produccion/barra", (Object) mensaje);
        verifyNoMoreInteractions(messagingTemplate);
    }
}
