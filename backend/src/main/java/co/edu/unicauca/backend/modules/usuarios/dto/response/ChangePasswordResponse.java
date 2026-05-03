package co.edu.unicauca.backend.modules.usuarios.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Respuesta devuelta por el endpoint {@code POST /api/clientes/me/cambiar-contraseña}.
 *
 * <p>Comunica el éxito del cambio de contraseña al cliente.
 *
 * @see co.edu.unicauca.backend.modules.usuarios.controller.ClienteController#cambiarContraseña(ChangePasswordRequest)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChangePasswordResponse {

    /** Indica si el cambio de contraseña fue exitoso; siempre {@code true} en esta respuesta. */
    private Boolean success;

    /** Mensaje amistoso confirmando que la contraseña fue actualizada exitosamente. */
    private String message;
}
