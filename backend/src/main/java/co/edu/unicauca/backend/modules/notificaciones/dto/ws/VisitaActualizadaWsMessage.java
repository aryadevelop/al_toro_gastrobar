package co.edu.unicauca.backend.modules.notificaciones.dto.ws;

import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.ItemVisitaResponse;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

/**
 * Mensaje WebSocket publicado en {@code /topic/visita/{visitaId}/orden} cuando
 * el mesero agrega o modifica ítems de comanda.
 *
 * <p>El call site se añadirá en la HU de creación de visita (mesa).
 */
@Getter
@Builder
public class VisitaActualizadaWsMessage {

    private final Long visitaId;
    private final List<ItemVisitaResponse> items;
    private final BigDecimal total;
}
