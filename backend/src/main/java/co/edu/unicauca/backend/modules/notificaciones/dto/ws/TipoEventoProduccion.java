package co.edu.unicauca.backend.modules.notificaciones.dto.ws;

/**
 * Tipo de evento WebSocket asociado al ciclo de vida de una comanda en una
 * estación de producción.
 *
 * <ul>
 *   <li>{@code CREADA} — La comanda transicionó de {@code BORRADOR} a
 *       {@code PENDIENTE} y debe aparecer en la columna de pendientes del
 *       tablero.</li>
 *   <li>{@code ELIMINADA} — La comanda dejó de estar visible en el tablero por
 *       cambio de estado o eliminación física y debe retirarse de cualquier
 *       columna donde figure.</li>
 *   <li>{@code COMPLETADA} — El mesero registró el servicio de la comanda.</li>
 * </ul>
 */
public enum TipoEventoProduccion {
    CREADA,
    ELIMINADA,
    COMPLETADA
}
