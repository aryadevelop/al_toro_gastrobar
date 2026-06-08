package co.edu.unicauca.backend.modules.notificaciones.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * Respuesta al atender una notificación de tipo {@code CAMBIO}.
 *
 * <p>Devuelve el identificador de la comanda que el mesero debe cargar
 * en modo edición tras aceptar la solicitud de cambio del cliente.
 */
@Getter
@Builder
public class AtenderCambioResponse {

    /** Identificador de la comanda lista para ser modificada por el mesero. */
    private final Long comandaId;
}
