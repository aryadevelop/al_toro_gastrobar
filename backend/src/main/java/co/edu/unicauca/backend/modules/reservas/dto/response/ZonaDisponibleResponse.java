package co.edu.unicauca.backend.modules.reservas.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * Zona disponible que se retorna en la consulta de disponibilidad.
 */
@Getter
@Builder
public class ZonaDisponibleResponse {
    private Long zonaId;
    private String nombre;
    private String imagenUrl;
    private Integer capacidad;
}
