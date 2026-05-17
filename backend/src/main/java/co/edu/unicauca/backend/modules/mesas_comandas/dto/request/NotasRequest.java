package co.edu.unicauca.backend.modules.mesas_comandas.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Payload para actualizar las notas de una comanda BORRADOR. Persistencia
 * incremental: el frontend invoca cada vez que el mesero deja de escribir.
 */
@Getter @Setter
public class NotasRequest {
    /** Texto libre; {@code null} elimina las notas existentes. */
    @Size(max = 500, message = "Las notas no deben exceder 500 caracteres")
    private String notas;
}
