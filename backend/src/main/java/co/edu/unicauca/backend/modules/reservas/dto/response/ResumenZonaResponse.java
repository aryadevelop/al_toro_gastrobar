package co.edu.unicauca.backend.modules.reservas.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * DTO de resumen por zona en el módulo de consulta de reservas.
 *
 * <p>Muestra el identificador de la zona, su nombre y la cantidad total de reservas
 * activas (CONFIRMADA/PENDIENTE) en esa zona para la fecha consultada.
 */
@Getter
@Builder
public class ResumenZonaResponse {

    /** Identificador de la zona. */
    private final Long zonaId;

    /** Nombre de la zona. */
    private final String zonaNombre;

    /** Cantidad de reservas activas en la zona para la fecha consultada. */
    private final Integer cantidadReservas;
}
