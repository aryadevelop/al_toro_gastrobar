package co.edu.unicauca.backend.modules.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Datos de entrada para el endpoint de renovación de access token.
 *
 * <p>El refresh token enviado debe corresponder a una sesión activa en la base de datos;
 * de lo contrario el servicio rechaza la solicitud con {@code 401 UNAUTHORIZED}.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenRequest {

    /** Refresh token emitido en el último login o renovación exitosa; máximo 1024 caracteres. */
    @NotBlank(message = "El refresh token es obligatorio")
    @Size(max = 1024, message = "El refresh token no debe exceder 1024 caracteres")
    private String refreshToken;
}