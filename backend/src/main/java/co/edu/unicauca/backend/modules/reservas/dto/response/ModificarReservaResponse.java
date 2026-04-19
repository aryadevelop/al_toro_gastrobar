package co.edu.unicauca.backend.modules.reservas.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

/**
 * DTO de respuesta para la modificación de una reserva.
 *
 * <p>Cuando {@code requiereWhatsApp} es {@code true}, el frontend debe redirigir
 * al cliente al chat de WhatsApp de la empresa con el mensaje {@code mensajeWhatsApp}
 * precompuesto. El {@code reservaId} siempre corresponde a la misma reserva modificada.
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ModificarReservaResponse {

    /** Identificador de la reserva modificada. */
    private final Long reservaId;

    /** Estado de la reserva resultante: {@code CONFIRMADA} o {@code PENDIENTE}. */
    private final String estado;

    /** Tipo de la reserva resultante: {@code BASICA} o {@code ESPECIAL}. */
    private final String tipo;

    /** Fecha y hora de llegada en formato {@code yyyy-MM-dd'T'HH:mm:ss}. */
    private final String fechaHoraLlegada;

    /** Número de comensales. */
    private final Integer numeroPersonas;

    /** Nombre de la zona; {@code null} si no se seleccionó zona. */
    private final String zonaNombre;

    /** Nombre de la decoración; {@code null} si no aplica. */
    private final String decoracionNombre;

    /** Observaciones del cliente; {@code null} si no hay notas. */
    private final String notas;

    /**
     * {@code true} cuando la modificación requiere confirmar vía WhatsApp
     * (transiciones BASICA→ESPECIAL o ESPECIAL→BASICA).
     */
    private final boolean requiereWhatsApp;

    /**
     * Mensaje precompuesto para enviar al chat de WhatsApp; {@code null} cuando
     * {@code requiereWhatsApp} es {@code false}.
     */
    private final String mensajeWhatsApp;
}
