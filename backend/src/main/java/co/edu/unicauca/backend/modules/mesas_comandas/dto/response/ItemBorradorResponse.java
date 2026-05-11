package co.edu.unicauca.backend.modules.mesas_comandas.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

/**
 * Ítem del formulario de modificar comanda. 
 * 
 * <p>Cuando el ítem proviene de un menú especial se fusiona el par COCINA+BARRA
 * del mismo {@code menuGrupo} en una sola fila: {@code bebida} contiene la
 * bebida del menú y {@code modificacionesMenu} las opciones seleccionadas.
 */
@Getter @Builder
public class ItemBorradorResponse {
    /** Identificador del {@code comanda_item} en BD; clave para PATCH/DELETE. */
    private final Long comandaItemId;

    /** Identificador del producto del catálogo. */
    private final Long productoId;

    /** Nombre del producto al momento de cargar el borrador. */
    private final String productoNombre;

    /** Categoría del producto: {@code "PLATO"} o {@code "BEBIDA"}. */
    private final String categoriaProducto;

    /** Precio unitario congelado en el ítem; puede diferir del catálogo si hubo cambio posterior. */
    private final BigDecimal precioUnitario;

    /** Cantidad solicitada. */
    private final Integer cantidad;

    /** Resultado de {@code precioUnitario * cantidad}. */
    private final BigDecimal subtotal;

    /** Texto de modificación libre; {@code null} indica ítem base sin modificación. */
    private final String descripcion;

    /** UUID del par COCINA+BARRA del mismo menú especial; {@code null} para ítems de carta. */
    private final String menuGrupo;

    /** Opciones del menú especial seleccionadas; vacía para ítems de carta. */
    private final List<OpcionMenuSeleccionadaResponse> modificacionesMenu;

    /** Bebida fusionada del menú especial; {@code null} si el ítem es de carta. */
    private final ItemBebidaMenuResponse bebida;

    /** Stock disponible del producto en catálogo; {@code null} si no se gestiona stock. */
    private final BigDecimal stockActual;
}
