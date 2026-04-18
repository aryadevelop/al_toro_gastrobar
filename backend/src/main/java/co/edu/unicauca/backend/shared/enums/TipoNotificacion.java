package co.edu.unicauca.backend.shared.enums;

/**
 * Motivo de una {@link co.edu.unicauca.backend.modules.notificaciones.entity.Notificacion} enviada entre empleados.
 *
 * <ul>
 *   <li>{@code ATENCION} — solicitud de atención en mesa; el mesero debe acercarse.</li>
 *   <li>{@code PLATOS_LISTOS} — cocina indica que los platos de la mesa están listos para servir.</li>
 *   <li>{@code BEBIDAS_LISTAS} — barra indica que las bebidas de la mesa están listas.</li>
 *   <li>{@code CAMBIO} — solicitud de cambio en un pedido ya enviado.</li>
 * </ul>
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
