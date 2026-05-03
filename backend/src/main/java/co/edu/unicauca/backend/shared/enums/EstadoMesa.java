package co.edu.unicauca.backend.shared.enums;

/**
 * Estado operativo de una {@link co.edu.unicauca.backend.modules.mesas_comandas.entity.Mesa}.
 *
 * <ul>
 *   <li>{@code ESPERA} — la mesa tiene comanda en borrador pero aún no ha iniciado la preparación.</li>
 *   <li>{@code EN_PREPARACION} — al menos una comanda de la mesa está siendo preparada o está en estado "En preparacion", "Listo", "Completado".</li>
 *   <li>{@code ATENDIDA} — todos los pedidos fueron entregados y no hay comandas en borrador; la cuenta está pendiente de cierre.</li>
 *   <li>{@code CERRADA} — la cuenta fue cerrada y la mesa queda disponible.</li>
 * </ul>
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
