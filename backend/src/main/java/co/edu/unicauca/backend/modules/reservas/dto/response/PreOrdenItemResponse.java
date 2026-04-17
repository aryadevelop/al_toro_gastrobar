package co.edu.unicauca.backend.modules.reservas.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

/**
 * Detalle de un ítem de la pre-orden de una reserva, incluyendo las modificaciones
 * de menú especial seleccionadas por el cliente.
 *
 * <p>Reglas de uso:
 * <ul>
 *   <li>{@code descripcion} es {@code null} cuando el cliente no añadió observaciones.</li>
 *   <li>{@code modificaciones} solo está poblado para ítems de menú especial;
 *       {@code null} para ítems sin opciones de personalización.</li>
 *   <li>{@code precioUnitario} refleja el precio del catálogo en el momento de registrar
 *       la pre-orden (snapshot); no varía si el catálogo cambia después.</li>
 * </ul>
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PreOrdenItemResponse {

    /** Identificador único del detalle de comanda en base de datos. */
    private Long comandaItemId;

    /** Identificador del producto asociado al ítem. */
    private Long productoId;

    /** Nombre del producto en el momento del registro. */
    private String productoNombre;

    /** Cantidad de unidades solicitadas del producto. */
    private Integer cantidad;

    /** Precio unitario capturado al registrar la pre-orden (snapshot). */
    private BigDecimal precioUnitario;

    /**
     * Observaciones libres del cliente sobre este ítem.
     * {@code null} si no se registraron observaciones.
     */
    private String descripcion;

    /**
     * Opciones de personalización de menú especial seleccionadas para este ítem.
     * {@code null} para ítems sin opciones de personalización.
     */
    private List<OpcionModificacionSeleccionada> modificaciones;

    /**
     * Opción de personalización de menú especial seleccionada por el cliente.
     */
    @Getter
    @Builder
    public static class OpcionModificacionSeleccionada {

        /** Identificador único de la opción de modificación. */
        private Long opcionId;

        /** Nombre de la opción de modificación. */
        private String opcionNombre;

        /** Tipo de componente del menú al que pertenece la opción. */
        private String tipoComponente;
    }
}
