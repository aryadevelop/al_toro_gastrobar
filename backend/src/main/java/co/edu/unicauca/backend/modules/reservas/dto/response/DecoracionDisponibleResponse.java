package co.edu.unicauca.backend.modules.reservas.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Decoración disponible devuelta en la consulta de disponibilidad para una fecha y hora.
 *
 * <p>Reglas de uso:
 * <ul>
 *   <li>Si {@code puedeSeleccionarZona} es {@code true}, el frontend debe mostrar
 *       al cliente las zonas de {@code zonaIdsCompatibles} para que elija una.</li>
 *   <li>Si {@code puedeSeleccionarZona} es {@code false}, la decoración no permite
 *       elección de zona y {@code zonaIdsCompatibles} puede estar vacío.</li>
 * </ul>
 *
 * @see DisponibilidadResponse
 */
@Getter
@Builder
public class DecoracionDisponibleResponse {

    /** Identificador único de la decoración. */
    private Long decoracionId;

    /** Nombre visible de la decoración*/
    private String nombre;

    /** URL de la imagen representativa de la decoración; puede ser {@code null} si no tiene imagen. */
    private String imagenUrl;

    /**
     * Indica si el cliente puede elegir una zona específica al seleccionar esta decoración.
     * {@code true} habilita la selección de zona.
     */
    private Boolean puedeSeleccionarZona;

    /**
     * IDs de las zonas compatibles con esta decoración y disponibles para la fecha consultada.
     * Solo relevante cuando {@code puedeSeleccionarZona = true}.
     */
    private List<Long> zonaIdsCompatibles;
}
