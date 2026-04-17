package co.edu.unicauca.backend.modules.reservas.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * DTO de respuesta para un abono o devolución dentro del detalle de una reserva.
 */
@Getter
@Builder
public class AbonoItemResponse {

    /** Identificador único del abono. */
    private final Long abonoId;

    /** Monto del abono o devolución. */
    private final BigDecimal monto;

    /** Fecha y hora del registro en formato {@code yyyy-MM-dd'T'HH:mm:ss}. */
    private final String fechaHora;

    /** Método de pago utilizado (EFECTIVO, TARJETA, etc.). */
    private final String metodo;

    /** Tipo de movimiento: ANTICIPO o DEVOLUCION. */
    private final String tipo;
}
