package co.edu.unicauca.backend.shared.enums;

/**
 * Unidad de medida utilizada para cuantificar el stock de un
 * {@link co.edu.unicauca.backend.modules.inventario.entity.Insumo}.
 *
 * <ul>
 *   <li>{@code KG} — kilogramo.</li>
 *   <li>{@code G} — gramo.</li>
 *   <li>{@code L} — litro.</li>
 *   <li>{@code ML} — mililitro.</li>
 *   <li>{@code UNIDAD} — unidad discreta (piezas, porciones, etc.).</li>
 *   <li>{@code DOCENA} — grupo de doce unidades.</li>
 *   <li>{@code OTRO} — unidad no contemplada en los valores anteriores.</li>
 * </ul>
 */
public enum UnidadMedida {
    KG("Kilogramo"),
    G("Gramo"),
    L("Litro"),
    ML("Mililitro"),
    UNIDAD("Unidad"),
    DOCENA("Docena"),
    OTRO("Otro");

    private final String descripcion;

    UnidadMedida(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
