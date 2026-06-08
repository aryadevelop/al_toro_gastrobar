package co.edu.unicauca.backend.modules.pagos_caja.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * Línea de ítem de la cuenta preliminar del cajero.
 *
 * <p>A diferencia de la vista del cliente, expone {@code comandaItemId} para permitir
 * el ajuste individual, y la metadata del menú especial ({@code menuGrupo},
 * {@code esMenuEspecial}) para que el frontend vincule el par plato+bebida.
 */
@Getter
@Builder
public class CuentaItemResponse {

    /** Identificador del ítem de comanda; clave del ajuste de cantidades/precio. */
    private final Long comandaItemId;

    /** Nombre del producto. */
    private final String nombreProducto;

    /** Categoría: "PLATO", "BEBIDA", "OTRO". */
    private final String categoriaProducto;

    /** Cantidad de unidades del ítem. */
    private final Integer cantidad;

    /** Precio unitario capturado al pedido (la bebida de un menú especial es 0). */
    private final BigDecimal precioUnitario;

    /** Subtotal del ítem ({@code precioUnitario × cantidad}). */
    private final BigDecimal subtotal;

    /** Descripción/modificaciones libres; {@code null} si es ítem base. */
    private final String descripcion;

    /** {@code true} si el ítem tiene descripción y, por tanto, su precio es editable. */
    private final boolean esModificado;

    /** UUID que vincula el par COCINA+BARRA de un menú especial; {@code null} si es de carta. */
    private final String menuGrupo;

    /** {@code true} si el producto es un menú especial. */
    private final boolean esMenuEspecial;
}
