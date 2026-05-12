package co.edu.unicauca.backend.modules.notificaciones.dto.ws;

/**
 * Mensaje WebSocket emitido al tópico {@code /topic/comandas/completado}
 * cuando un mesero registra el servicio de platos o bebidas.
 *
 * <p>Los dashboards del cocinero y del bartender, suscritos a este tópico,
 * eliminan en tiempo real la comanda de su columna "Listas" sin necesidad
 * de refrescar la página.
 *
 * @param comandaId identificador de la comanda marcada como {@code COMPLETADO}
 * @param estacion  estación productora: {@code "COCINA"} o {@code "BARRA"}
 */
public record ComandaCompletadaWsMessage(Long comandaId, String estacion) {}
