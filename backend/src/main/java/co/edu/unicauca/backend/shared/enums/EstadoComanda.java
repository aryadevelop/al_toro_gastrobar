package co.edu.unicauca.backend.shared.enums;

/**
 * Estados posibles de una {@link co.edu.unicauca.backend.modules.mesas_comandas.entity.Comanda}.
 *
 * <p>Ciclo de vida:
 * <pre>
 *   PRE_RESERVA → PENDIENTE → EN_PREPARACION → LISTO → COMPLETADO
 * </pre>
 *
 * <ul>
 *   <li>{@code PRE_RESERVA} — pre-orden registrada al crear una reserva; aún no enviada a producción.</li>
 *   <li>{@code PENDIENTE} — comanda enviada a la estación de producción, en espera de ser tomada.</li>
 *   <li>{@code EN_PREPARACION} — la estación de producción inició la preparación.</li>
 *   <li>{@code LISTO} — preparación finalizada; pendiente de entrega al cliente.</li>
 *   <li>{@code COMPLETADO} — entregada al cliente; ciclo cerrado.</li>
 * </ul>
 */
public enum EstadoComanda {
    PRE_RESERVA("Pre-Reserva"),
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
