package co.edu.unicauca.backend.modules.reservas.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * Resumen de un ítem de pre-orden incluido dentro de {@link ReservaResponse}.
 *
 * <p>Ejemplo de estructura JSON:
 * <pre>
 * {
 *   "productoId": 12,
 *   "productoNombre": "Bandeja Paisa",
 *   "cantidad": 2,
 *   "precioUnitario": 28000.00,
 *   "descripcion": "Bandeja Paisa - sin huevo"
 * }
 * </pre>
 *
 * <p>Reglas de uso:
 * <ul>
 *   <li>{@code descripcion} es {@code null} para ítems sin modificación libre;
 *       contiene el texto {@code "Nombre - modificación"} cuando el cliente especificó una.</li>
 * </ul>
 *
 * @see ReservaResponse
 * @see PreOrdenDetalleResponse
 */
@Getter
@Builder
public class PreOrdenItemResumen {

    /** Identificador del producto. */
    private Long productoId;

    /** Nombre del producto, por ejemplo {@code "Bandeja Paisa"}. */
    private String productoNombre;

    /** Cantidad de unidades solicitadas del producto. */
    private Integer cantidad;

    /** Precio unitario del producto. */
    private BigDecimal precioUnitario;

    /**
     * Texto de modificación libre del ítem, por ejemplo {@code "Bandeja Paisa - sin huevo"}.
     * {@code null} cuando el ítem no tiene modificación asociada.
     */
    private String descripcion;
}
