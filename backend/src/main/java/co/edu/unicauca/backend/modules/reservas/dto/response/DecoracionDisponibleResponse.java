package co.edu.unicauca.backend.modules.reservas.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Decoración disponible que se retorna en la consulta de disponibilidad.
 */
@Getter
@Builder
public class DecoracionDisponibleResponse {
    private Long decoracionId;
    private String nombre;
    private String imagenUrl;
    /**
     * {@code true} si el cliente puede elegir la zona libremente;
     * {@code false} si la decoración tiene zona fija.
     */
    private Boolean puedeSeleccionarZona;
    /**
     * IDs de zonas compatibles y libres para esta decoración.
     * Lista vacía significa que es compatible con todas las zonas disponibles.
     */
    private List<Long> zonaIdsCompatibles;
}
