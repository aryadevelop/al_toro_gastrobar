package co.edu.unicauca.backend.modules.notificaciones.dto.ws;

import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.ComandaProduccionResumenResponse;

/**
 * Mensaje WebSocket emitido a los tópicos de producción
 * ({@code /topic/produccion/cocina} y {@code /topic/produccion/barra}).
 *
 * <p>Concentra los tres eventos relevantes del tablero de producción: 
 * aparición de una comanda nueva, retirada del tablero y registro
 * del servicio de la comanda.
 *
 * @param tipo      tipo de evento que describe el cambio en la comanda
 * @param estacion  estación productora; debe coincidir con el tópico al que se
 *                  emite el mensaje ({@code "COCINA"} o {@code "BARRA"})
 * @param comandaId identificador de la comanda afectada
 * @param resumen   resumen completo de la comanda. Está presente cuando
 *                  {@code tipo == CREADA} para permitir al cliente añadir la
 *                  comanda al tablero sin una llamada adicional. Vale
 *                  {@code null} para {@code ELIMINADA} y {@code COMPLETADA},
 *                  donde solo el identificador es relevante.
 */
public record ComandaProduccionEventoWsMessage(
        TipoEventoProduccion tipo,
        String estacion,
        Long comandaId,
        ComandaProduccionResumenResponse resumen) {
}
