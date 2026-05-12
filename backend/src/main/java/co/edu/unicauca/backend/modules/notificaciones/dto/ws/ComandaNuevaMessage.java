package co.edu.unicauca.backend.modules.notificaciones.dto.ws;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload publicado en RabbitMQ (routing key {@code RK_COMANDA_NUEVA}) cuando
 * una comanda transiciona de BORRADOR a PENDIENTE. 
 * 
 * Lo consume el bridge de impresión de tickets desde la cola {@code q.comanda.produccion}. 
 * 
 * Actualizar atributos en la HU correspondiente
 */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ComandaNuevaMessage {
    /** Identificador de la comanda enviada a producción. */
    private Long comandaId;

    /** Identificador de la visita dueña de la comanda. */
    private Long visitaId;

    /** Estación destino: {@code "COCINA"} o {@code "BARRA"}. */
    private String estacion;

    /** Marca temporal asignada al transicionar a PENDIENTE. */
    private LocalDateTime fechaHoraInicio;
}
