package co.edu.unicauca.backend.modules.mesas_comandas.mapper;

import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.*;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.ComandaItem;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Mesa;
import co.edu.unicauca.backend.modules.notificaciones.entity.Notificacion;
import co.edu.unicauca.backend.shared.enums.EstadoComanda;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Mapper para transformaciones entity→DTO del módulo mesas_comandas.
 * Responsable de mapear Mesa, Notificacion y ComandaItem a sus DTOs de respuesta.
 */
@Component
public class MesaMapper {

    /**
     * Comparador para ordenar items por categoría de producto.
     * Orden: PLATO (0) → BEBIDA (1) → OTRO (2)
     */
    private static final Comparator<ComandaItem> COMPARATOR_POR_CATEGORIA =
            Comparator.comparing(item -> item.getProducto().getProductoCategoria().ordinal());

    /**
     * Mapea una Notificacion a NotificacionActivaResponse.
     *
     * @param notificacion entidad de notificación
     * @return DTO de notificación activa
     */
    public NotificacionActivaResponse toNotificacionActivaResponse(Notificacion notificacion) {
        return NotificacionActivaResponse.builder()
                .notificacionId(notificacion.getNotificacionId())
                .tipo(notificacion.getNotificacionTipo().name())  // Enum → String
                .fechaHora(notificacion.getNotificacionFechaHora())
                .build();
    }

    /**
     * Mapea una Mesa a MesaMapaResponse para el mapa de mesas.
     *
     * @param mesa entidad de mesa
     * @param notificaciones lista de notificaciones activas de la mesa
     * @param tieneBorrador flag indicando si la mesa tiene comanda en borrador
     * @param emailMeseroActual email del mesero que hace la petición
     * @return DTO de mesa para el mapa
     */
    public MesaMapaResponse toMesaMapaResponse(Mesa mesa,
                                                List<Notificacion> notificaciones,
                                                boolean tieneBorrador,
                                                String emailMeseroActual) {
        String emailMesero = mesa.getMesero().getUsuario().getUsuarioEmail();

        // RN-04: Solo enviar nombre si la mesa NO es propia
        String nombreMesero = emailMesero.equals(emailMeseroActual)
                ? null
                : mesa.getMesero().getEmpleadoNombre();

        List<NotificacionActivaResponse> notificacionesDto = notificaciones.stream()
                .map(this::toNotificacionActivaResponse)
                .collect(Collectors.toList());

        return MesaMapaResponse.builder()
                .visitaId(mesa.getVisitaId())
                .identificador(mesa.getMesaIdentificador())
                .numeroPersonas(mesa.getMesaNumeroPersonas())
                .estado(mesa.getMesaEstado().name())  // Enum → String
                .nombreMesero(nombreMesero)
                .emailMesero(emailMesero)
                .esMesaPropia(emailMesero.equals(emailMeseroActual))
                .tieneBorrador(tieneBorrador)
                .notificacionesActivas(notificacionesDto)
                .build();
    }

