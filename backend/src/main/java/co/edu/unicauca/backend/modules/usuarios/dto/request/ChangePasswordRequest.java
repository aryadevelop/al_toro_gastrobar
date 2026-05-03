package co.edu.unicauca.backend.modules.usuarios.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Payload de entrada para el endpoint {@code POST /api/clientes/me/cambiar-contraseña}.
 *
 * <p>Captura los datos necesarios para cambiar la contraseña del usuario autenticado.
 *
 * <p>Validaciones aplicadas:
 * <ul>
 *   <li>Contraseña actual: obligatoria, se valida contra el hash almacenado.</li>
 *   <li>Nueva contraseña: obligatoria, mínimo 8 caracteres, debe incluir
 *       mayúscula, minúscula, número y carácter especial.</li>
 *   <li>Confirmación: obligatoria, debe coincidir con nueva contraseña.</li>
 * </ul>
 *
 * @see co.edu.unicauca.backend.modules.usuarios.service.ClienteProfileService#cambiarContraseña(Long, ChangePasswordRequest)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChangePasswordRequest {

    /**
     * Contraseña actual en texto plano. Obligatoria; se valida contra el hash
     * almacenado en la base de datos.
     */
    @NotBlank(message = "La contraseña actual es obligatoria")
    private String contraseñaActual;

    /**
     * Nueva contraseña en texto plano. Obligatoria; mínimo 8 caracteres;
     * debe incluir: 1 mayúscula, 1 minúscula, 1 número, 1 carácter especial.
     * Se cifra con BCrypt antes de almacenarse.
     */
    @NotBlank(message = "La nueva contraseña es obligatoria")
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres, incluir mayuscula, minuscula y caracter especial")
    private String nuevaContraseña;

    /**
     * Confirmación de la nueva contraseña en texto plano.
     * Obligatoria; debe coincidir exactamente con {@code nuevaContraseña}.
     * La validación de coincidencia se realiza en la capa de servicio.
     */
    @NotBlank(message = "La confirmación de la contraseña es obligatoria")
    private String confirmacion;
}
