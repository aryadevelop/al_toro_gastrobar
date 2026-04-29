package co.edu.unicauca.backend.modules.mesas_comandas.service;

import co.edu.unicauca.backend.shared.enums.EstadoMesa;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Publicador de mensajes WebSocket para actualizaciones del mapa de mesas.
 *
 * <p>Publica eventos cuando:
 * - Se crea una mesa (nueva visita)
 * - Cambia el estado de una mesa
 * - Se cierra una mesa
 * - Se crea/atiende una notificación en una mesa
 *
 * <p>Destino: /topic/mesas
 */
@Service
@RequiredArgsConstructor
public class MesaWsPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    private static final String TOPIC_MESAS = "/topic/mesas";

    /**
     * Publica evento de actualización de mesa.
     *
     * @param visitaId ID de la visita/mesa
     * @param tipoEvento tipo de evento (CREAR, ACTUALIZAR, CERRAR, NOTIFICACION)
     */
    public void publicarActualizacionMesa(Long visitaId, TipoEventoMesa tipoEvento) {
        MesaWsMessage mensaje = MesaWsMessage.builder()
                .visitaId(visitaId)
                .tipoEvento(tipoEvento)
                .timestamp(System.currentTimeMillis())
                .build();

        messagingTemplate.convertAndSend(TOPIC_MESAS, mensaje);
    }

    /**
     * Publica evento de cambio de estado de mesa.
     *
     * @param visitaId ID de la visita/mesa
     * @param nuevoEstado nuevo estado de la mesa
     */
    public void publicarCambioEstadoMesa(Long visitaId, EstadoMesa nuevoEstado) {
        MesaWsMessage mensaje = MesaWsMessage.builder()
                .visitaId(visitaId)
                .tipoEvento(TipoEventoMesa.ACTUALIZAR)
                .nuevoEstado(nuevoEstado.name())  // Enum → String
                .timestamp(System.currentTimeMillis())
                .build();

        messagingTemplate.convertAndSend(TOPIC_MESAS, mensaje);
    }

    /**
     * Mensaje WebSocket para eventos de mesa.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MesaWsMessage {
        private Long visitaId;
        private TipoEventoMesa tipoEvento;
        private String nuevoEstado;  // String, no enum
        private Long timestamp;
    }

    /**
     * Tipos de eventos de mesa.
     */
    public enum TipoEventoMesa {
        /** Nueva mesa creada */
        CREAR,
        /** Mesa actualizada (cambio de estado, notificación, etc.) */
        ACTUALIZAR,
        /** Mesa cerrada (visita finalizada) */
        CERRAR,
        /** Nueva notificación en la mesa */
        NOTIFICACION
    }
}
