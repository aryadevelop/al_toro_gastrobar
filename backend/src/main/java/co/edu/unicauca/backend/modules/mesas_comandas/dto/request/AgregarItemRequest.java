package co.edu.unicauca.backend.modules.mesas_comandas.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Payload para agregar un ítem al borrador de comanda. La estación destino
 * (cocina o barra) la deduce el servicio a partir de la categoría del producto.
 */
@Getter @Setter
public class AgregarItemRequest {

    /** Identificador de la visita dueña del borrador. */
    @NotNull(message = "visitaId es obligatorio")
    private Long visitaId;

    /** Producto a agregar; no se aceptan productos marcados como menú especial. */
    @NotNull(message = "productoId es obligatorio")
    private Long productoId;

    /** Cantidad solicitada; rango {@code [1, 250]}. */
    @NotNull(message = "cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad mínima es 1")
    @Max(value = 250, message = "La cantidad máxima por producto/bebida es de 250")
    private Integer cantidad;

    /**
     * Texto de modificación libre. 
     * {@code null} indica un ítem base sin modificación;
     * Un valor presente persiste un ítem modificado con el mismo {@code productoId} 
     * y descripción propia. El mismo producto puede aparecer
     * en la comanda múltiples veces con distintas descripciones.
     */
    @Size(max = 500, message = "La descripción no debe exceder 500 caracteres")
    private String descripcion;
}
