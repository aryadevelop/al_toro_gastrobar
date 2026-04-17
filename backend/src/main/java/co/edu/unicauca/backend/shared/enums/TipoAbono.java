package co.edu.unicauca.backend.shared.enums;

/**
 * Tipo de movimiento registrado en {@link co.edu.unicauca.backend.modules.pagos_caja.entity.Abono}.
 *
 * <ul>
 *   <li>{@code ANTICIPO} — pago parcial previo realizado por el cliente al confirmar una reserva especial.</li>
 *   <li>{@code DEVOLUCION} — reintegro del anticipo al cliente cuando la reserva es cancelada.</li>
 * </ul>
 */
public enum TipoAbono {
    ANTICIPO("Anticipo"),
    DEVOLUCION("Devolución");

    private final String descripcion;

    TipoAbono(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
