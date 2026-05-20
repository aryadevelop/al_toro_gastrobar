package co.edu.unicauca.backend.modules.notificaciones.service;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import co.edu.unicauca.backend.modules.notificaciones.dto.ws.StockActualizadoWsMessage;

import java.math.BigDecimal;

/**
 * Publicador de mensajes WebSocket para eventos de inventario.
 *
 * <p>Destino: {@code /topic/inventario} (broadcast).
 */
@Service
@RequiredArgsConstructor
public class InventarioWsPublisher {

    private static final String TOPIC_INVENTARIO = "/topic/inventario";

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Publica un evento de stock actualizado tras un ajuste manual de inventario.
     *
     * @param productoId  identificador del producto ajustado; {@code null} si fue un insumo
     * @param insumoId    identificador del insumo ajustado; {@code null} si fue un producto
     * @param stockActual stock resultante luego del ajuste
     */
    public void publicarStockActualizado(Long productoId, Long insumoId, BigDecimal stockActual) {
        messagingTemplate.convertAndSend(TOPIC_INVENTARIO,
                new StockActualizadoWsMessage(productoId, insumoId, stockActual));
    }
}
