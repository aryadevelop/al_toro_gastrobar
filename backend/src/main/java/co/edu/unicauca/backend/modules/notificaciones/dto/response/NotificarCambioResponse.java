package co.edu.unicauca.backend.modules.notificaciones.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * Respuesta devuelta tras crear una notificación de cambio. Expone el
 * identificador para que el frontend pueda referenciar la notificación en
 * acciones posteriores y su estado inicial.
 */
@Getter
@Builder
public class NotificarCambioResponse {

    /** Identificador de la notificación recién creada. */
    private final Long notificacionId;

    /** Estado de la notificación: {@code "ACTIVA"} al momento de la creación. */
    private final String estado;
}
