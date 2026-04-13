package co.edu.unicauca.backend.modules.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Datos de entrada para el login de usuarios.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    @NotBlank(message = "Por favor ingresa tu correo electronico")
    @Email(message = "Por favor ingresa un correo electrónico válido. Ej: juan@gmail.com")
    @Size(max = 150, message = "El correo no debe exceder 150 caracteres")
    private String email;

    @NotBlank(message = "Por favor, ingresa tu contraseña")
    @Size(max = 255, message = "La contraseña no debe exceder 255 caracteres")
    private String password;

    private Boolean forceSessionOverride;
}