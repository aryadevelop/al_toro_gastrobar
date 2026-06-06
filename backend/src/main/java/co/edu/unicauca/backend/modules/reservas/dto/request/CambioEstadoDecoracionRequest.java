package co.edu.unicauca.backend.modules.reservas.dto.request;

import co.edu.unicauca.backend.shared.enums.EstadoGenerico;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request para cambiar el estado de una decoración.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CambioEstadoDecoracionRequest {

    @NotNull(message = "El estado es obligatorio")
    private EstadoGenerico estado;
}
