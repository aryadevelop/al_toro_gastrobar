package co.edu.unicauca.backend.modules.mesas_comandas.mapper;

import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.ComandaItemResponse;
import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.VisitaDetalleResponse;
import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.VisitaResumenResponse;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.ComandaItem;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Mesa;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Visita;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Zona;
import co.edu.unicauca.backend.modules.pagos_caja.entity.Abono;
import co.edu.unicauca.backend.modules.pagos_caja.entity.Venta;
import co.edu.unicauca.backend.modules.reservas.dto.response.AbonoItemResponse;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Mapper para convertir entidades de visitas en sus DTOs de respuesta.
 */
@Component
public class VisitaMapper {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    /**
     * Comparador para ordenar items por categoría de producto.
     * Orden: PLATO (0) → BEBIDA (1) → OTRO (2)
     */
    private static final Comparator<ComandaItem> COMPARATOR_POR_CATEGORIA =
            Comparator.comparing(item -> item.getProducto().getProductoCategoria().ordinal());

    /**
     * Convierte una {@link Visita} en el DTO de resumen de visitas,
     * 
     * @param visita   entidad de la visita
     * @param mesaOpt  mesa asignada a la visita (vacío si no hubo asignación)
     * @param ventaOpt venta de la visita (vacío si la cuenta no fue cerrada)
     * @return {@link VisitaResumenResponse} listo para serializar
     */
    public VisitaResumenResponse toResumen(
            Visita visita,
            Optional<Mesa> mesaOpt,
            Optional<Venta> ventaOpt) {

        // Si la venta está presente, la visita se considera cerrada; de lo contrario, sigue activa.
        String estadoVisita = ventaOpt.isPresent() ? "CERRADA" : "ATENDIDA";

        // Para el resumen, se prioriza mostrar la zona de la mesa asignada. Si no hay mesa, se muestra la zona de la reserva (si existe).
        String zonaNombre = mesaOpt
                .map(Mesa::getZona)
                .map(Zona::getZonaNombre)
                .orElse(visita.getReserva() != null && visita.getReserva().getZona() != null
                        ? visita.getReserva().getZona().getZonaNombre() : null);

        return VisitaResumenResponse.builder()
                .visitaId(visita.getVisitaId())
                .reservaId(visita.getReserva() != null ? visita.getReserva().getReservaId() : null)
                .fechaHoraLlegada(visita.getVisitaFechaHoraInicio().format(FORMATTER))
                .numeroPersonas(mesaOpt.map(Mesa::getMesaNumeroPersonas).orElse(null))
                .mesaIdentificador(mesaOpt.map(Mesa::getMesaIdentificador).orElse(null))
                .estadoVisita(estadoVisita)
                .montoTotal(ventaOpt.map(Venta::getVentaTotal).orElse(null))
                .zonaNombre(zonaNombre)
                .build();
    }

