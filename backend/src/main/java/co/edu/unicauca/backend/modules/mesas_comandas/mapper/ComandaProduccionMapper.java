package co.edu.unicauca.backend.modules.mesas_comandas.mapper;

import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.ComandaProduccionDetalleResponse;
import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.ComandaProduccionResumenResponse;
import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.ItemDetalleResponse;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Comanda;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.ComandaItem;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.ComandaMenuModificacion;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Mesa;
import co.edu.unicauca.backend.shared.enums.CategoriaProducto;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Transforma entidades del dominio de comandas a los DTOs expuestos por las
 * pantallas de producción.
 */
@Component
public class ComandaProduccionMapper {

    private static final Comparator<ItemDetalleResponse> ITEM_BY_NOMBRE =
            Comparator.comparing(ItemDetalleResponse::getProductoNombre);

    /**
     * Construye el resumen de una comanda para su uso en el tablero de
     * producción y en los mensajes WebSocket del tipo {@code CREADA}.
     *
     * @param comanda     comanda fuente; debe tener su {@code visita}
     *                    y los campos de estado y estación poblados
     * @param mesa        mesa asociada a la visita; puede ser {@code null} si
     *                    la mesa todavía no fue asignada en el momento de la
     *                    consulta
     * @param totalItems  suma de las cantidades de los ítems de la comanda;
     *                    se inyecta calculado externamente para evitar la
     *                    carga lazy de la colección
     * @return resumen poblado con los campos requeridos por las vistas de
     *         producción
     */
    public ComandaProduccionResumenResponse toResumen(Comanda comanda, Mesa mesa, int totalItems) {
        return ComandaProduccionResumenResponse.builder()
                .comandaId(comanda.getComandaId())
                .estacion(comanda.getComandaEstacion().name())
                .comandaEstado(comanda.getComandaEstado().name())
                .mesaIdentificador(mesa != null ? mesa.getMesaIdentificador() : null)
                .meseroNombre(meseroNombre(mesa))
                .totalItems(totalItems)
                .createdAt(comanda.getCreatedAt())
                .fechaHoraInicio(comanda.getComandaFechaHoraInicio())
                .fechaHoraListo(comanda.getComandaFechaHoraListo())
                .notas(comanda.getComandaNotas())
                .build();
    }

    /**
     * Construye el detalle completo de una comanda agrupando sus ítems por
     * categoría de producto.
     *
     * @param comanda comanda fuente; debe tener su {@code visita}
     *                y los campos de estado y estación poblados
     * @param mesa    mesa asociada; puede ser {@code null} si todavía no
     *                fue asignada
     * @param items   ítems de la comanda con su producto cargado y, dentro
     *                de la misma transacción, con su colección de
     *                {@link ComandaMenuModificacion} accesible
     * @return detalle poblado con tres listas de ítems agrupadas por categoría
     */
    public ComandaProduccionDetalleResponse toDetalle(Comanda comanda, Mesa mesa, List<ComandaItem> items) {
        return ComandaProduccionDetalleResponse.builder()
                .comandaId(comanda.getComandaId())
                .estacion(comanda.getComandaEstacion().name())
                .comandaEstado(comanda.getComandaEstado().name())
                .mesaIdentificador(mesa != null ? mesa.getMesaIdentificador() : null)
                .meseroNombre(meseroNombre(mesa))
                .createdAt(comanda.getCreatedAt())
                .fechaHoraInicio(comanda.getComandaFechaHoraInicio())
                .fechaHoraListo(comanda.getComandaFechaHoraListo())
                .notas(comanda.getComandaNotas())
                .platos(filtrarYMapear(items, CategoriaProducto.PLATO))
                .bebidas(filtrarYMapear(items, CategoriaProducto.BEBIDA))
                .otros(filtrarYMapear(items, CategoriaProducto.OTRO))
                .build();
    }

    private List<ItemDetalleResponse> filtrarYMapear(List<ComandaItem> items, CategoriaProducto categoria) {
        return items.stream()
                .filter(it -> it.getProducto().getProductoCategoria() == categoria)
                .map(this::toItem)
                .sorted(ITEM_BY_NOMBRE)
                .collect(Collectors.toList());
    }

    private ItemDetalleResponse toItem(ComandaItem item) {
        return ItemDetalleResponse.builder()
                .comandaItemId(item.getComandaItemId())
                .productoId(item.getProducto().getProductoId())
                .productoNombre(item.getProducto().getProductoNombre())
                .categoriaProducto(item.getProducto().getProductoCategoria().name())
                .cantidad(item.getComandaItemCantidad())
                .descripcion(item.getComandaItemDescripcion())
                .modificacionesMenu(extraerOpciones(item))
                .build();
    }

    private List<String> extraerOpciones(ComandaItem item) {
        List<ComandaMenuModificacion> mods = item.getModificaciones();
        if (mods == null || mods.isEmpty()) {
            return List.of();
        }
        return mods.stream()
                .map(ComandaMenuModificacion::getOpcion)
                .map(o -> o.getOpcionNombre())
                .sorted()
                .collect(Collectors.toList());
    }

    private String meseroNombre(Mesa mesa) {
        return Optional.ofNullable(mesa)
                .map(Mesa::getMesero)
                .map(e -> e.getEmpleadoNombre())
                .orElse(null);
    }
}
