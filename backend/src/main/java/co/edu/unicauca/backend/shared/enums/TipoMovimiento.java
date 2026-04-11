package co.edu.unicauca.backend.shared.enums;

/**
 * Enum para tipos de movimiento de inventario
 */
public enum TipoMovimiento {
    INGRESO("Ingreso"),
    EGRESO("Egreso");

    private final String descripcion;

    TipoMovimiento(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
