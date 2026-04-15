package co.edu.unicauca.backend.shared.enums;

/**
 * Categorías de componentes modificables en un menú especial.
 * Permite agrupar las opciones de modificación por tipo en el formulario de pre-orden.
 */
public enum TipoComponenteMenu {
    ARROZ("Arroz"),
    PROTEINA("Proteína"),
    SALSA("Salsa"),
    ACOMPAÑAMIENTO("Acompañamiento"),
    BEBIDA("Bebida"),
    ENSALADA("Ensalada"),
    OTRO("Otro");

    private final String descripcion;

    TipoComponenteMenu(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
