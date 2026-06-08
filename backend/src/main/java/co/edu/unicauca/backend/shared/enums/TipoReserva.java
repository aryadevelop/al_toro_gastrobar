package co.edu.unicauca.backend.shared.enums;

/**
 * Categoría de una {@link co.edu.unicauca.backend.modules.reservas.entity.Reserva} según sus características.
 *
 * <p>El tipo se determina automáticamente al crear la reserva: es {@code ESPECIAL} si incluye
 * decoración con costo adicional o al menos un ítem de menú especial en la pre-orden.
 *
 * <ul>
 *   <li>{@code BASICA} — reserva sin decoración con costo ni menú especial; se confirma de inmediato.</li>
 *   <li>{@code ESPECIAL} — reserva con decoración o menú especial; requiere pago de anticipo para confirmarse.</li>
 * </ul>
 */
public enum TipoReserva {
    BASICA("Básica"),
    ESPECIAL("Especial");

    private final String descripcion;

    TipoReserva(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
