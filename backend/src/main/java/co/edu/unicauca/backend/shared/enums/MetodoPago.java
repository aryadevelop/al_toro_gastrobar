package co.edu.unicauca.backend.shared.enums;

/**
 * Método de pago aceptado en las transacciones del restaurante.
 *
 * <p>Usado en {@link co.edu.unicauca.backend.modules.pagos_caja.entity.Venta} y
 * {@link co.edu.unicauca.backend.modules.pagos_caja.entity.Abono}.
 *
 * <ul>
 *   <li>{@code EFECTIVO} — pago en moneda física.</li>
 *   <li>{@code TARJETA} — pago con tarjeta débito o crédito.</li>
 *   <li>{@code TRANSFERENCIA} — pago por transferencia bancaria.</li>
 *   <li>{@code OTRO} — cualquier otro medio de pago no listado.</li>
 * </ul>
 */
public enum MetodoPago {
    EFECTIVO("Efectivo"),
    TARJETA("Tarjeta"),
    TRANSFERENCIA("Transferencia"),
    OTRO("Otro");

    private final String descripcion;

    MetodoPago(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
