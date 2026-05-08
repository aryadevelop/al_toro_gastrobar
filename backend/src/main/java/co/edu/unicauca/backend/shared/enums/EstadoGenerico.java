package co.edu.unicauca.backend.shared.enums;

/**
 * Estado binario de activación para entidades como {@link co.edu.unicauca.backend.modules.inventario.entity.Producto},
 * {@link co.edu.unicauca.backend.modules.inventario.entity.Insumo} y
 * {@link co.edu.unicauca.backend.modules.reservas.entity.Decoracion}.
 *
 * <ul>
 *   <li>{@code ACTIVO} — la entidad está operativa y visible en los flujos del sistema.</li>
 *   <li>{@code INACTIVO} — la entidad está deshabilitada y no aparece en los listados públicos.</li>
 * </ul>
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
