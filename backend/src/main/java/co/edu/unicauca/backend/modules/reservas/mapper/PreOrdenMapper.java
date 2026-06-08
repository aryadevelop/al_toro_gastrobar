package co.edu.unicauca.backend.modules.reservas.mapper;

import co.edu.unicauca.backend.modules.inventario.dto.response.ProductoBebidaResponse;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.ComandaItem;
import co.edu.unicauca.backend.modules.reservas.dto.response.PreOrdenItemResponse;
import co.edu.unicauca.backend.shared.enums.EstacionComanda;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Mapper para convertir ítems de una {@link co.edu.unicauca.backend.modules.mesas_comandas.entity.Comanda}
 * en estado {@code PRE_RESERVA} en sus DTOs de respuesta.
 *
 * <p>Ítems de menú especial vienen en pares COCINA+BARRA que comparten el mismo
 * {@code comandaItemMenuGrupo}; este mapper los fusiona en un único
 * {@link PreOrdenItemResponse} con el campo {@code bebida} poblado.
 * Ítems de carta (sin grupo) se convierten individualmente.
 */
@Component
public class PreOrdenMapper {

    /**
     * Comparador para ordenar ítems de carta por categoría de producto.
     * Orden: PLATO (0) → BEBIDA (1) → OTRO (2)
     */
    public static final Comparator<ComandaItem> COMPARATOR_POR_CATEGORIA =
            Comparator.comparing(item -> item.getProducto().getProductoCategoria().ordinal());

    /**
     * Convierte una lista de {@link ComandaItem} de una pre-orden en DTOs de respuesta,
     * agrupando ítems COCINA+BARRA con el mismo {@code comandaItemMenuGrupo} en una sola entrada.
     *
     * <p>Los ítems sin grupo (carta) se ordenan por categoría antes de añadirse al resultado.
     *
     * @param items todos los ítems de las comandas PRE_RESERVA de la reserva
     * @return lista de {@link PreOrdenItemResponse} lista para serializar
     */
    public List<PreOrdenItemResponse> toDetalleResponse(List<ComandaItem> items) {
        // Agrupar los ítems de menú especial por su UUID de grupo
        Map<String, List<ComandaItem>> porGrupo = items.stream()
                .filter(i -> i.getComandaItemMenuGrupo() != null)
                .collect(Collectors.groupingBy(ComandaItem::getComandaItemMenuGrupo));

        List<PreOrdenItemResponse> result = new ArrayList<>();

        // Fusionar cada par COCINA+BARRA en un único response con bebida
        for (List<ComandaItem> grupo : porGrupo.values()) {
            ComandaItem plato = grupo.stream()
                    .filter(ci -> ci.getComanda().getComandaEstacion() == EstacionComanda.COCINA)
                    .findFirst()
                    .orElseThrow();
            ComandaItem bebidaItem = grupo.stream()
                    .filter(ci -> ci.getComanda().getComandaEstacion() == EstacionComanda.BARRA)
                    .findFirst()
                    .orElseThrow();
            result.add(toMenuItemResponse(plato, bebidaItem));
        }

        // Ítems de carta (sin grupo): un response por ítem, ordenados por categoría
        items.stream()
                .filter(i -> i.getComandaItemMenuGrupo() == null)
                .sorted(COMPARATOR_POR_CATEGORIA)
                .forEach(i -> result.add(toCartaItemResponse(i)));

        return result;
    }

    /**
     * Construye el DTO para un ítem de menú especial fusionando el plato (COCINA) y
     * la bebida (BARRA) del mismo grupo.
     *
     * @param plato     ítem del plato del menú (comanda COCINA)
     * @param bebidaItem ítem de la bebida del menú (comanda BARRA)
     * @return {@link PreOrdenItemResponse} con el campo {@code bebida} poblado
     */
    private PreOrdenItemResponse toMenuItemResponse(ComandaItem plato, ComandaItem bebidaItem) {
        List<PreOrdenItemResponse.OpcionModificacionSeleccionada> mods = plato.getModificaciones().stream()
                .map(m -> PreOrdenItemResponse.OpcionModificacionSeleccionada.builder()
                        .opcionId(m.getOpcion().getOpcionId())
                        .opcionNombre(m.getOpcion().getOpcionNombre())
                        .tipoComponente(m.getOpcion().getTipoComponente().name())
                        .build())
                .toList();
        return PreOrdenItemResponse.builder()
                .comandaItemId(plato.getComandaItemId())
                .productoId(plato.getProducto().getProductoId())
                .productoNombre(plato.getProducto().getProductoNombre())
                .cantidad(plato.getComandaItemCantidad())
                .categoriaProducto(plato.getProducto().getProductoCategoria().name())
                .precioUnitario(plato.getComandaItemPrecio())
                .descripcion(plato.getComandaItemDescripcion())
                .modificaciones(mods.isEmpty() ? null : mods)
                .bebida(new ProductoBebidaResponse(
                        bebidaItem.getProducto().getProductoId(),
                        bebidaItem.getProducto().getProductoNombre()))
                .build();
    }

    /**
     * Construye el DTO para un ítem de carta (sin grupo de menú especial).
     *
     * @param ci ítem de la comanda
     * @return {@link PreOrdenItemResponse} sin campo {@code bebida}
     */
    private PreOrdenItemResponse toCartaItemResponse(ComandaItem ci) {
        return PreOrdenItemResponse.builder()
                .comandaItemId(ci.getComandaItemId())
                .productoId(ci.getProducto().getProductoId())
                .productoNombre(ci.getProducto().getProductoNombre())
                .cantidad(ci.getComandaItemCantidad())
                .categoriaProducto(ci.getProducto().getProductoCategoria().name())
                .precioUnitario(ci.getComandaItemPrecio())
                .descripcion(ci.getComandaItemDescripcion())
                .build();
    }
}
