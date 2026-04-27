package co.edu.unicauca.backend.modules.reservas.mapper;

import co.edu.unicauca.backend.modules.mesas_comandas.entity.ComandaItem;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.ComandaMenuModificacion;
import co.edu.unicauca.backend.modules.reservas.dto.response.PreOrdenItemResponse;

import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper para convertir ítems de una {@link co.edu.unicauca.backend.modules.mesas_comandas.entity.Comanda}
 * en estado {@code PRE_RESERVA} en sus DTOs de respuesta.
 *
 * <p>Transforma cada {@link co.edu.unicauca.backend.modules.mesas_comandas.entity.ComandaItem}
 * junto con sus {@link co.edu.unicauca.backend.modules.mesas_comandas.entity.ComandaMenuModificacion}
 * en un {@link PreOrdenDetalleResponse} para exponer la pre-orden del cliente.
 */
@Component
public class PreOrdenMapper {

    /**
     * Comparador para ordenar items por categoría de producto.
     * Orden: PLATO (0) → BEBIDA (1) → OTRO (2)
     */
    public static final Comparator<ComandaItem> COMPARATOR_POR_CATEGORIA =
            Comparator.comparing(item -> item.getProducto().getProductoCategoria().ordinal());

    /**
     * Convierte un {@link co.edu.unicauca.backend.modules.mesas_comandas.entity.ComandaItem}
     * y sus modificaciones de menú especial en el DTO de detalle.
     *
     * @param detalle detalle de comanda a convertir
     * @param mods    modificaciones de menú especial asociadas al ítem; vacía si no aplica
     * @return {@link PreOrdenDetalleResponse} con producto, cantidad, precio snapshot y modificaciones
     */
    public PreOrdenItemResponse toDetalleResponse(ComandaItem detalle,
                                                     List<ComandaMenuModificacion> mods) {
        List<PreOrdenItemResponse.OpcionModificacionSeleccionada> modificacionesDto = mods.stream()
                .map(m -> PreOrdenItemResponse.OpcionModificacionSeleccionada.builder()
                        .opcionId(m.getOpcion().getOpcionId())
                        .opcionNombre(m.getOpcion().getOpcionNombre())
                        .tipoComponente(m.getOpcion().getTipoComponente().name())
                        .build())
                .collect(Collectors.toList());

        return PreOrdenItemResponse.builder()
                .comandaItemId(detalle.getComandaItemId())
                .productoId(detalle.getProducto().getProductoId())
                .productoNombre(detalle.getProducto().getProductoNombre())
                .cantidad(detalle.getComandaItemCantidad())
                .precioUnitario(detalle.getComandaItemPrecio())
                .descripcion(detalle.getComandaItemDescripcion())
                .modificaciones(modificacionesDto.isEmpty() ? null : modificacionesDto)
                .build();
    }
}
