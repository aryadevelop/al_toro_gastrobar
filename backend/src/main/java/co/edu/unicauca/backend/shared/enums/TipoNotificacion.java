package co.edu.unicauca.backend.shared.enums;

/**
 * Enum para tipos de notificación
 */
public enum TipoNotificacion {
    ATENCION("Atención"),
    PLATOS_LISTOS("Platos Listos"),
    BEBIDAS_LISTAS("Bebidas Listas"),
    CAMBIO("Cambio");

    private final String descripcion;

    TipoNotificacion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
