package co.edu.unicauca.backend.modules.inventario.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;


/**
 * Respuesta que representa un producto de la carta del restaurante.
 *
 * <p>Reglas de uso:
 * <ul>
 *   <li>{@code productoDescripcion} puede ser {@code null} si el producto no tiene
 *       descripción registrada.</li>
 *   <li>{@code productoCategoria} corresponde al nombre del enumerado
 *       {@link co.edu.unicauca.backend.shared.enums.CategoriaProducto} .</li>
 * </ul>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductoCartaResponse {

    /** Identificador único del producto. */
    private Long productoId;

    /** Nombre del producto tal como aparece en la carta. */
    private String productoNombre;

    /** Descripción del producto; {@code null} si no está registrada. */
    private String productoDescripcion;

    /** Precio unitario del producto. */
    private BigDecimal productoPrecio;

    /**
     * Categoría del producto como cadena (valor del enumerado
     * {@link co.edu.unicauca.backend.shared.enums.CategoriaProducto}).
     */
    private String productoCategoria;
}
