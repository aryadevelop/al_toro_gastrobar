package co.edu.unicauca.backend.shared.enums;

/**
 * Estación de producción a la que se dirige una comanda.
 *
 * <ul>
 *   <li>{@code COCINA} — comanda enviada al área de preparación de alimentos.</li>
 *   <li>{@code BARRA} — comanda enviada al área de bebidas.</li>
 * </ul>
 */
public enum EstacionComanda {
    COCINA("Cocina"),
    BARRA("Barra");

    private final String descripcion;

    EstacionComanda(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
