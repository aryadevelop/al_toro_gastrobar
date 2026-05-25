package co.edu.unicauca.backend.modules.mesas_comandas.service;

import co.edu.unicauca.backend.modules.mesas_comandas.dto.request.AjustarItemsRequest;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.ComandaItem;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Visita;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ComandaItemRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ComandaRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.VisitaRepository;
import co.edu.unicauca.backend.modules.notificaciones.service.VisitaEventoPublisher;
import co.edu.unicauca.backend.modules.pagos_caja.dto.response.CuentaPreliminarResponse;
import co.edu.unicauca.backend.modules.pagos_caja.service.CuentaService;
import co.edu.unicauca.backend.shared.exception.BusinessException;
import co.edu.unicauca.backend.shared.exception.ErrorCode;
import co.edu.unicauca.backend.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Aplica el ajuste batch de ítems de la cuenta por parte del cajero.
 *
 * <p>Modifica cantidades y precios y elimina ítems de forma transaccional sobre las
 * comandas ya en producción. Respeta el acoplamiento del par de menú especial
 * (COCINA+BARRA comparten {@code menuGrupo}): la cantidad se sincroniza en ambos y la
 * eliminación retira el par completo. Solo los ítems modificados (con descripción)
 * admiten edición de precio. Tras aplicar, republica el estado al cliente.
 */
@Service
@RequiredArgsConstructor
public class CuentaAjusteService {

    private final VisitaRepository visitaRepository;
    private final ComandaRepository comandaRepository;
    private final ComandaItemRepository comandaItemRepository;
    private final VisitaEventoPublisher visitaEventoPublisher;
    private final CuentaService cuentaService;

    /**
     * Aplica los cambios de cantidad/precio y las eliminaciones a los ítems de una visita.
     *
     * <p>Flujo:
     * <ol>
     *   <li>Bloquea la visita (mutex del cierre) y valida que esté activa.</li>
     *   <li>Si no hay cambios, devuelve la cuenta actual sin tocar nada.</li>
     *   <li>Carga los ítems activos e indexa por id.</li>
     *   <li>Procesa eliminaciones (par completo en menús).</li>
     *   <li>Procesa modificaciones (cantidad sincronizada en menús; precio solo en modificados).</li>
     *   <li>Republica la orden al cliente y devuelve la cuenta recalculada.</li>
     * </ol>
     *
     * @param visitaId identificador de la visita
     * @param request  ítems a modificar y/o eliminar
     * @return la cuenta preliminar recalculada
     * @throws ResourceNotFoundException si la visita o algún ítem no existen en la visita
     * @throws BusinessException         si la visita está cerrada o se edita el precio de un ítem no modificado
     */
    @Transactional
    public CuentaPreliminarResponse ajustarItems(Long visitaId, AjustarItemsRequest request) {
        // 1. Lock pesimista de la visita: serializa con asignación de cliente y pago
        Visita visita = visitaRepository.findByIdForUpdate(visitaId)
                .orElseThrow(() -> new ResourceNotFoundException("Visita", visitaId));
        if (visita.getVisitaFechaHoraFin() != null) {
            throw new BusinessException(ErrorCode.INVALID_STATE,
                    "La visita ya está cerrada.", HttpStatus.CONFLICT);
        }

        List<AjustarItemsRequest.ItemAjuste> cambios = request.getItems() != null ? request.getItems() : List.of();
        List<Long> eliminados = request.getEliminados() != null ? request.getEliminados() : List.of();

        // 2. Sin cambios → no-op: si no hay cambios, sale del modo)
        if (cambios.isEmpty() && eliminados.isEmpty()) {
            return cuentaService.obtenerCuenta(visitaId);
        }

        // 3. Índice de los ítems activos de la visita (fuente de verdad y validación de pertenencia)
        List<ComandaItem> activos = comandaRepository.findAllItemsActivosByVisita(visitaId);
        Map<Long, ComandaItem> porId = activos.stream()
                .collect(Collectors.toMap(ComandaItem::getComandaItemId, Function.identity()));

        // 4. Eliminaciones (el par completo cuando es menú especial)
        for (Long id : eliminados) {
            ComandaItem item = requerirItem(porId, id);
            bloquearComanda(item);
            if (item.getComandaItemMenuGrupo() != null) {
                eliminarPar(activos, item.getComandaItemMenuGrupo());
            } else {
                comandaItemRepository.delete(item);
            }
        }

        // 5. Modificaciones de cantidad/precio
        for (AjustarItemsRequest.ItemAjuste ajuste : cambios) {
            ComandaItem item = requerirItem(porId, ajuste.getComandaItemId());
            bloquearComanda(item);
            aplicarCantidad(activos, item, ajuste.getCantidad());
            if (ajuste.getPrecio() != null) {
                aplicarPrecio(item, ajuste.getPrecio());
            }
        }

        // 6. Republicar al cliente y devolver la cuenta recalculada
        visitaEventoPublisher.publicarOrden(visitaId);
        return cuentaService.obtenerCuenta(visitaId);
    }

    /** Recupera un ítem por id dentro de los activos de la visita o lanza 404 (cubre no-existe y no-pertenece). */
    private ComandaItem requerirItem(Map<Long, ComandaItem> porId, Long id) {
        ComandaItem item = porId.get(id);
        if (item == null) {
            throw new ResourceNotFoundException("ComandaItem", id);
        }
        return item;
    }

    /** Bloquea la comanda del ítem para serializar con transiciones de producción concurrentes. */
    private void bloquearComanda(ComandaItem item) {
        comandaRepository.findByIdForUpdate(item.getComanda().getComandaId());
    }

    /** Aplica la cantidad; en menús especiales la sincroniza en todo el par del grupo. */
    private void aplicarCantidad(List<ComandaItem> activos, ComandaItem item, Integer cantidad) {
        if (item.getComandaItemMenuGrupo() != null) {
            // Menú especial: misma cantidad en COCINA y BARRA
            for (ComandaItem par : delMismoGrupo(activos, item.getComandaItemMenuGrupo())) {
                par.setComandaItemCantidad(cantidad);
                comandaItemRepository.save(par);
            }
        } else {
            item.setComandaItemCantidad(cantidad);
            comandaItemRepository.save(item);
        }
    }

    /** Aplica el precio solo si el ítem es modificado (tiene descripción); si no, regla violada. */
    private void aplicarPrecio(ComandaItem item, BigDecimal precio) {
        if (item.getComandaItemDescripcion() == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR,
                    "Solo se puede editar el precio de ítems modificados.", HttpStatus.CONFLICT);
        }
        item.setComandaItemPrecio(precio);
        comandaItemRepository.save(item);
    }

    /** Elimina todos los ítems (COCINA+BARRA) del grupo de menú indicado. */
    private void eliminarPar(List<ComandaItem> activos, String grupo) {
        for (ComandaItem par : delMismoGrupo(activos, grupo)) {
            comandaItemRepository.delete(par);
        }
    }

    /** Devuelve los ítems activos que pertenecen al grupo de menú indicado. */
    private List<ComandaItem> delMismoGrupo(List<ComandaItem> activos, String grupo) {
        return activos.stream()
                .filter(ci -> grupo.equals(ci.getComandaItemMenuGrupo()))
                .toList();
    }
}
