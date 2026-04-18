package co.edu.unicauca.backend.shared.enums;

/**
 * Dirección de un {@link co.edu.unicauca.backend.modules.inventario.entity.MovimientoInventario}.
 *
 * <ul>
 *   <li>{@code INGRESO} — entrada de stock: compra de insumos o producción de semielaborados.</li>
 *   <li>{@code EGRESO} — salida de stock: consumo durante la preparación de productos.</li>
 * </ul>
 */
public enum TipoMovimiento {
    INGRESO("Ingreso"),
    EGRESO("Egreso");

    private final String descripcion;

    TipoMovimiento(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
