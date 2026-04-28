package co.edu.unicauca.backend.modules.mesas_comandas.mapper;

import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.EstadoVisitaResponse;
import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.ItemVisitaResponse;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Comanda;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.ComandaItem;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Visita;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ComandaItemRepository;
import co.edu.unicauca.backend.modules.notificaciones.entity.Notificacion;
import co.edu.unicauca.backend.shared.enums.EstadoComanda;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Comparator;
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
@RequiredArgsConstructor
public class VisitaEstadoMapper {

    private final ComandaItemRepository comandaItemRepository;

    /**
     * Comparador para ordenar items por categoría de producto.
     * Orden: PLATO (0) → BEBIDA (1) → OTRO (2)
     */
    private static final Comparator<ComandaItem> COMPARATOR_POR_CATEGORIA =
            Comparator.comparing(item -> item.getProducto().getProductoCategoria().ordinal());

    /**
     * Construye {@link ItemVisitaResponse} desde un ítem de comanda.
     *
     * @param item    ítem de comanda con producto y precio
     * @param estado  estado de la comanda padre (determina "En preparación" o "Servido")
     * @return DTO listo para serializar
     */
    public ItemVisitaResponse toItemVisitaResponse(ComandaItem item, EstadoComanda estado) {
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
     * @param asistenciaActiva    notificación activa, o empty si no hay solicitud pendiente
     * @return DTO completo del estado de la visita
     */
    public EstadoVisitaResponse toEstadoVisitaResponse(
            Visita visita,
            String mesaIdentificador,
            List<ItemVisitaResponse> items,
            Optional<Notificacion> asistenciaActiva) {

        BigDecimal total = items.stream()
                .map(ItemVisitaResponse::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return EstadoVisitaResponse.builder()
                .visitaId(visita.getVisitaId())
                .mesaIdentificador(mesaIdentificador)
                .visitaCerrada(visita.getVisitaFechaHoraFin() != null)
                .items(items)
                .total(total)
                .asistenciaSolicitada(asistenciaActiva.isPresent())
                .notificacionAsistenciaId(
                        asistenciaActiva.map(Notificacion::getNotificacionId).orElse(null))
                .build();
    }

    /**
     * Mapea los items de todas las comandas a DTOs, ordenados por categoría de producto.
     *
     * <p>Excluye comandas en estado PRE_RESERVA y ordena items por categoría (PLATO → BEBIDA → OTRO).
     *
     * @param comandas lista de comandas de la visita
     * @return lista de items ordenados por categoría
     */
    public List<ItemVisitaResponse> mapearItemsOrdenados(List<Comanda> comandas) {
        return comandas.stream()
                .filter(c -> c.getComandaEstado() != EstadoComanda.PRE_RESERVA)
                .flatMap(c -> comandaItemRepository.findByComanda_ComandaId(c.getComandaId())
                        .stream()
                        .sorted(COMPARATOR_POR_CATEGORIA)
                        .map(item -> toItemVisitaResponse(item, c.getComandaEstado())))
                .collect(Collectors.toList());
    }

    /**
     * Convierte el estado interno de la comanda al texto visible para el cliente.
     * LISTO y COMPLETADO se muestran como "Servido"; cualquier otro estado como "En preparación".
     */
    private String resolverEstadoItem(EstadoComanda estado) {
        return (estado == EstadoComanda.LISTO || estado == EstadoComanda.COMPLETADO)
                ? "Servido"
                : "En preparación";
    }
}
