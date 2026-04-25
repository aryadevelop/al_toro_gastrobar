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

    /**
     * Saldo de puntos del cliente después del cierre (incluye el +1 de esta visita).
     * Permite al frontend actualizar el indicador de puntos en tiempo real.
     */
    private final Integer puntosActuales;
}
