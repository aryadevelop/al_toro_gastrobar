package co.edu.unicauca.backend.shared.enums;

/**
 * Enum para estados de rol
 */
public enum RolEstado {
    ACTIVO("Activo"),
    INACTIVO("Inactivo");

    private final String descripcion;

    RolEstado(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
