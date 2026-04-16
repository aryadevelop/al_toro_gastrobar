package co.edu.unicauca.backend.modules.produccion.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;


/**
 * Respuesta que representa una categoría de la carta del restaurante con sus productos.
 *
 * <p>Agrupa los productos activos bajo una misma categoría y expone el campo
 * {@code orden} para que el frontend pueda renderizar las categorías en el orden
 * definido por administración.
 *
 * <p>Reglas de uso:
 * <ul>
 *   <li>{@code productos} nunca es {@code null}; puede ser lista vacía si la categoría
 *       no tiene productos activos en carta.</li>
 * </ul>
 *
 * @see ProductoCartaResponse
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoriaCartaResponse {

    /** Identificador único de la categoría de carta. */
    private Integer categoriaId;

    /** Nombre visible de la categoría. */
    private String categoriaNombre;

    /** Posición de la categoría en la carta. */
    private Integer orden;

    /**
     * Lista de productos activos pertenecientes a esta categoría.
     * Nunca es {@code null}; puede ser vacía si no hay productos activos en la categoría.
     *
     * @see ProductoCartaResponse
     */
    private List<ProductoCartaResponse> productos;
}
