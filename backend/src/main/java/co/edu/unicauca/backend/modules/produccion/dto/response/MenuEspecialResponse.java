package co.edu.unicauca.backend.modules.produccion.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;


/**
 * Respuesta que representa un menú especial del restaurante con sus grupos de modificación.
 *
 * <p>Un menú especial es un {@link co.edu.unicauca.backend.modules.produccion.entity.Producto}
 * cuyo campo {@code menuEspecial} es {@code true}. Está disponible para reservas de grupos
 * de más de 10 personas y permite al cliente personalizar cada componente del plato.
 *
 * <p>Reglas de uso:
 * <ul>
 *   <li>{@code productoDescripcion} puede ser {@code null} si el producto no tiene
 *       descripción registrada.</li>
 *   <li>{@code modificacionesPorComponente} nunca es {@code null}; puede ser lista vacía
 *       si el menú especial no tiene opciones de modificación activas.</li>
 * </ul>
 *
 * @see GrupoModificacionResponse
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MenuEspecialResponse {

    /** Identificador único del menú especial. */
    private Long productoId;

    /** Nombre del menú especial tal como aparece en la carta. */
    private String productoNombre;

    /** Descripción del menú especial; {@code null} si no está registrada. */
    private String productoDescripcion;

    /** Precio base del menú especial. */
    private BigDecimal productoPrecio;

    /**
     * Lista de grupos de modificación disponibles, agrupados por tipo de componente.
     * Nunca es {@code null}; puede ser vacía si no hay opciones de modificación activas.
     *
     * @see GrupoModificacionResponse
     */
    private List<GrupoModificacionResponse> modificacionesPorComponente;

    /**
     * Bebidas disponibles para seleccionar junto con este menú especial.
     * Nunca es {@code null}; puede ser vacía si no hay bebidas asociadas.
     */
    private List<ProductoBebidaResponse> bebidasDisponibles;
}
