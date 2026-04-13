package co.edu.unicauca.backend.modules.reservas.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Respuesta al consultar disponibilidad para una fecha y hora.
 * Incluye el flag de disponibilidad y las listas de decoraciones/zonas libres.
 */
@Getter
@Builder
public class DisponibilidadResponse {
    private Boolean disponible;
    private List<DecoracionDisponibleResponse> decoraciones;
    private List<ZonaDisponibleResponse> zonas;
}
