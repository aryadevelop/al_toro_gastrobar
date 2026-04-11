package co.edu.unicauca.backend.shared.enums;

/**
 * Enum para estados de reserva
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
