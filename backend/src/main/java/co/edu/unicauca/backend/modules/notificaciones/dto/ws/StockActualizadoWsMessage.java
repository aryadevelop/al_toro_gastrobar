package co.edu.unicauca.backend.modules.notificaciones.dto.ws;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;

/**
 * Mensaje WebSocket emitido al tópico {@code /topic/inventario} cuando un
 * ajuste manual modifica el stock de un producto o insumo.
 *
 * <p>Permite que cualquier pantalla abierta (tablero de producción, formulario
 * de borrador del mesero, módulo de inventario) refresque el stock en tiempo
 * real. Exactamente uno de {@code productoId} / {@code insumoId} viaja con valor.
 *
 * @param productoId  identificador del producto ajustado; {@code null} si el
 *                    ajuste fue sobre un insumo
 * @param insumoId    identificador del insumo ajustado; {@code null} si el
 *                    ajuste fue sobre un producto
 * @param stockActual stock resultante luego del ajuste
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record StockActualizadoWsMessage(
        Long productoId,
        Long insumoId,
        BigDecimal stockActual) {
}
