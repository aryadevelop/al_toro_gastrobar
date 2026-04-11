package co.edu.unicauca.backend.shared.enums;

/**
 * Enum para los roles del sistema
 */
public enum RolNombre {
    CLIENTE("Cliente"),
    MESERO("Mesero"),
    CAJERO("Cajero"),
    COCINERO("Cocinero"),
    BARTENDER("Bartender"),
    ADM("Administrador");

    private final String descripcion;

    RolNombre(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
