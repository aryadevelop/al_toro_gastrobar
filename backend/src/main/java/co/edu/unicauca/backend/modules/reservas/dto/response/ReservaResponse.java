package co.edu.unicauca.backend.modules.reservas.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

/**
 * Representación completa de una reserva devuelta al cliente tras crearla o consultarla.
 *
 * <p>Ejemplo de estructura JSON:
 * <pre>
 * {
 *   "reservaId": 101,
 *   "fechaHoraLlegada": "2026-05-10T19:30:00",
 *   "numeroPersonas": 4,
 *   "estado": "PENDIENTE",
 *   "tipo": "CON_PREORDEN",
 *   "decoracionId": 3,
 *   "decoracionNombre": "Romántica",
 *   "zonaId": 1,
 *   "zonaNombre": "Terraza",
 *   "notas": "Alergia al mariscos",
 *   "fechaCreacion": "2026-04-15T10:00:00",
 *   "clienteId": 55,
 *   "clienteNombre": "Ana Gómez",
 *   "preOrdenItems": [ ... ],
 *   "preOrdenTotal": 45000.00
 * }
 * </pre>
 *
 * <p>Reglas de uso:
 * <ul>
 *   <li>{@code decoracionId}, {@code decoracionNombre}, {@code zonaId} y {@code zonaNombre}
 *       son {@code null} cuando la reserva no tiene decoración o zona asignada.</li>
 *   <li>{@code preOrdenItems} es {@code null} o lista vacía cuando la reserva no incluye pre-orden.</li>
 *   <li>{@code preOrdenTotal} es {@code null} cuando no hay pre-orden.</li>
 * </ul>
 *
 * @see PreOrdenItemResumen
 */
@Getter
@Builder
public class ReservaResponse {

    /** Identificador único de la reserva. */
    private Long reservaId;

    /** Fecha y hora de llegada del cliente, formateada como {@code ISO-8601}. */
    private String fechaHoraLlegada;

    /** Número de comensales confirmados para la reserva. */
    private Integer numeroPersonas;

    /** Estado actual de la reserva (ver {@code EstadoReserva}), por ejemplo {@code "PENDIENTE"}. */
    private String estado;

    /** Tipo de reserva, {@code "ESPECIAL"} o {@code "BÁSICA"}. */
    private String tipo;

    /** Identificador de la decoración asignada; {@code null} si la reserva no tiene decoración. */
    private Long decoracionId;

    /** Nombre de la decoración asignada; {@code null} si la reserva no tiene decoración. */
    private String decoracionNombre;

    /** Identificador de la zona asignada; {@code null} si la reserva no tiene zona. */
    private Long zonaId;

    /** Nombre de la zona asignada; {@code null} si la reserva no tiene zona. */
    private String zonaNombre;

    /** Observaciones o peticiones especiales del cliente; {@code null} si no hay notas. */
    private String notas;

    /** Fecha y hora en que se registró la reserva, formateada como {@code ISO-8601}. */
    private String fechaCreacion;

    /** Identificador del cliente que realizó la reserva. */
    private Long clienteId;

    /** Nombre completo del cliente que realizó la reserva. */
    private String clienteNombre;

    /**
     * Resumen de los ítems de la pre-orden asociada a la reserva.
     * {@code null} o lista vacía cuando la reserva no incluye pre-orden.
     */
    private List<PreOrdenItemResumen> preOrdenItems;

    /**
     * Suma total del valor de la pre-orden.
     * {@code null} cuando la reserva no incluye pre-orden.
     */
    private BigDecimal preOrdenTotal;
}