    /**
     * Convierte una {@link Visita} en el DTO de detalle completo, incluyendo los
     * ítems de todas las comandas de la visita, el historial de abonos y el total
     * de la venta.
     *
     * @param visita          entidad de la visita
     * @param itemsComanda    ítems consolidados de todas las comandas de la visita
     * @param abonosOpt       abonos y devoluciones registrados (vacío si no hay reserva)
     * @param ventaOpt        venta de la visita (vacío si la cuenta no fue cerrada)
     * @param mesaOpt         mesa asignada a la visita (vacío si no hubo asignación)
     * @return {@link VisitaDetalleResponse} listo para serializar
     */
    public VisitaDetalleResponse toDetalle(
            Visita visita,
            List<ComandaItem> itemsComanda,
            Optional<List<Abono>> abonosOpt,
            Optional<Venta> ventaOpt,
            Optional<Mesa> mesaOpt) {

        // Convierte los items de comanda en su DTO correspondiente; si no hay detalles, se deja como null para omitir el campo en la respuesta.
        List<ComandaItemResponse> itemsDto = itemsComanda.isEmpty() ? null : agruparItems(itemsComanda);

        // Convierte los abonos en su DTO correspondiente; si no hay abonos, se deja como null para omitir el campo en la respuesta.
        List<AbonoItemResponse> abonosDto = abonosOpt
                .filter(abonos -> !abonos.isEmpty())
                .map(abonos -> abonos.stream()
                        .map(a -> AbonoItemResponse.builder()
                                .abonoId(a.getAbonoId())
                                .monto(a.getAbonoMonto())
                                .fechaHora(a.getAbonoFechaHora().format(FORMATTER))
                                .metodo(a.getAbonoMetodo().name())
                                .tipo(a.getAbonoTipo().name())
                                .build())
                        .collect(Collectors.toList()))
                .orElse(null);

        // Si la venta está presente, la visita se considera cerrada; de lo contrario, sigue activa.
        String estadoVisita = ventaOpt.isPresent() ? "CERRADA" : "ATENDIDA";

        // Para el detalle, se prioriza mostrar la zona de la mesa asignada. Si no hay mesa, se muestra la zona de la reserva (si existe).
        String zonaNombre = mesaOpt
                .map(Mesa::getZona)
                .map(Zona::getZonaNombre)
                .orElse(visita.getReserva() != null && visita.getReserva().getZona() != null
                        ? visita.getReserva().getZona().getZonaNombre() : null);

        return VisitaDetalleResponse.builder()
                .visitaId(visita.getVisitaId())
                .reservaId(visita.getReserva() != null ? visita.getReserva().getReservaId() : null)
                .clienteId(visita.getCliente() != null ? visita.getCliente().getUsuarioId() : null)
                .clienteNombre(visita.getCliente() != null ? visita.getCliente().getClienteNombre() : null)
                .fechaHoraLlegada(visita.getVisitaFechaHoraInicio().format(FORMATTER))
                .fechaHoraSalida(visita.getVisitaFechaHoraFin() != null ? visita.getVisitaFechaHoraFin().format(FORMATTER) : null)
                .numeroPersonas(mesaOpt.map(Mesa::getMesaNumeroPersonas).orElse(null))
                .estadoVisita(estadoVisita)
                .zonaNombre(zonaNombre)
                .mesaIdentificador(mesaOpt.map(Mesa::getMesaIdentificador).orElse(null))
                .meseroNombre(mesaOpt.map(m -> m.getMesero().getEmpleadoNombre()).orElse(null))
                .decoracionNombre(visita.getReserva() != null && visita.getReserva().getDecoracion() != null
                        ? visita.getReserva().getDecoracion().getDecoracionNombre() : null)
                .notas(visita.getReserva() != null ? visita.getReserva().getReservaNotas() : null)
                .itemsComanda(itemsDto)
                .abonos(abonosDto)
                .subtotalCuenta(ventaOpt.map(Venta::getVentaSubtotal).orElse(null))
                .descuentoCuenta(ventaOpt.map(Venta::getVentaDescuento).orElse(null))
                .totalCuenta(ventaOpt.map(Venta::getVentaTotal).orElse(null))
                .build();
    }

    /**
     * Agrupa items de comanda por (nombreProducto + descripcion) y suma cantidades.
     *
     * <p>Utilizado en detalle de visita para mostrar al cliente una vista consolidada
     * de todos los items pedidos, sin importar en cuántas comandas se dividieron.
     *
     * @param items lista de items a agrupar
     * @return lista de items agrupados ordenados por nombre
     */
    private List<ComandaItemResponse> agruparItems(List<ComandaItem> items) {
        // Clave de agrupación: nombreProducto + "|" + descripcion (null-safe)
        Map<String, List<ComandaItem>> agrupados = items.stream()
            .sorted(COMPARATOR_POR_CATEGORIA)
            .collect(Collectors.groupingBy(item ->
                item.getProducto().getProductoNombre() + "|" +
                (item.getComandaItemDescripcion() != null ? item.getComandaItemDescripcion() : "")
            ));

        return agrupados.values().stream()
            .map(grupo -> {
                ComandaItem primero = grupo.get(0);
                int cantidadTotal = grupo.stream()
                    .mapToInt(ComandaItem::getComandaItemCantidad)
                    .sum();

                return ComandaItemResponse.builder()
                    .nombreProducto(primero.getProducto().getProductoNombre())
                    .descripcion(primero.getComandaItemDescripcion())
                    .cantidad(cantidadTotal)
                    .precioUnitario(primero.getComandaItemPrecio())
                    .subtotal(primero.getComandaItemPrecio()
                        .multiply(BigDecimal.valueOf(cantidadTotal)))
                    .build();
            })
            .sorted(Comparator.comparing(ComandaItemResponse::getNombreProducto))
            .toList();
    }

}
