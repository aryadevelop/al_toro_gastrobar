package co.edu.unicauca.backend.modules.reservas.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * Resumen de un ítem de pre-orden incluido dentro de {@link ReservaResponse}.
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
public class PreOrdenItemResumenResponse {

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
