package co.edu.unicauca.backend.modules.notificaciones.dto.ws;

import java.util.List;

import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.ItemBorradorResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Mensaje WebSocket emitido al tópico {@code /topic/estacion/{estacion}} cuando una
 * comanda pasa a PENDIENTE. 
 * 
 * Lo consume el dashboard de la estación correspondiente: COCINA recibe platos, BARRA recibe bebidas.
 */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ComandaEstacionWsMessage {
    /** Identificador de la comanda recién enviada. */
    private Long comandaId;
    /** Identificador de la visita dueña de la comanda. */
    private Long visitaId;
    /** Estación destino: {@code "COCINA"} o {@code "BARRA"}. */
    private String estacion;
    /** Ítems de la comanda, ordenados por nombre de producto. */
    private List<ItemBorradorResponse> items;
}
