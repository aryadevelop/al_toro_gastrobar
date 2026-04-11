package co.edu.unicauca.backend.shared.enums;

/**
 * Enum para tipos de abono
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
