package co.edu.unicauca.backend.modules.notificaciones.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * Respuesta al cliente cuando solicita asistencia de un mesero.
 */
@Getter
@Builder
public class NotificacionAsistenciaResponse {

    /** ID de la notificación creada. */
    private final Long notificacionId;

    /** Estado de la notificación: siempre "ACTIVA" al crearse. */
    private final String estado;
}
