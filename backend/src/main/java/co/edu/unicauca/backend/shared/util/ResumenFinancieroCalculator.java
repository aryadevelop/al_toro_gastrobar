package co.edu.unicauca.backend.shared.util;

import co.edu.unicauca.backend.shared.dto.ResumenFinanciero;

import java.math.BigDecimal;

/**
 * Calculadora pura (sin entidades ni estado) de los importes de un {@link ResumenFinanciero}.
 *
 * <p>Centraliza las fórmulas de total a pagar, monto neto abonado y saldo pendiente para que
 * todas las vistas (cuenta, visita activa, detalle de visita, detalle de reserva, historial)
 * presenten los mismos valores con la misma lógica. Cada llamador suma sus propios ítems y
 * abonos y delega aquí el resto.
 */
public final class ResumenFinancieroCalculator {

    private ResumenFinancieroCalculator() {
    }

    /**
     * Calcula el resumen financiero a partir de importes ya agregados por el llamador.
     *
     * @param totalPreorden   suma de subtotales de ítems; {@code null} se trata como cero
     * @param valorDecoracion costo de decoración; {@code null} si no aplica (se preserva en el resultado para omitirlo)
     * @param anticipos       suma de abonos de tipo ANTICIPO; {@code null} se trata como cero
     * @param devoluciones    suma de abonos de tipo DEVOLUCION; {@code null} se trata como cero
     * @return resumen con {@code totalAPagar}, {@code montoAbonado} (neto) y {@code saldoPendiente}
     */
    public static ResumenFinanciero calcular(BigDecimal totalPreorden, BigDecimal valorDecoracion,
                                             BigDecimal anticipos, BigDecimal devoluciones) {
        BigDecimal preorden = nz(totalPreorden);
        // Total a pagar = pre-orden + decoración (la decoración nula cuenta como cero)
        BigDecimal totalAPagar = preorden.add(nz(valorDecoracion));
        // Monto abonado NETO = anticipos − devoluciones
        BigDecimal montoAbonado = nz(anticipos).subtract(nz(devoluciones));
        BigDecimal saldoPendiente = totalAPagar.subtract(montoAbonado);
        // valorDecoracion se preserva tal cual (incluido null) para que el DTO pueda omitirlo
        return new ResumenFinanciero(preorden, valorDecoracion, totalAPagar, montoAbonado, saldoPendiente);
    }

    /** Devuelve {@link BigDecimal#ZERO} si el valor es {@code null}; en otro caso el propio valor. */
    private static BigDecimal nz(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
