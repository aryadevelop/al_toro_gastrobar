package co.edu.unicauca.backend.modules.reservas.dto.response;

import lombok.Builder;
import lombok.Getter;
/**
 * Representación completa de una reserva devuelta al cliente tras crearla o consultarla.
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

    /** Estado actual de la reserva (ver {@code EstadoReserva}). */
    private String estado;

    /** Tipo de reserva, {@code "ESPECIAL"} o {@code "BÁSICA"}. */
    private String tipo;

    /** Nombre de la decoración asignada; {@code null} si la reserva no tiene decoración. */
    private String decoracionNombre;

    /** Nombre de la zona asignada; {@code null} si la reserva no tiene zona. */
    private String zonaNombre;

    /** Observaciones o peticiones especiales del cliente; {@code null} si no hay notas. */
    private String notas;

    /** Identificador del cliente que realizó la reserva. */
    private Long clienteId;

    /** Nombre completo del cliente que realizó la reserva. */
    private String clienteNombre;

    /** {@code true} cuando la reserva es ESPECIAL y requiere confirmar anticipo vía WhatsApp. */
    private Boolean requiereWhatsApp;

    /** Mensaje precompuesto para WhatsApp; {@code null} cuando no aplica. */
    private String mensajeWhatsApp;
}
