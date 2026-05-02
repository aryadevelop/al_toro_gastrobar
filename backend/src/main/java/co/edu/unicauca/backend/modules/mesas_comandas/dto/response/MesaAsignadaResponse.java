package co.edu.unicauca.backend.modules.mesas_comandas.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * DTO de respuesta tras asignar identificador a una mesa.
 */
@Getter
@Builder
public class MesaAsignadaResponse {

    /** ID de la visita creada */
    private final Long visitaId;

    /** Identificador de la mesa asignado */
    private final String mesaIdentificador;

    /** ID de la zona donde se ubicó la mesa */
    private final Long zonaId;

    /** Nombre de la zona */
    private final String zonaNombre;

    /** Número de personas */
    private final Integer numeroPersonas;

    /** Estado inicial de la mesa: "ESPERA" */
    private final String estadoMesa;

    /** Email del mesero asignado */
    private final String emailMesero;

    /** ID de la reserva (null si es walk-in) */
    private final Long reservaId;
}
