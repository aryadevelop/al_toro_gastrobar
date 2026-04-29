package co.edu.unicauca.backend.modules.mesas_comandas.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * DTO para notificación activa de una mesa.
 * Usado en el mapa de mesas para mostrar alertas pendientes.
 */
@Getter
@Builder
public class NotificacionActivaResponse {

    /** ID de la notificación */
    private final Long notificacionId;

    /** Tipo de notificación: "ATENCION", "PLATOS_LISTOS", "BEBIDAS_LISTAS", "CAMBIO" */
    private final String tipo;

    /** Fecha y hora de emisión (para ordenamiento y tiempo transcurrido en UI) */
    private final LocalDateTime fechaHora;
}
