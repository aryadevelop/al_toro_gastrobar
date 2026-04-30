package co.edu.unicauca.backend.modules.mesas_comandas.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * DTO para obtener información de items en producción de una mesa.
 */
@Getter
@Builder
public class MesaItemsProduccionResponse {

    /** Identificador de la mesa */
    private final String identificadorMesa;

    /** Resumen de items enviados a producción (PENDIENTE, EN_PREPARACION, LISTO, COMPLETADO) */
    private final List<ItemComandaEnProduccionResponse> itemsEnProduccion;
}
