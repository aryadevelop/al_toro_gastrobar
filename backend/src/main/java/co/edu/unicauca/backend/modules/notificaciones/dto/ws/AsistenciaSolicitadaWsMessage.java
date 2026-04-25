package co.edu.unicauca.backend.modules.notificaciones.dto.ws;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Mensaje WebSocket publicado en {@code /topic/mesas/asistencia} para
 * todos los empleados conectados cuando el cliente solicita asistencia.
 */
@Getter
@Builder
public class AsistenciaSolicitadaWsMessage {

    private final Long visitaId;
    private final Long notificacionId;
    private final String mesaIdentificador;
    private final String clienteNombre;
    private final LocalDateTime fechaHora;
}
