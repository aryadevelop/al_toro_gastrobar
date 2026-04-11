package co.edu.unicauca.backend.shared.enums;

/**
 * Enum para estados de decoración y producto
 */
public enum EstadoGenerico {
    ACTIVO("Activo"),
    INACTIVO("Inactivo");

    private final String descripcion;

    EstadoGenerico(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
