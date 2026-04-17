package co.edu.unicauca.backend.modules.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Payload de entrada para el endpoint {@code POST /api/auth/login}.
 *
 * <p>Contiene las credenciales del usuario y un flag opcional para gestionar
 * sesiones concurrentes: si el usuario ya tiene una sesión activa en otro
 * dispositivo y desea cerrarla para continuar, debe enviar
 * {@code forceSessionOverride: true}.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    /**
     * Correo electrónico del usuario; se normaliza a minúsculas antes de validar.
     * Obligatorio; máximo 150 caracteres.
     */
    @NotBlank(message = "Por favor ingresa tu correo electronico")
    @Email(message = "Por favor ingresa un correo electrónico válido. Ej: juan@gmail.com")
    @Size(max = 150, message = "El correo no debe exceder 150 caracteres")
    private String email;

    /**
     * Contraseña en texto plano; se compara contra el hash BCrypt almacenado.
     * Obligatoria; máximo 255 caracteres.
     */
    @NotBlank(message = "Por favor, ingresa tu contraseña")
    @Size(max = 255, message = "La contraseña no debe exceder 255 caracteres")
    private String password;

    /**
     * Si es {@code true}, cierra la sesión activa previa y crea una nueva.
     * Si es {@code null} o {@code false} y ya existe una sesión activa, el servidor
     * responde {@code 409 Conflict} solicitando confirmación al usuario.
     */
    private Boolean forceSessionOverride;
}