    /**
     * Agrupa items de comanda en producción según RN-06.
     *
     * <p><b>IMPORTANTE:</b> NO agrupar items en estado PENDIENTE.
     * Solo agrupar items en estados: EN_PREPARACION, LISTO, COMPLETADO.
     *
     * <p>Criterio de agrupación: (nombreProducto + descripcion + estadoComanda)
     *
     * @param items lista de items en producción
     * @return lista de items agrupados y ordenados por categoría
     */
    public List<ItemComandaEnProduccionResponse> agruparItemsEnProduccion(List<ComandaItem> items) {
        // Separar items por estado
        Map<Boolean, List<ComandaItem>> itemsPorAgrupabilidad = items.stream()
                .sorted(COMPARATOR_POR_CATEGORIA)
                .collect(Collectors.partitioningBy(
                        item -> item.getComanda().getComandaEstado() != EstadoComanda.PENDIENTE
                ));

        List<ComandaItem> itemsAgrupables = itemsPorAgrupabilidad.get(true);     // EN_PREPARACION, LISTO, COMPLETADO
        List<ComandaItem> itemsNoAgrupables = itemsPorAgrupabilidad.get(false);  // PENDIENTE

        // Agrupar solo los items agrupables
        Map<String, List<ComandaItem>> agrupados = itemsAgrupables.stream()
                .collect(Collectors.groupingBy(item -> {
                    String nombre = item.getProducto().getProductoNombre();
                    String desc = item.getComandaItemDescripcion() != null
                            ? item.getComandaItemDescripcion()
                            : "";
                    String estado = item.getComanda().getComandaEstado().name();
                    return nombre + "|" + desc + "|" + estado;
                }));

        // Mapear grupos a DTOs
        List<ItemComandaEnProduccionResponse> itemsAgrupadosDto = agrupados.values().stream()
                .map(grupo -> {
                    ComandaItem primero = grupo.get(0);
                    int cantidadTotal = grupo.stream()
                            .mapToInt(ComandaItem::getComandaItemCantidad)
                            .sum();

                    return ItemComandaEnProduccionResponse.builder()
                            .nombreProducto(primero.getProducto().getProductoNombre())
                            .descripcion(primero.getComandaItemDescripcion())
                            .cantidad(cantidadTotal)
                            .categoriaProducto(primero.getProducto().getProductoCategoria().name())
                            .estadoComanda(primero.getComanda().getComandaEstado().name())
                            .build();
                })
                .collect(Collectors.toList());

        // Mapear items no agrupables individualmente
        List<ItemComandaEnProduccionResponse> itemsNoAgrupadosDto = itemsNoAgrupables.stream()
                .map(item -> ItemComandaEnProduccionResponse.builder()
                        .nombreProducto(item.getProducto().getProductoNombre())
                        .descripcion(item.getComandaItemDescripcion())
                        .cantidad(item.getComandaItemCantidad())
                        .categoriaProducto(item.getProducto().getProductoCategoria().name())
                        .estadoComanda(item.getComanda().getComandaEstado().name())
                        .build())
                .collect(Collectors.toList());

        // Combinar ambas listas
        List<ItemComandaEnProduccionResponse> todosLosItems = new ArrayList<>();
        todosLosItems.addAll(itemsAgrupadosDto);
        todosLosItems.addAll(itemsNoAgrupadosDto);

        return todosLosItems;
    }

    /**
     * Mapea una Mesa a MesaDetalleResponse para detalle completo.
     *
     * @param mesa entidad de mesa
     * @param itemsAgrupados items de comanda agrupados según RN-06
     * @return DTO de detalle de mesa
     */
    public MesaDetalleResponse toMesaDetalleResponse(Mesa mesa,
                                                      List<ItemComandaEnProduccionResponse> itemsAgrupados) {
        String nombreCliente = null;
        if (mesa.getVisita().getCliente() != null) {
            nombreCliente = mesa.getVisita().getCliente().getClienteNombre();
        }

        // Obtener notas de reserva si existe
        String notasReserva = null;
        if (mesa.getVisita().getReserva() != null) {
            notasReserva = mesa.getVisita().getReserva().getReservaNotas();
        }

        return MesaDetalleResponse.builder()
                .visitaId(mesa.getVisitaId())
                .identificador(mesa.getMesaIdentificador())
                .nombreCliente(nombreCliente)
                .horaLlegada(mesa.getVisita().getVisitaFechaHoraInicio())
                .numeroPersonas(mesa.getMesaNumeroPersonas())
                .estado(mesa.getMesaEstado().name())  // Enum → String
                .notasReserva(notasReserva)
                .itemsComanda(itemsAgrupados)
                .build();
    }

    /**
     * Mapea identificador y items a MesaItemsProduccionResponse.
     *
     * @param identificadorMesa identificador de la mesa
     * @param itemsAgrupados items de comanda agrupados según RN-06
     * @return DTO de items en producción
     */
    public MesaItemsProduccionResponse toMesaItemsProduccionResponse(
            String identificadorMesa,
            List<ItemComandaEnProduccionResponse> itemsAgrupados) {
        return MesaItemsProduccionResponse.builder()
                .identificadorMesa(identificadorMesa)
                .itemsEnProduccion(itemsAgrupados)
                .build();
    }
}
