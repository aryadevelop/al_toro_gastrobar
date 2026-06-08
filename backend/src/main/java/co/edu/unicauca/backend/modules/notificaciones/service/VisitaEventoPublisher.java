package co.edu.unicauca.backend.modules.notificaciones.service;

import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.ItemVisitaResponse;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.ComandaItem;
import co.edu.unicauca.backend.modules.mesas_comandas.mapper.VisitaEstadoMapper;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ComandaRepository;
import co.edu.unicauca.backend.modules.notificaciones.dto.ws.VisitaActualizadaWsMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * Publica al cliente el estado consolidado de los ítems de su visita.
 *
 * <p>Centraliza el patrón —antes duplicado en {@code ComandaProduccionService} y
 * {@code NotificacionService}— que recarga los ítems activos de una visita, calcula
 * el total y emite {@link VisitaActualizadaWsMessage} al tópico
 * {@code /topic/visita/{id}/orden}. Lo reutiliza toda transición que muta comandas,
 * incluido el ajuste del cajero.
 */
@Service
@RequiredArgsConstructor
public class VisitaEventoPublisher {

    private final ComandaRepository comandaRepository;
    private final VisitaEstadoMapper visitaEstadoMapper;
    private final NotificacionWsPublisher wsPublisher;

    /**
     * Recarga los ítems activos de la visita, calcula el total (ignorando precios nulos)
     * y publica el evento de orden actualizada al cliente.
     *
     * @param visitaId identificador de la visita cuyo estado se difunde
     */
    public void publicarOrden(Long visitaId) {
        // Recargar el estado vigente de los ítems activos tras la mutación
        List<ComandaItem> items = comandaRepository.findAllItemsActivosByVisita(visitaId);
        List<ItemVisitaResponse> itemsResponse = visitaEstadoMapper.toItemsVisitaResponse(items);
        // Total = Σ(precio × cantidad); las bebidas de menú (precio nulo/0) aportan cero
        BigDecimal total = items.stream()
                .filter(ci -> ci.getComandaItemPrecio() != null)
                .map(ci -> ci.getComandaItemPrecio().multiply(BigDecimal.valueOf(ci.getComandaItemCantidad())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        wsPublisher.publicarVisitaActualizada(visitaId,
                VisitaActualizadaWsMessage.builder()
                        .visitaId(visitaId)
                        .items(itemsResponse)
                        .total(total)
                        .build());
    }
}
