package co.edu.unicauca.backend.modules.usuarios.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * DTO de respuesta con los puntos de fidelización del cliente.
 */
@Getter
@Builder
public class ClientePuntosResponse {

    /** Puntos canjeables actuales del cliente. Se resetean a 0 en cada canje. */
    private final Integer puntosActuales;

    /** Total de puntos ganados históricamente. Nunca disminuye. */
    private final Integer puntosAcumulados;
}
