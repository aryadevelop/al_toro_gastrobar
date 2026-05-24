package co.edu.unicauca.backend.modules.reservas.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * DTO de respuesta con el resumen financiero de los pagos asociados a una reserva.
 *
 * <p>Consolida importes de anticipos y devoluciones para facilitar la conciliación
 * de caja y la visualización del estado de pago de la reserva.
 *
 * <p>Campos que pueden ser {@code null} cuando no aplican se omiten de la serialización
 * JSON gracias a {@link JsonInclude.Include#NON_NULL}.
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResumenPagoResponse {

    /** Identificador único de la reserva a la que pertenece este resumen. */
    private final Long reservaId;

    /** Nombre completo del cliente titular de la reserva. */
    private final String clienteNombre;

    /** Fecha y hora de llegada programada en formato {@code yyyy-MM-dd'T'HH:mm:ss}. */
    private final String fechaHoraLlegada;

    /** Número de comensales registrados en la reserva. */
    private final Integer numeroPersonas;

    /** Estado actual de la reserva ({@code PENDIENTE}, {@code CONFIRMADA}, etc.). */
    private final String estado;

    /** Tipo de reserva: {@code BASICA} o {@code ESPECIAL}. */
    private final String tipo;

    /**
     * Importe total de la reserva, calculado como pre-orden más decoración;
     * {@code BigDecimal.ZERO} cuando no existe pre-orden ni decoración con costo.
     */
    private final BigDecimal totalReserva;

    /**
     * Suma acumulada de todos los anticipos registrados para esta reserva;
     * {@code BigDecimal.ZERO} cuando no se ha registrado ningún anticipo.
     */
    private final BigDecimal totalAnticipado;

    /**
     * Suma acumulada de todas las devoluciones registradas para esta reserva;
     * {@code BigDecimal.ZERO} cuando no se ha registrado ninguna devolución.
     */
    private final BigDecimal totalDevuelto;

    /**
     * Diferencia neta entre anticipos y devoluciones ({@code totalAnticipado - totalDevuelto});
     * {@code BigDecimal.ZERO} cuando no existen movimientos.
     */
    private final BigDecimal netoAbonado;

    /**
     * Saldo que el cliente aún debe anticipar ({@code max(totalReserva − netoAbonado, 0)}).
     * Presente únicamente cuando el estado de la reserva es {@code CONFIRMADA};
     * {@code null} en cualquier otro estado (se omite del JSON).
     */
    private final BigDecimal pendientePorAbonar;

    /**
     * Monto que el restaurante aún debe devolver al cliente (igual a {@code netoAbonado}).
     * Presente únicamente cuando el estado de la reserva es {@code CANCELADA} o {@code DEVUELTA}
     * (en este último caso será {@code BigDecimal.ZERO} tras un reembolso total);
     * {@code null} en cualquier otro estado (se omite del JSON).
     */
    private final BigDecimal pendientePorDevolver;
}
