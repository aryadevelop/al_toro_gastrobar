package co.edu.unicauca.backend.shared.enums;

/**
 * Estado del ciclo de vida de una {@link co.edu.unicauca.backend.modules.reservas.entity.Reserva}.
 *
 * <ul>
 *   <li>{@code PENDIENTE} — reserva especial creada; requiere confirmación (pago de anticipo).</li>
 *   <li>{@code CONFIRMADA} — reserva activa; el cliente se presentará en la fecha acordada.</li>
 *   <li>{@code ATENDIDA} — los comensales llegaron y la visita fue registrada en el sistema.</li>
 *   <li>{@code CANCELADA} — reserva anulada antes de la fecha de llegada.</li>
 *   <li>{@code DEVUELTA} — reserva cancelada con devolución de anticipo registrada.</li>
 *   <li>{@code INASISTENCIA} — los comensales no se presentaron en la fecha acordada.</li>
 * </ul>
 */
public enum EstadoReserva {
    PENDIENTE("Pendiente"),
    CONFIRMADA("Confirmada"),
    ATENDIDA("Atendida"),
    CANCELADA("Cancelada"),
    DEVUELTA("Devuelta"),
    INASISTENCIA("Inasistencia");

    private final String descripcion;

    EstadoReserva(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
