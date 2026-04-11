package co.edu.unicauca.backend.shared.enums;

/**
 * Enum para estados de comanda
 */
public enum EstadoComanda {
    PENDIENTE("Pendiente"),
    EN_PREPARACION("En Preparación"),
    LISTO("Listo"),
    COMPLETADO("Completado");

    private final String descripcion;

    EstadoComanda(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
