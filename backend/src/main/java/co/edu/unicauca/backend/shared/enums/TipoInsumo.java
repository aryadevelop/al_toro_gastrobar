package co.edu.unicauca.backend.shared.enums;

/**
 * Clasifica los insumos según su nivel de procesamiento.
 * MATERIA_PRIMA: ingrediente comprado directamente (carne, queso, vino).
 * SEMIELABORADO: preparación de cocina alistada en batch (tostones, puré, salsa de ajo).
 *                El cocinero registra cuántas porciones preparó; su stock determina
 *                si el plato que lo requiere está disponible.
 */
public enum TipoInsumo {
    MATERIA_PRIMA("Materia Prima"),
    SEMIELABORADO("Semielaborado");

    private final String descripcion;

    TipoInsumo(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
