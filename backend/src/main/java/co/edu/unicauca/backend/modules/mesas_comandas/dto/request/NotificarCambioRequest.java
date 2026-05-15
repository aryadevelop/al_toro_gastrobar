package co.edu.unicauca.backend.modules.mesas_comandas.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Cuerpo de la solicitud para que el personal de producción registre una
 * notificación de cambio sobre una comanda pendiente.
 */
@Getter
@Setter
@NoArgsConstructor
public class NotificarCambioRequest {

    /** Identificador de la comanda en estado {@code PENDIENTE} sobre la que se registra el cambio. */
    @NotNull(message = "El identificador de la comanda es obligatorio")
    private Long comandaId;
}
