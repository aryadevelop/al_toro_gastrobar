package co.edu.unicauca.backend.modules.reservas.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

/**
 * Respuesta de la operación de cancelación de una reserva.
 *
 * <p>Cuando {@code requiereWhatsApp} es {@code true}, el campo {@code mensajeWhatsApp}
 * contiene el texto precompuesto que el frontend debe enviar al chat de la empresa
 * para gestionar el reembolso del abono.
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CancelarReservaResponse {

    /** Identificador de la reserva cancelada. */
    private Long reservaId;

    /** Estado resultante; siempre {@code "CANCELADA"}. */
    private String estado;

    /** Tipo de la reserva cancelada: {@code "BASICA"} o {@code "ESPECIAL"}. */
    private String tipo;

    /** Fecha y hora de llegada formateada como {@code yyyy-MM-dd'T'HH:mm:ss}. */
    private String fechaHoraLlegada;

    /** Número de personas de la reserva cancelada. */
    private Integer numeroPersonas;

    /**
     * {@code true} si el cliente debe ser redirigido al chat de WhatsApp
     * para gestionar el reembolso del abono (CA-02 y CA-03).
     */
    private boolean requiereWhatsApp;

    /**
     * Mensaje precompuesto para WhatsApp; {@code null} cuando {@code requiereWhatsApp}
     * es {@code false}.
     */
    private String mensajeWhatsApp;
}
