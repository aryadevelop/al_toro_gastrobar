package co.edu.unicauca.backend.modules.mesas_comandas.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * DTO de respuesta para un ítem de comanda dentro del detalle de visita.
 *
 * <p>Items agrupados por (nombreProducto + descripcion) de todas las comandas.
 * La cantidad representa la suma de items idénticos a través de todas las comandas.
 */
@Getter
@Builder
public class ComandaItemResponse {

    /** Nombre del producto consumido. */
    private final String nombreProducto;

    /** Descripción o modificaciones del ítem; {@code null} si no aplica. */
    private final String descripcion;

    /** Cantidad de unidades del producto. */
    private final Integer cantidad;

    /** Precio unitario en el momento del pedido. */
    private final BigDecimal precioUnitario;

    /** Subtotal del ítem ({@code precioUnitario × cantidad}). */
    private final BigDecimal subtotal;
}
