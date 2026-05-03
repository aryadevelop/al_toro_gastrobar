package co.edu.unicauca.backend.modules.reservas.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

/**
 * DTO de respuesta al marcar una reserva como inasistencia.
 *
 * <p>Retornado tras ejecutar {@code PATCH /api/reservas/{reservaId}/marcar-inasistencia}.
 * Confirma el cambio de estado y detalla los recursos liberados (zona y decoración).
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MarcarInasistenciaResponse {

    /** Identificador de la reserva marcada como inasistencia. */
    private final Long reservaId;

    /** Estado final de la reserva (siempre {@code "INASISTENCIA"}). */
    private final String estado;

    /** Nombre de la zona liberada; {@code null} si la reserva no tenía zona asignada. */
    private final String zonaLiberada;

    /** Nombre de la decoración liberada; {@code null} si la reserva no tenía decoración. */
    private final String decoracionLiberada;
}
