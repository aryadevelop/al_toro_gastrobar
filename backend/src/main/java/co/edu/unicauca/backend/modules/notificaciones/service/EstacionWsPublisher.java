package co.edu.unicauca.backend.modules.notificaciones.service;

import co.edu.unicauca.backend.modules.notificaciones.dto.ws.ComandaEstacionWsMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Publicador WebSocket para los dashboards de producción por estación.
 *
 * <p>Destino: {@code /topic/estacion/COCINA} o {@code /topic/estacion/BARRA}
 */
@Service
@RequiredArgsConstructor
public class EstacionWsPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    /** Prefijo común de los tópicos por estación; concatena con {@code COCINA} o {@code BARRA}. */
    private static final String TOPIC_ESTACION = "/topic/estacion/";

    /**
     * Publica la comanda enviada a producción en el tópico de su estación.
     *
     * @param mensaje payload con items y metadata de la comanda enviada
     */
    public void publicarComandaEnviada(ComandaEstacionWsMessage mensaje) {
        messagingTemplate.convertAndSend(TOPIC_ESTACION + mensaje.getEstacion(), mensaje);
    }
}
