package co.edu.unicauca.backend.modules.mesas_comandas.mapper;

import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.EstadoVisitaResponse;
import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.ItemVisitaResponse;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.ComandaItem;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Visita;
import co.edu.unicauca.backend.modules.notificaciones.entity.Notificacion;
import co.edu.unicauca.backend.modules.pagos_caja.entity.Abono;
import co.edu.unicauca.backend.modules.reservas.entity.Decoracion;
import co.edu.unicauca.backend.shared.dto.ResumenFinanciero;
import co.edu.unicauca.backend.shared.enums.EstadoComanda;
import co.edu.unicauca.backend.shared.enums.TipoAbono;
import co.edu.unicauca.backend.shared.util.ResumenFinancieroCalculator;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Mapper para construir las respuestas del estado de visita activa.
 *
 * <p>Centraliza toda la lógica de conversión entity→DTO para que
 * {@link co.edu.unicauca.backend.modules.mesas_comandas.service.VisitaEstadoService}
 * no contenga lógica de presentación.
 */
@Component
public class VisitaEstadoMapper {

    /**
     * Construye {@link ItemVisitaResponse} desde un ítem de comanda.
     *
     * @param item   ítem de comanda con producto y precio
     * @param estado estado de la comanda padre
     * @return DTO listo para serializar
     */
    private ItemVisitaResponse toItemVisitaResponse(ComandaItem item, EstadoComanda estado) {
        BigDecimal subtotal = item.getComandaItemPrecio()
                .multiply(BigDecimal.valueOf(item.getComandaItemCantidad()));

        return ItemVisitaResponse.builder()
                .comandaItemId(item.getComandaItemId())
                .nombreProducto(item.getProducto().getProductoNombre())
                .descripcion(item.getComandaItemDescripcion())
                .cantidad(item.getComandaItemCantidad())
                .categoriaProducto(item.getProducto().getProductoCategoria().name())
                .estadoItem(resolverEstadoItem(estado))
                .precioUnitario(item.getComandaItemPrecio())
                .subtotal(subtotal)
                .build();
    }

    /**
     * Construye {@link EstadoVisitaResponse} con todos los campos calculados.
     *
     * @param visita              visita activa
     * @param mesaIdentificador   etiqueta de la mesa; {@code null} si no hay mesa asignada
     * @param items               lista de ítems ya mapeados
     * @param abonos              abonos de la reserva de la visita; vacío si es walk-in o sin abonos
     * @param asistenciaActiva    notificación activa, o empty si no hay solicitud pendiente
     * @return DTO completo del estado de la visita
     */
    public EstadoVisitaResponse toEstadoVisitaResponse(
            Visita visita,
            String mesaIdentificador,
            List<ItemVisitaResponse> items,
            List<Abono> abonos,
            Optional<Notificacion> asistenciaActiva) {

        // Total de la pre-orden = suma de subtotales de los ítems mostrados
        BigDecimal totalPreorden = items.stream()
                .map(ItemVisitaResponse::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        ResumenFinanciero resumen = ResumenFinancieroCalculator.calcular(
                totalPreorden, valorDecoracion(visita), anticipos(abonos), devoluciones(abonos));

        return EstadoVisitaResponse.builder()
                .visitaId(visita.getVisitaId())
                .mesaIdentificador(mesaIdentificador)
                .visitaCerrada(visita.getVisitaFechaHoraFin() != null)
                .items(items)
                .totalPreorden(resumen.totalPreorden())
                .valorDecoracion(resumen.valorDecoracion())
                .totalAPagar(resumen.totalAPagar())
                .montoAbonado(resumen.montoAbonado())
                .saldoPendiente(resumen.saldoPendiente())
                .asistenciaSolicitada(asistenciaActiva.isPresent())
                .notificacionAsistenciaId(
                        asistenciaActiva.map(Notificacion::getNotificacionId).orElse(null))
                .build();
    }

    /** Costo de la decoración de la reserva de la visita; {@code null} si no aplica. */
    private BigDecimal valorDecoracion(Visita visita) {
        if (visita.getReserva() != null && visita.getReserva().getDecoracion() != null) {
            Decoracion d = visita.getReserva().getDecoracion();
            return d.getDecoracionCostoAdicional();
        }
        return null;
    }

    /** Suma de abonos de tipo ANTICIPO. */
    private BigDecimal anticipos(List<Abono> abonos) {
        return abonos.stream().filter(a -> a.getAbonoTipo() == TipoAbono.ANTICIPO)
                .map(Abono::getAbonoMonto).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Suma de abonos de tipo DEVOLUCION. */
    private BigDecimal devoluciones(List<Abono> abonos) {
        return abonos.stream().filter(a -> a.getAbonoTipo() == TipoAbono.DEVOLUCION)
                .map(Abono::getAbonoMonto).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Convierte una lista de {@link ComandaItem} a DTOs de visita, leyendo el estado
     * directamente de la comanda padre de cada ítem.
     *
     * @param items ítems con comanda y producto ya cargados
     * @return lista de respuestas; vacía si {@code items} es null o vacía
     */
    public List<ItemVisitaResponse> toItemsVisitaResponse(List<ComandaItem> items) {
        if (items == null || items.isEmpty()) return List.of();
        return items.stream()
                .map(item -> toItemVisitaResponse(item, item.getComanda().getComandaEstado()))
                .collect(Collectors.toList());
    }

    /**
     * Convierte el estado interno de la comanda al texto visible para el cliente.
     *
     * <p>Mapping:
     * <ul>
     *   <li>{@code BORRADOR}, {@code PENDIENTE} → {@code "En espera"}</li>
     *   <li>{@code EN_PREPARACION} → {@code "En preparación"}</li>
     *   <li>{@code LISTO}, {@code COMPLETADO} → {@code "Servido"}</li>
     *   <li>cualquier otro estado → {@code "En espera"} (defensivo)</li>
     * </ul>
     */
    private String resolverEstadoItem(EstadoComanda estado) {
        return switch (estado) {
            case BORRADOR, PENDIENTE -> "En espera";
            case EN_PREPARACION      -> "En preparación";
            case LISTO, COMPLETADO   -> "Servido";
            default                  -> "En espera";
        };
    }
}
