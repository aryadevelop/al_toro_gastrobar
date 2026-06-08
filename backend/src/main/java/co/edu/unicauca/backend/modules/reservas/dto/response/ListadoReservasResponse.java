package co.edu.unicauca.backend.modules.reservas.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * DTO wrapper que agrupa el listado de reservas y los resúmenes por zona.
 */
@Getter
@Builder
public class ListadoReservasResponse {

    /** Lista de reservas activas ordenadas por hora de llegada ascendente. */
    private final List<ReservaConsultaResponse> reservas;

    /** Resumen de cantidad de reservas por zona. */
    private final List<ResumenZonaResponse> resumenZonas;
}
