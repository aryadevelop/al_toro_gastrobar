package co.edu.unicauca.backend.modules.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Respuesta devuelta por el endpoint {@code POST /api/auth/register}.
 *
 * <p>Comunica el éxito del registro al cliente e incluye datos mínimos del usuario
 * creado para que el frontend configure la sesión o perfil en el dashboard.
 * El cliente debe navegar a la página de login con el correo registrado.
 *
 * @see co.edu.unicauca.backend.modules.auth.controller.AuthController#register(RegisterRequest)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterResponse {

    /** Indica si el registro fue exitoso; siempre {@code true} en esta respuesta. */
    private Boolean success;

    /** Mensaje amistoso confirmando el registro exitoso. */
    private String message;

    /** Datos mínimos del usuario registrado para configuración inicial del frontend. */
    private UserRegistrationData user;

    /**
     * Subclase con los datos básicos del usuario recién registrado.
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserRegistrationData {

        /** Identificador único del usuario (ID de la tabla usuario). */
        private String id;

        /** Nombre completo del cliente. */
        private String nombre;

        /** Correo electrónico del usuario (identificador de sesión). */
        private String email;

        /** Teléfono de contacto del cliente. */
        private String telefono;

        /** Rol asignado; para nuevos registros siempre es {@code "CLIENTE"}. */
        private String role;
    }
}
