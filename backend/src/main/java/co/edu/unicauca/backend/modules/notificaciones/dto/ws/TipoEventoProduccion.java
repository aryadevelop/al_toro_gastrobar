package co.edu.unicauca.backend.modules.notificaciones.dto.ws;

/**
 * Tipo de evento WebSocket asociado al ciclo de vida de una comanda en una
 * estación de producción.
 *
 * <ul>
 *   <li>{@code CREADA} — La comanda transicionó de {@code BORRADOR} a
 *       {@code PENDIENTE} y debe aparecer en la columna de pendientes.</li>
 *   <li>{@code ACTUALIZADA} — La comanda cambió de estado dentro del tablero
 *       (por ejemplo {@code PENDIENTE→EN_PREPARACION} o
 *       {@code EN_PREPARACION→LISTO}); el campo {@code nuevoEstado} del
 *       payload indica la columna destino.</li>
 *   <li>{@code ELIMINADA} — La comanda dejó de estar visible en el tablero.</li>
 *   <li>{@code COMPLETADA} — El mesero registró el servicio de la comanda.</li>
 * </ul>
 */
public enum TipoEventoProduccion {
    CREADA,
    ACTUALIZADA,
    ELIMINADA,
    COMPLETADA
}
