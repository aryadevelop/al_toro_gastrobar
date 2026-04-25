package co.edu.unicauca.backend.modules.mesas_comandas.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * Ítem de la visita activa tal como se muestra al cliente en el dashboard.
 * El estado ("En preparación" / "Servido") se deriva del estado de la
 * {@link co.edu.unicauca.backend.modules.mesas_comandas.entity.Comanda} padre.
 */
@Getter
@Builder
public class ItemVisitaResponse {

    /** Identificador del ítem de comanda. */
    private final Long comandaItemId;

    /** Nombre del producto. */
    private final String nombreProducto;

    /** Unidades pedidas. */
    private final Integer cantidad;

    /**
     * Estado visible para el cliente: {@code "En preparación"} o {@code "Servido"}.
     * Derivado de {@code EstadoComanda}: PENDIENTE/EN_PREPARACION → "En preparación";
     * LISTO/COMPLETADO → "Servido".
     */
    private final String estadoItem;

    /** Precio capturado al momento del pedido; no varía si el catálogo cambia. */
    private final BigDecimal precioUnitario;

    /** {@code precioUnitario × cantidad}. */
    private final BigDecimal subtotal;
}
