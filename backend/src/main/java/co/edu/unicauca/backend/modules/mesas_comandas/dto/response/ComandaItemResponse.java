package co.edu.unicauca.backend.modules.mesas_comandas.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * DTO de respuesta para un ítem de comanda dentro del detalle de visita.
 */
@Getter
@Builder
public class ComandaItemResponse {

    /** Nombre del producto consumido. */
    private final String nombreProducto;

    /** Cantidad de unidades del producto. */
    private final Integer cantidad;

    /** Precio unitario en el momento del pedido. */
    private final BigDecimal precioUnitario;

    /** Subtotal del ítem ({@code precioUnitario × cantidad}). */
    private final BigDecimal subtotal;
}
