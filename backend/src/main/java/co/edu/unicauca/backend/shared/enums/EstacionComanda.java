package co.edu.unicauca.backend.shared.enums;

/**
 * Enum para estaciones de comanda
 */
public enum EstacionComanda {
    COCINA("Cocina"),
    BARRA("Barra");

    private final String descripcion;

    EstacionComanda(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
