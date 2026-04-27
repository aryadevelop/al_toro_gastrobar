package co.edu.unicauca.backend.shared.enums;

/**
 * Estados del ciclo de vida de una comanda.
 *
 * <p>Flujo típico: PRE_RESERVA → BORRADOR → PENDIENTE → EN_PREPARACION → LISTO → COMPLETADO
 *
 * <ul>
 *   <li><b>PRE_RESERVA:</b> Comanda creada desde reserva, aún no enviada a producción</li>
 *   <li><b>BORRADOR:</b> Comanda walk-in guardada pero no enviada a producción</li>
 *   <li><b>PENDIENTE:</b> Enviada a cocina/barra, esperando preparación</li>
 *   <li><b>EN_PREPARACION:</b> Estación trabajando en los items</li>
 *   <li><b>LISTO:</b> Terminado, pendiente de marcar servido</li>
 *   <li><b>COMPLETADO:</b> Servido al cliente</li>
 * </ul>
 */
public enum EstadoComanda {

    PRE_RESERVA("Pre-Reserva"),

    BORRADOR("Borrador"),

    PENDIENTE("Pendiente"),

    EN_PREPARACION("En Preparación"),

    LISTO("Listo"),

    COMPLETADO("Completado");

    private final String displayName;

    EstadoComanda(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
