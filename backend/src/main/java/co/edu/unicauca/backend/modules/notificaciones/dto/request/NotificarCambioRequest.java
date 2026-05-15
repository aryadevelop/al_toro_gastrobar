package co.edu.unicauca.backend.modules.notificaciones.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Cuerpo del request para que el personal de producción registre una
 * notificación de cambio sobre una comanda pendiente.
 */
@Getter
@NoArgsConstructor
public class NotificarCambioRequest {

    /** Identificador de la comanda sobre la que se solicita el cambio. */
    @NotNull
    private Long comandaId;
}
