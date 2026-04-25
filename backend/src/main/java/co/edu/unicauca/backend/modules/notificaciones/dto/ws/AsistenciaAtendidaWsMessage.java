package co.edu.unicauca.backend.modules.notificaciones.dto.ws;

import lombok.Builder;
import lombok.Getter;

/**
 * Mensaje WebSocket publicado en {@code /topic/visita/{visitaId}/asistencia}
 * cuando el mesero marca la solicitud como atendida.
 * El frontend re-habilita el botón "Solicitar asistencia".
 */
@Getter
@Builder
public class AsistenciaAtendidaWsMessage {

    private final Long visitaId;
    private final boolean asistenciaAtendida;
}
