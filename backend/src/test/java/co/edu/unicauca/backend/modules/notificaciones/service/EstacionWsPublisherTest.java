package co.edu.unicauca.backend.modules.notificaciones.service;

import co.edu.unicauca.backend.modules.notificaciones.dto.ws.ComandaEstacionWsMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("EstacionWsPublisher — tests unitarios")
class EstacionWsPublisherTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private EstacionWsPublisher publisher;

    @ParameterizedTest(name = "publicarComandaEnviada envía a /topic/estacion/{0} con el payload")
    @ValueSource(strings = { "COCINA", "BARRA" })
    void publicarComandaEnviada_enviaTopicSegunEstacion(String estacion) {
        ComandaEstacionWsMessage mensaje = ComandaEstacionWsMessage.builder()
                .comandaId(1L)
                .visitaId(2L)
                .estacion(estacion)
                .items(List.of())
                .build();

        publisher.publicarComandaEnviada(mensaje);

        verify(messagingTemplate).convertAndSend("/topic/estacion/" + estacion, (Object) mensaje);
        verifyNoMoreInteractions(messagingTemplate);
    }
}
