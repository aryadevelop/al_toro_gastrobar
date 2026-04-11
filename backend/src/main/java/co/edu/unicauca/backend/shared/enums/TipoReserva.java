package co.edu.unicauca.backend.shared.enums;

/**
 * Enum para tipos de reserva
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
