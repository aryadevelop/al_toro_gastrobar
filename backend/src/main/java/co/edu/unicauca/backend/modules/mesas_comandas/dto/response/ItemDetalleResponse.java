package co.edu.unicauca.backend.modules.mesas_comandas.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Línea de ítem incluida en el detalle de una comanda de producción.
 * Constituye un fragmento de {@link ComandaProduccionDetalleResponse}, donde
 * los ítems se agrupan por categoría de producto.
 */
@Getter
@Builder
public class ItemDetalleResponse {

    /** Identificador del ítem de comanda. */
    private final Long comandaItemId;

    /** Identificador del producto del catálogo. */
    private final Long productoId;

    /** Nombre del producto vigente al momento del pedido. */
    private final String productoNombre;

    /** Categoría del producto */
    private final String categoriaProducto;

    /** Cantidad solicitada del ítem. */
    private final Integer cantidad;

    /** Descripción adicional registrada para el ítem 
     * Vale {@code null} cuando el ítem no tiene descripción asociada.
     */
    private final String descripcion;

    /**
     * Descripciones de las opciones de menú especial seleccionadas para el
     * ítem. Lista vacía cuando el ítem corresponde a un producto de carta.
     */
    private final List<String> modificacionesMenu;
}
