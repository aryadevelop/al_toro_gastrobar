package co.edu.unicauca.backend.shared.enums;

/**
 * Estado de una {@link co.edu.unicauca.backend.modules.notificaciones.entity.Notificacion}.
 *
 * <ul>
 *   <li>{@code ACTIVA} — la notificación fue emitida y está pendiente de atención.</li>
 *   <li>{@code ATENDIDA} — el empleado destinatario revisó y resolvió la notificación.</li>
 * </ul>
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
