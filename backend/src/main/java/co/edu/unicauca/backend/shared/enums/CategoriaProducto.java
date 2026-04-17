package co.edu.unicauca.backend.shared.enums;

/**
 * Categoría de presentación de un {@link co.edu.unicauca.backend.modules.produccion.entity.Producto}
 * en la carta del restaurante.
 *
 * <ul>
 *   <li>{@code PLATO} — preparación de cocina que se sirve como alimento principal o entrada.</li>
 *   <li>{@code BEBIDA} — bebida alcohólica o no alcohólica.</li>
 *   <li>{@code OTRO} — producto no clasificable en las categorías anteriores.</li>
 * </ul>
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
