package co.edu.unicauca.backend.shared.enums;

/**
 * Enum para estados de mesa
 */
public enum EstadoMesa {
    ESPERA("En Espera"),
    EN_PREPARACION("En Preparación"),
    ATENDIDA("Atendida"),
    CERRADA("Cerrada");

    private final String descripcion;

    EstadoMesa(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
