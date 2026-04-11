package co.edu.unicauca.backend.shared.enums;

/**
 * Enum para tipos de producto
 */
public enum TipoProducto {
    VENTA_DIRECTA("Venta Directa"),
    PREPARACION("Preparación");

    private final String descripcion;

    TipoProducto(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
