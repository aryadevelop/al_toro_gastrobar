package co.edu.unicauca.backend.modules.reservas.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * Zona del restaurante disponible devuelta en la consulta de disponibilidad para una fecha y hora.
 *
 * @see DisponibilidadResponse
 */
@Getter
@Builder
public class ZonaDisponibleResponse {

    /** Identificador único de la zona. */
    private Long zonaId;

    /** Nombre visible de la zona. */
    private String nombre;

    /** URL de la imagen representativa de la zona; puede ser {@code null} si no tiene imagen. */
    private String imagenUrl;

    /** Número máximo de personas que admite la zona. */
    private Integer capacidad;
}
