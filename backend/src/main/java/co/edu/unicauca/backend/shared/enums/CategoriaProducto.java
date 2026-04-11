package co.edu.unicauca.backend.shared.enums;

/**
 * Enum para categorías de producto
 */
public enum CategoriaProducto {
    PLATO("Plato"),
    BEBIDA("Bebida"),
    OTRO("Otro");

    private final String descripcion;

    CategoriaProducto(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
