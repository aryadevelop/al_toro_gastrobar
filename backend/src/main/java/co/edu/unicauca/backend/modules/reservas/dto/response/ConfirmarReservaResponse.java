package co.edu.unicauca.backend.modules.reservas.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

/**
 * DTO de respuesta para la confirmación de una reserva especial por parte del cajero.
 *
 * <p>Refleja el estado resultante de la reserva tras la transición de {@code PENDIENTE}
 * a {@code CONFIRMADA}.
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConfirmarReservaResponse {

    /** Identificador único de la reserva confirmada. */
    private final Long reservaId;

    /** Estado actual de la reserva; {@code CONFIRMADA} tras la operación. */
    private final String estado;

    /** Tipo de reserva: {@code ESPECIAL} (único elegible para confirmación). */
    private final String tipo;

    /** Fecha y hora de llegada programada en formato {@code yyyy-MM-dd'T'HH:mm:ss}. */
    private final String fechaHoraLlegada;

    /** Número de comensales de la reserva. */
    private final Integer numeroPersonas;

    /** Nombre del cliente que realizó la reserva. */
    private final String clienteNombre;

    /** Nombre de la zona seleccionada; {@code null} si la reserva no tiene zona. */
    private final String zonaNombre;
}
