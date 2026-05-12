package co.edu.unicauca.backend.modules.mesas_comandas.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Payload para modificar la cantidad y/o la descripción de un ítem existente
 * del borrador. Cualquier campo {@code null} se interpreta como "no cambiar".
 */
@Getter @Setter
public class ModificarItemRequest {

    /** Nueva cantidad del ítem; rango {@code [1, 250]}. {@code null} no la altera. */
    @Min(value = 1, message = "La cantidad mínima es 1")
    @Max(value = 250, message = "La cantidad máxima por producto/bebida es de 250")
    private Integer cantidad;

    /** Nueva descripción del ítem; {@code null} no la altera. */
    @Size(max = 500, message = "La descripción no debe exceder 500 caracteres")
    private String descripcion;
}
