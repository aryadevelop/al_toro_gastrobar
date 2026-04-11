package co.edu.unicauca.backend.shared.enums;

/**
 * Enum para estados de notificación
 */
public enum EstadoNotificacion {
    ACTIVA("Activa"),
    ATENDIDA("Atendida");

    private final String descripcion;

    EstadoNotificacion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
