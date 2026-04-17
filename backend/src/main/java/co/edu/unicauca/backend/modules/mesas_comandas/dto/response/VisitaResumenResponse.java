package co.edu.unicauca.backend.modules.mesas_comandas.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * DTO de respuesta para cada elemento del historial de visitas del cliente.
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VisitaResumenResponse {

    /** Identificador de la visita registrada. */
    private final Long visitaId;

    /** Identificador de la reserva vinculada; {@code null} para walk-ins. */
    private final Long reservaId;

    /** Fecha y hora de llegada en formato {@code yyyy-MM-dd'T'HH:mm:ss}. */
    private final String fechaHoraLlegada;

    /** Número de comensales. */
    private final Integer numeroPersonas;

    /** Identificador visual de la mesa asignada; {@code null} si la visita no tuvo mesa registrada. */
    private final String mesaIdentificador;

    /** Nombre de la zona seleccionada; {@code null} si el cliente no eligió zona. */
    private final String zonaNombre;

    /** Estado de la visita. */
    private final String estadoVisita;

    /** Monto total cobrado al cliente; {@code null} si la cuenta no fue cerrada */
    private final BigDecimal montoTotal;
}
