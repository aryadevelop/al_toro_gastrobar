package co.edu.unicauca.backend.modules.notificaciones.dto.ws;

import lombok.Builder;
import lombok.Getter;

/**
 * Mensaje WebSocket publicado en {@code /topic/visita/{visitaId}/cuenta}
 * cuando el cajero cierra la cuenta.
 *
 * <p>El frontend usa {@code puntosActuales} para actualizar en tiempo real el
 * saldo de puntos del cliente sin necesidad de un request adicional.
 * El campo {@code mensaje} se muestra como popup de confirmación.
 */
@Getter
@Builder
public class CuentaCerradaWsMessage {

    private final Long visitaId;
    private final String mensaje;
    private final Integer puntosActuales;
}
