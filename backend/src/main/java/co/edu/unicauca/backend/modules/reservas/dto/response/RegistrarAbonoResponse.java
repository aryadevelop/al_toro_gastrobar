package co.edu.unicauca.backend.modules.reservas.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

/**
 * DTO de respuesta devuelto al registrar un anticipo o devolución sobre una reserva.
 *
 * <p>Combina el identificador y datos básicos del movimiento registrado con el
 * resumen financiero actualizado de la reserva afectada.
 *
 * <p>Campos nulos se omiten de la serialización JSON mediante
 * {@link JsonInclude.Include#NON_NULL}.
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RegistrarAbonoResponse {

    /** Identificador único del abono o devolución recién registrado. */
    private final Long abonoId;

    /** Tipo de movimiento: {@code ANTICIPO} o {@code DEVOLUCION}. */
    private final String tipo;

    /** Estado resultante de la reserva tras registrar el movimiento (p. ej. {@code CONFIRMADA}, {@code DEVUELTA}). */
    private final String estado;

    /**
     * Resumen financiero actualizado de la reserva tras aplicar el movimiento;
     * {@code null} si no se calculó resumen en esta operación.
     */
    private final ResumenPagoResponse resumen;
}
