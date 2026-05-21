package co.edu.unicauca.backend.modules.mesas_comandas.mapper;

import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.*;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.ComandaItem;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Mesa;
import co.edu.unicauca.backend.modules.notificaciones.entity.Notificacion;
import co.edu.unicauca.backend.shared.enums.EstadoComanda;
import co.edu.unicauca.backend.shared.enums.EstadoMesa;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.MonthDay;
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
                .tipo(notificacion.getNotificacionTipo().name())
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
     * @param esCajero flag indicando si el caller es CAJERO (ignora ownership y siempre llena nombreMesero)
     * @return DTO de mesa para el mapa
     */
    public MesaMapaResponse toMesaMapaResponse(Mesa mesa,
                                                List<Notificacion> notificaciones,
                                                boolean tieneBorrador,
                                                String emailMeseroActual,
                                                boolean esCajero) {
        String emailMesero = mesa.getMesero().getUsuario().getUsuarioEmail();

        boolean esMesaPropia;
        String nombreMesero;
        if (esCajero) {
            esMesaPropia = false;
            nombreMesero = mesa.getMesero().getEmpleadoNombre();
        } else {
            esMesaPropia = emailMesero.equals(emailMeseroActual);
            nombreMesero = esMesaPropia ? null : mesa.getMesero().getEmpleadoNombre();
        }

        // Ordenar notificaciones de más tarde a más temprano
        List<NotificacionActivaResponse> notificacionesDto = notificaciones.stream()
                .sorted(Comparator.comparing(Notificacion::getNotificacionFechaHora).reversed())
                .map(this::toNotificacionActivaResponse)
                .collect(Collectors.toList());

        return MesaMapaResponse.builder()
                .mesaId(mesa.getVisitaId())
                .visitaId(mesa.getVisitaId())
                .identificador(mesa.getMesaIdentificador())
                .numeroPersonas(mesa.getMesaNumeroPersonas())
                .estado(mesa.getMesaEstado().name())
                .nombreMesero(nombreMesero)
                .esMesaPropia(esMesaPropia)
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
     * <p>Criterio de agrupación: (nombreProducto + descripcion)
     *
     * <p>Estados:
     * <ul>
     *   <li>PENDIENTE → "PENDIENTE"</li>
     *   <li>EN_PREPARACION, LISTO, COMPLETADO → "CONFIRMADO"</li>
     * </ul>
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

        // Agrupar solo los items agrupables (por nombre + descripcion, sin estado)
        Map<String, List<ComandaItem>> agrupados = itemsAgrupables.stream()
                .collect(Collectors.groupingBy(item -> {
                    String nombre = item.getProducto().getProductoNombre();
                    String desc = item.getComandaItemDescripcion() != null
                            ? item.getComandaItemDescripcion()
                            : "";
                    return nombre + "|" + desc;
                }));

        // Mapear grupos a DTOs (items confirmados = EN_PREPARACION, LISTO, COMPLETADO)
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
                            .estadoComanda("CONFIRMADO")
                            .build();
                })
                .collect(Collectors.toList());

        // Mapear items no agrupables individualmente (PENDIENTE)
        List<ItemComandaEnProduccionResponse> itemsNoAgrupadosDto = itemsNoAgrupables.stream()
                .map(item -> ItemComandaEnProduccionResponse.builder()
                        .nombreProducto(item.getProducto().getProductoNombre())
                        .descripcion(item.getComandaItemDescripcion())
                        .cantidad(item.getComandaItemCantidad())
                        .categoriaProducto(item.getProducto().getProductoCategoria().name())
                        .estadoComanda("PENDIENTE")
                        .build())
                .collect(Collectors.toList());

        // Combinar ambas listas y ordenar por categoría
        List<ItemComandaEnProduccionResponse> todosLosItems = new ArrayList<>();
        todosLosItems.addAll(itemsAgrupadosDto);
        todosLosItems.addAll(itemsNoAgrupadosDto);

        // Ordenar resultado final por categoría: PLATO → BEBIDA → OTRO
        return todosLosItems.stream()
                .sorted(Comparator.comparing(ItemComandaEnProduccionResponse::getCategoriaProducto,
                        (cat1, cat2) -> {
                            // Mapeo manual: PLATO=0, BEBIDA=1, OTRO=2
                            int orden1 = cat1.equals("PLATO") ? 0 : cat1.equals("BEBIDA") ? 1 : 2;
                            int orden2 = cat2.equals("PLATO") ? 0 : cat2.equals("BEBIDA") ? 1 : 2;
                            return Integer.compare(orden1, orden2);
                        }))
                .collect(Collectors.toList());
    }

    /**
     * Mapea una Mesa a MesaDetalleResponse para detalle completo.
     *
     * @param mesa entidad de mesa
     * @param itemsOriginales items de comanda originales
     * @param itemsAgrupados items de comanda agrupados
     * @param esCajero flag indicando si el caller es CAJERO (para exponer campos de cliente y generación de cuenta)
     * @return DTO de detalle de mesa
     */
    public MesaDetalleResponse toMesaDetalleResponse(Mesa mesa,
                                                      List<ComandaItem> itemsOriginales,
                                                      List<ItemComandaEnProduccionResponse> itemsAgrupados,
                                                      boolean esCajero) {
        String nombreCliente = null;
        if (mesa.getVisita().getCliente() != null) {
            nombreCliente = mesa.getVisita().getCliente().getClienteNombre();
        }

        // Obtener notas de reserva si existe
        String notasReserva = null;
        if (mesa.getVisita().getReserva() != null) {
            notasReserva = mesa.getVisita().getReserva().getReservaNotas();
        }

        // Extraer notas de comandas únicas y concatenarlas
        String notasComandas = itemsOriginales.stream()
                .map(item -> item.getComanda().getComandaNotas())
                .filter(nota -> nota != null && !nota.trim().isEmpty())
                .distinct()
                .collect(Collectors.joining(" | "));

        // Si no hay notas, retornar null en lugar de string vacío
        if (notasComandas.isEmpty()) {
            notasComandas = null;
        }

        Long clienteId = null;
        Integer puntosFidelizacion = null;
        Boolean esCumpleanos = null;
        Boolean puedeGenerarCuenta = null;
        if (esCajero) {
            if (mesa.getVisita().getCliente() != null) {
                clienteId = mesa.getVisita().getCliente().getUsuarioId();
                puntosFidelizacion = mesa.getVisita().getCliente().getClientePuntos();
                esCumpleanos = esCumpleanosHoy(mesa.getVisita().getCliente().getClienteFechaNacimiento());
            }
            puedeGenerarCuenta = mesa.getMesaEstado() == EstadoMesa.ATENDIDA;
        }

        return MesaDetalleResponse.builder()
                .mesaId(mesa.getVisitaId())
                .visitaId(mesa.getVisitaId())
                .identificador(mesa.getMesaIdentificador())
                .nombreCliente(nombreCliente)
                .horaLlegada(mesa.getVisita().getVisitaFechaHoraInicio())
                .numeroPersonas(mesa.getMesaNumeroPersonas())
                .estado(mesa.getMesaEstado().name())
                .notasReserva(notasReserva)
                .notasMesa(mesa.getMesaNotas())
                .notasComandas(notasComandas)
                .itemsComanda(itemsAgrupados)
                .clienteId(clienteId)
                .puntosFidelizacion(puntosFidelizacion)
                .esCumpleanos(esCumpleanos)
                .puedeGenerarCuenta(puedeGenerarCuenta)
                .build();
    }

    /**
     * Indica si la fecha de nacimiento dada coincide en día y mes con la fecha actual.
     *
     * @param fechaNacimiento fecha de nacimiento del cliente (puede ser null)
     * @return true si día y mes coinciden con la fecha actual; false en caso contrario
     *         o si la fecha es null
     */
    static boolean esCumpleanosHoy(LocalDate fechaNacimiento) {
        if (fechaNacimiento == null) return false;
        MonthDay hoy = MonthDay.from(LocalDate.now());
        return MonthDay.from(fechaNacimiento).equals(hoy);
    }

    /**
     * Mapea identificador y items a MesaItemsProduccionResponse.
     *
     * @param identificadorMesa identificador de la mesa
     * @param itemsAgrupados items de comanda agrupados según
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
