package co.edu.unicauca.backend.modules.mesas_comandas.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * DTO para listar zonas disponibles al crear mesa.
 *
 * <p>Una zona es disponible si la cantidad de personas en visitas activas
 * no supera su capacidad máxima.
 */
@Getter
@Builder
public class ZonaDisponibleMesaResponse {

    /** ID de la zona */
    private final Long zonaId;

    /** Nombre descriptivo de la zona */
    private final String zonaNombre;

    /** Capacidad máxima de personas */
    private final Integer capacidadTotal;

    /** Personas actualmente ocupando mesas en esta zona */
    private final Integer personasOcupadas;

    /** Disponibilidad restante (capacidadTotal - personasOcupadas) */
    private final Integer disponibilidad;
}
