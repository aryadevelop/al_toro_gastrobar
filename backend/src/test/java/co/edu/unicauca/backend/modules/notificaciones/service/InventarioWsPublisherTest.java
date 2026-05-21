package co.edu.unicauca.backend.modules.notificaciones.service;

import co.edu.unicauca.backend.modules.notificaciones.dto.ws.StockActualizadoWsMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("InventarioWsPublisher — tests unitarios")
class InventarioWsPublisherTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private InventarioWsPublisher publisher;

    @Test
    @DisplayName("publicarStockActualizado para producto envía /topic/inventario con productoId y stock")
    void publicarStockActualizado_productoEnviaPayload() {
        BigDecimal stock = new BigDecimal("25.50");
        ArgumentCaptor<StockActualizadoWsMessage> captor =
                ArgumentCaptor.forClass(StockActualizadoWsMessage.class);

        publisher.publicarStockActualizado(7L, null, stock);

        verify(messagingTemplate).convertAndSend(eq("/topic/inventario"), captor.capture());
        StockActualizadoWsMessage payload = captor.getValue();
        assertThat(payload.productoId()).isEqualTo(7L);
        assertThat(payload.insumoId()).isNull();
        assertThat(payload.stockActual()).isEqualByComparingTo("25.50");
        verifyNoMoreInteractions(messagingTemplate);
    }

    @Test
    @DisplayName("publicarStockActualizado para insumo envía /topic/inventario con insumoId y stock")
    void publicarStockActualizado_insumoEnviaPayload() {
        BigDecimal stock = new BigDecimal("100.00");
        ArgumentCaptor<StockActualizadoWsMessage> captor =
                ArgumentCaptor.forClass(StockActualizadoWsMessage.class);

        publisher.publicarStockActualizado(null, 42L, stock);

        verify(messagingTemplate).convertAndSend(eq("/topic/inventario"), captor.capture());
        StockActualizadoWsMessage payload = captor.getValue();
        assertThat(payload.productoId()).isNull();
        assertThat(payload.insumoId()).isEqualTo(42L);
        assertThat(payload.stockActual()).isEqualByComparingTo("100.00");
        verifyNoMoreInteractions(messagingTemplate);
    }
}
