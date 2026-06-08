package co.edu.unicauca.backend.modules.notificaciones.service;

import co.edu.unicauca.backend.shared.enums.EstadoMesa;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("MesaWsPublisher — tests unitarios")
class MesaWsPublisherTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private MesaWsPublisher publisher;

    @Test
    @DisplayName("publicarActualizacionMesa envía a /topic/mesas con visitaId, tipoEvento y timestamp")
    void publicarActualizacionMesa_enviaPayloadCompleto() {
        long antes = System.currentTimeMillis();
        ArgumentCaptor<MesaWsPublisher.MesaWsMessage> captor =
                ArgumentCaptor.forClass(MesaWsPublisher.MesaWsMessage.class);

        publisher.publicarActualizacionMesa(11L, MesaWsPublisher.TipoEventoMesa.CREAR);

        verify(messagingTemplate).convertAndSend(eq("/topic/mesas"), captor.capture());
        MesaWsPublisher.MesaWsMessage payload = captor.getValue();
        assertThat(payload.getVisitaId()).isEqualTo(11L);
        assertThat(payload.getTipoEvento()).isEqualTo(MesaWsPublisher.TipoEventoMesa.CREAR);
        assertThat(payload.getNuevoEstado()).isNull();
        assertThat(payload.getTimestamp()).isGreaterThanOrEqualTo(antes);
        verifyNoMoreInteractions(messagingTemplate);
    }

    @Test
    @DisplayName("publicarCambioEstadoMesa envía a /topic/mesas con tipoEvento ACTUALIZAR y nombre del nuevo estado")
    void publicarCambioEstadoMesa_enviaPayloadConEstado() {
        long antes = System.currentTimeMillis();
        ArgumentCaptor<MesaWsPublisher.MesaWsMessage> captor =
                ArgumentCaptor.forClass(MesaWsPublisher.MesaWsMessage.class);

        publisher.publicarCambioEstadoMesa(33L, EstadoMesa.EN_PREPARACION);

        verify(messagingTemplate).convertAndSend(eq("/topic/mesas"), captor.capture());
        MesaWsPublisher.MesaWsMessage payload = captor.getValue();
        assertThat(payload.getVisitaId()).isEqualTo(33L);
        assertThat(payload.getTipoEvento()).isEqualTo(MesaWsPublisher.TipoEventoMesa.ACTUALIZAR);
        assertThat(payload.getNuevoEstado()).isEqualTo(EstadoMesa.EN_PREPARACION.name());
        assertThat(payload.getTimestamp()).isGreaterThanOrEqualTo(antes);
        verifyNoMoreInteractions(messagingTemplate);
    }
}
