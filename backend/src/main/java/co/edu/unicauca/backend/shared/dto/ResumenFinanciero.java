package co.edu.unicauca.backend.shared.dto;

import java.math.BigDecimal;

/**
 * Resumen financiero común a las vistas de cuenta, visita y reserva.
 *
 * <p>Centraliza los importes que el cliente y el cajero necesitan ver de forma consistente:
 * total de la pre-orden, costo de decoración, total a pagar, monto neto abonado y saldo
 * pendiente. Se construye con {@link ResumenFinancieroCalculator}.
 *
 * @param totalPreorden  suma de subtotales de los ítems
 * @param valorDecoracion costo adicional de la decoración; {@code null} si no aplica
 * @param totalAPagar    {@code totalPreorden + valorDecoracion}
 * @param montoAbonado   neto abonado ({@code anticipos − devoluciones})
 * @param saldoPendiente {@code totalAPagar − montoAbonado}
 */
public record ResumenFinanciero(
        BigDecimal totalPreorden,
        BigDecimal valorDecoracion,
        BigDecimal totalAPagar,
        BigDecimal montoAbonado,
        BigDecimal saldoPendiente) {
}
