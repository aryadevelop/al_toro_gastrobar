package co.edu.unicauca.backend.modules.inventario.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * Resultado de búsqueda de productos para el formulario de modificar comanda.
 */
@Getter @Builder
public class ProductoBusquedaResponse {
    /** Identificador del producto. */
    private final Long productoId;

    /** Nombre del producto. */
    private final String productoNombre;

    /** Precio unitario vigente en el catálogo. */
    private final BigDecimal productoPrecio;

    /** Categoría del producto: {@code "PLATO"} o {@code "BEBIDA"}. */
    private final String productoCategoria;
}
