package co.edu.unicauca.backend.modules.reservas.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Respuesta al consultar disponibilidad para una fecha y hora determinadas.
 *
 * <p>Reglas de uso:
 * <ul>
 *   <li>Si {@code disponible} es {@code false}, las listas {@code decoraciones} y {@code zonas}
 *       estarán vacías y el frontend debe informar al cliente.</li>
 *   <li>Las zonas devueltas en este objeto son las libres sin decoración asignada.</li>
 * </ul>
 *
 * @see DecoracionDisponibleResponse
 * @see ZonaDisponibleResponse
 */
@Getter
@Builder
public class DisponibilidadResponse {

    /**
     * {@code true} si existe al menos un espacio disponible para la fecha y hora consultadas;
     * {@code false} si el restaurante está completo.
     */
    private Boolean disponible;

    /**
     * Lista de decoraciones disponibles para la fecha y hora consultadas.
     * Vacía cuando {@code disponible = false}.
     */
    private List<DecoracionDisponibleResponse> decoraciones;

    /**
     * Lista de zonas disponibles sin decoración asignada para la fecha y hora consultadas.
     * Vacía cuando {@code disponible = false}.
     */
    private List<ZonaDisponibleResponse> zonas;
}
