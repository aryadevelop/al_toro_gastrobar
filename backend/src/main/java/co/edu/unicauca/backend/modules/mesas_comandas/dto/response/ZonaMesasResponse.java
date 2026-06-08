package co.edu.unicauca.backend.modules.mesas_comandas.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * DTO para representar una zona con sus mesas activas.
 * Usado en el mapa de mesas para agrupar por zona.
 */
@Getter
@Builder
public class ZonaMesasResponse {

    /** ID de la zona */
    private final Long zonaId;

    /** Nombre de la zona */
    private final String zonaNombre;

    /** Cantidad de mesas activas en esta zona */
    private final Integer cantidadMesasActivas;

    /** Lista de mesas en la zona */
    private final List<MesaMapaResponse> mesas;
}
