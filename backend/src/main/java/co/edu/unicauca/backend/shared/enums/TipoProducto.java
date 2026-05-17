package co.edu.unicauca.backend.shared.enums;

/**
 * Clasificación de un {@link co.edu.unicauca.backend.modules.inventario.entity.Producto} según cómo se obtiene.
 *
 * <ul>
 *   <li>{@code VENTA_DIRECTA} — producto comercializado tal como se recibe del proveedor; no requiere preparación.</li>
 *   <li>{@code PREPARACION} — producto elaborado en cocina o barra a partir de insumos de la receta.</li>
 * </ul>
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
