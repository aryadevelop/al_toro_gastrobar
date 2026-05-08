package co.edu.unicauca.backend.shared.enums;

/**
 * Categorías de componentes modificables en un menú especial.
 * Permite agrupar las opciones de modificación por tipo en el formulario de pre-orden.
 */
public enum TipoComponenteMenu {
    ARROZ("Arroz"),
    SALSA_PROTEINA_1("Salsa Proteína 1"),
    SALSA_PROTEINA_2("Salsa Proteína 2");

    private final String descripcion;

    TipoComponenteMenu(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
