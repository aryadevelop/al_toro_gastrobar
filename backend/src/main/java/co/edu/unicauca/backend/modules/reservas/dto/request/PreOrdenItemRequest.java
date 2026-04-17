package co.edu.unicauca.backend.modules.reservas.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Representa un ítem de la pre-orden enviado por el cliente al crear una reserva.
 *
 * <p>Reglas de uso:
 * <ul>
 *   <li>{@code productoId} es siempre obligatorio.</li>
 *   <li>{@code descripcion} es {@code null} para ítems normales; contiene el texto
 *       {@code "Nombre - modificación"} cuando el cliente especifica una modificación libre.</li>
 *   <li>{@code esMenuEspecial} es {@code true} únicamente cuando el ítem corresponde
 *       a un menú especial.</li>
 *   <li>{@code opcionesModificacion} solo aplica cuando {@code esMenuEspecial = true};
 *       contiene los IDs de {@code OpcionModificacion} seleccionados.</li>
 * </ul>
 *
 * @see CrearReservaRequest
 */
@Getter
@Setter
@NoArgsConstructor
public class PreOrdenItemRequest {

    /**
     * Identificador del producto seleccionado por el cliente.
     * Obligatorio en todo ítem de pre-orden.
     */
    @NotNull(message = "El producto es obligatorio")
    private Long productoId;

    /**
     * Cantidad de unidades del producto solicitadas.
     * Rango permitido: {@code 1} – {@code 250}; obligatorio.
     */
    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad mínima es 1")
    @Max(value = 250, message = "La cantidad máxima por producto es 250")
    private Integer cantidad;

    /**
     * Texto de modificación libre embebido en el ítem, por ejemplo {@code "Hambuerguesa torito - sin huevo"}.
     * {@code null} cuando el ítem no tiene modificación libre asociada.
     */
    private String descripcion;

    /** {@code true} cuando el ítem corresponde a un menú especial; {@code false} o {@code null} en caso contrario. */
    private Boolean esMenuEspecial;

    /**
     * IDs de {@code OpcionModificacion} seleccionados mediante checkboxes (CA-07).
     * Solo aplica cuando {@code esMenuEspecial = true}; {@code null} o lista vacía si no hay opciones.
     */
    private List<Long> opcionesModificacion;
}
