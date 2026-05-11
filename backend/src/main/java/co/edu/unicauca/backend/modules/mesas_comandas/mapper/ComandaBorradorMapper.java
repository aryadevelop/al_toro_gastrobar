package co.edu.unicauca.backend.modules.mesas_comandas.mapper;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Component;

import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.BorradorComandaResponse;
import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.ItemBebidaMenuResponse;
import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.ItemBorradorResponse;
import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.OpcionMenuSeleccionadaResponse;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Comanda;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.ComandaItem;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.ComandaMenuModificacion;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Mesa;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ComandaItemRepository;
import co.edu.unicauca.backend.shared.enums.EstacionComanda;
import lombok.RequiredArgsConstructor;

/**
 * Mapper de la vista del formulario de modificar comanda.
 *
 * Para ítems de menú especial, fusionar el par COCINA+BARRA del mismo
 * {@code comandaItemMenuGrupo} en un único {@link ItemBorradorResponse}
 * con la bebida embebida.</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class ComandaBorradorMapper {

    /** Carga los ítems de la comanda contraparte para localizar la bebida del menú. */
    private final ComandaItemRepository comandaItemRepository;

    /** Orden alfabético case-insensitive por nombre de producto. */
    private static final Comparator<ComandaItem> POR_NOMBRE =
            Comparator.comparing(it -> it.getProducto().getProductoNombre(),
                                 String.CASE_INSENSITIVE_ORDER);

    /**
     * Construye la respuesta completa de {@code GET /api/comandas/borrador}.
     *
     * @param mesa             mesa dueña del borrador
     * @param comandasBorrador comandas BORRADOR de la visita
     * @param totalAcumulado   suma de {@code precio * cantidad} de todos los ítems
     *                         de la visita en estados distintos de COMPLETADO,
     *                         calculada por el servicio
     * @return DTO listo para el frontend; nunca {@code null}
     */
    public BorradorComandaResponse toBorradorResponse(Mesa mesa,
                                                     List<Comanda> comandasBorrador,
                                                     BigDecimal totalAcumulado) {

        Comanda cocina = comandasBorrador.stream()
                .filter(c -> c.getComandaEstacion() == EstacionComanda.COCINA)
                .findFirst().orElse(null);
        Comanda barra = comandasBorrador.stream()
                .filter(c -> c.getComandaEstacion() == EstacionComanda.BARRA)
                .findFirst().orElse(null);

        List<ComandaItem> itemsCocina = cargarItems(cocina);
        List<ComandaItem> itemsBarra  = cargarItems(barra);

        List<ItemBorradorResponse> platos  = mapearEstacion(itemsCocina, itemsBarra);
        List<ItemBorradorResponse> bebidas = mapearEstacion(itemsBarra,  itemsCocina);

        BigDecimal total = totalAcumulado != null ? totalAcumulado : BigDecimal.ZERO;
        BigDecimal subTotal = Stream.concat(itemsCocina.stream(), itemsBarra.stream())
                .filter(ci -> ci.getComandaItemPrecio() != null)
                .map(ci -> ci.getComandaItemPrecio().multiply(BigDecimal.valueOf(ci.getComandaItemCantidad())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return BorradorComandaResponse.builder()
                .visitaId(mesa.getVisitaId())
                .mesaIdentificador(mesa.getMesaIdentificador())
                .comandaCocinaId(cocina != null ? cocina.getComandaId() : null)
                .comandaBarraId(barra  != null ? barra.getComandaId()  : null)
                .platos(platos)
                .bebidas(bebidas)
                .subTotal(subTotal)
                .total(total)
                .notasCocina(cocina != null ? cocina.getComandaNotas() : null)
                .notasBarra(barra   != null ? barra.getComandaNotas()  : null)
                .puedeEnviarCocina(!platos.isEmpty())
                .puedeEnviarBarra(!bebidas.isEmpty())
                .build();
    }

    /**
     * Convierte una lista plana de {@link ComandaItem} a una lista de
     * {@link ItemBorradorResponse} sin fusión de menú ni anidado de modificaciones.
     * Lo usa el publisher WebSocket {@code /topic/estacion/{estacion}} al enviar
     * a producción.
     *
     * @param items ítems de la comanda enviada
     * @return lista de respuestas ordenada por nombre de producto
     */
    public List<ItemBorradorResponse> toItemsResponse(List<ComandaItem> items) {
        if (items == null || items.isEmpty()) return List.of();
        return items.stream()
                .sorted(POR_NOMBRE)
                .map(it -> mapearItem(it, BigDecimal.ZERO, null))
                .collect(Collectors.toList());
    }

    /**
     * Mapea los ítems de UNA estación
     *
     * @param itemsFuente      ítems de la estación que se quiere mostrar
     * @param itemsContraparte ítems de la otra estación (para localizar la bebida del menú)
     * @return ítems base ordenados alfabéticamente, con modificaciones anidadas
     */
    private List<ItemBorradorResponse> mapearEstacion(List<ComandaItem> itemsFuente,
                                                      List<ComandaItem> itemsContraparte) {
        if (itemsFuente == null || itemsFuente.isEmpty()) return List.of();
        return itemsFuente.stream()
                .sorted(POR_NOMBRE)
                .map(it -> mapearItem(it, calcularSubtotal(it), buscarBebidaDelMenu(itemsContraparte, it)))
                .collect(Collectors.toList());
    }

    /**
     * Construye el DTO de un {@link ComandaItem} dado.
     *
     * @param item            entidad fuente
     * @param subtotal        {@code precioUnitario * cantidad}; cero si se desconoce
     * @param modificaciones  hijos modificados ya mapeados (vacío para ítems hoja)
     * @param bebidaMenu      bebida fusionada del par menú especial; {@code null} si no aplica
     * @return DTO inmutable listo para el frontend
     */
    private ItemBorradorResponse mapearItem(ComandaItem item,
                                            BigDecimal subtotal,
                                            ItemBebidaMenuResponse bebidaMenu) {
        return ItemBorradorResponse.builder()
                .comandaItemId(item.getComandaItemId())
                .productoId(item.getProducto().getProductoId())
                .productoNombre(item.getProducto().getProductoNombre())
                .categoriaProducto(item.getProducto().getProductoCategoria().name())
                .precioUnitario(item.getComandaItemPrecio())
                .cantidad(item.getComandaItemCantidad())
                .subtotal(subtotal)
                .descripcion(item.getComandaItemDescripcion())
                .menuGrupo(item.getComandaItemMenuGrupo())
                .modificacionesMenu(mapearModificacionesMenu(item.getModificaciones()))
                .bebida(bebidaMenu)
                .stockActual(item.getProducto().getStockActual())
                .build();
    }

    /**
     * Carga los ítems de una comanda desde el repositorio. Devuelve lista vacía
     * cuando la comanda es {@code null} (no existe BORRADOR para esa estación).
     */
    private List<ComandaItem> cargarItems(Comanda comanda) {
        if (comanda == null) return List.of();
        return comandaItemRepository
                .findByComanda_ComandaIdOrderByProductoNombreAsc(comanda.getComandaId());
    }

    /**
     * Localiza la bebida del menú especial cuyo {@code comandaItemMenuGrupo} coincide
     * con el del ítem dado, dentro de los ítems de la otra estación.
     *
     * @param itemsContraparte ítems de la otra estación
     * @param itemMenu         ítem cocina/barra del menú; cualquier otro ítem retorna {@code null}
     * @return DTO de la bebida; {@code null} si el ítem no es de menú o no hay contraparte
     */
    private ItemBebidaMenuResponse buscarBebidaDelMenu(List<ComandaItem> itemsContraparte,
                                                       ComandaItem itemMenu) {
        String grupo = itemMenu.getComandaItemMenuGrupo();
        if (grupo == null || itemsContraparte == null || itemsContraparte.isEmpty()) return null;
        return itemsContraparte.stream()
                .filter(ci -> grupo.equals(ci.getComandaItemMenuGrupo()))
                .findFirst()
                .map(ci -> ItemBebidaMenuResponse.builder()
                        .productoId(ci.getProducto().getProductoId())
                        .productoNombre(ci.getProducto().getProductoNombre())
                        .build())
                .orElse(null);
    }

    /**
     * Mapea las opciones de modificación de menú especial seleccionadas para un
     * ítem dado.
     *
     * @param origen lista persistida en {@code comanda_menu_modificacion}
     * @return lista de DTOs; vacía si {@code origen} es null o vacía
     */
    private List<OpcionMenuSeleccionadaResponse> mapearModificacionesMenu(
            List<ComandaMenuModificacion> origen) {
        if (origen == null || origen.isEmpty()) return List.of();
        return origen.stream()
                .filter(Objects::nonNull)
                .map(m -> OpcionMenuSeleccionadaResponse.builder()
                        .opcionId(m.getOpcion().getOpcionId())
                        .opcionNombre(m.getOpcion().getOpcionNombre())
                        .tipoComponente(m.getOpcion().getTipoComponente().name())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Calcula {@code precio * cantidad} para un ítem; devuelve {@link BigDecimal#ZERO}
     * si el precio es {@code null} (ítem de menú con precio cero).
     */
    private BigDecimal calcularSubtotal(ComandaItem item) {
        if (item.getComandaItemPrecio() == null) return BigDecimal.ZERO;
        return item.getComandaItemPrecio()
                .multiply(BigDecimal.valueOf(item.getComandaItemCantidad()));
    }
}
