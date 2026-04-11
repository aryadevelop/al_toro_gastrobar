package co.edu.unicauca.backend.shared.enums;

/**
 * Enum para unidades de medida de insumos
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
