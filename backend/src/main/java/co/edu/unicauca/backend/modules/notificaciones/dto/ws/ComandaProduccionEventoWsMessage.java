package co.edu.unicauca.backend.modules.notificaciones.dto.ws;

import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.ComandaProduccionResumenResponse;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Mensaje WebSocket emitido a los tópicos de producción
 * ({@code /topic/produccion/cocina} y {@code /topic/produccion/barra}).
 *
 * @param tipo        tipo de evento que describe el cambio en la comanda
 * @param estacion    estación productora; debe coincidir con el tópico
 * @param comandaId   identificador de la comanda afectada
 * @param resumen     resumen completo. Presente cuando {@code tipo == CREADA}.
 *                    {@code null} en {@code ACTUALIZADA}, {@code ELIMINADA} y
 *                    {@code COMPLETADA}.
 * @param nuevoEstado nombre del estado destino. Presente cuando
 *                    {@code tipo == ACTUALIZADA}; {@code null} en los demás.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ComandaProduccionEventoWsMessage(
        TipoEventoProduccion tipo,
        String estacion,
        Long comandaId,
        ComandaProduccionResumenResponse resumen,
        String nuevoEstado) {
}
