package co.edu.unicauca.backend.modules.inventario.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO de producto para el listado de inventario.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductoInventarioResponse {

    /** Identificador del producto. */
    private Long productoId;

    /** Nombre del producto. */
    private String productoNombre;

    /** Nombre de la categoría de carta asociada al producto. */
    private String categoriaNombre;

    /** Precio de venta actual. */
    private BigDecimal productoPrecio;

    /** Stock actual del producto; {@code null} indica ausencia de stock registrado. */
    private BigDecimal stockActual;

    /** Estado del producto, por ejemplo {@code ACTIVO} o {@code INACTIVO}. */
    private String productoEstado;
}
