package co.edu.unicauca.backend.modules.auth.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Payload de entrada para el endpoint {@code POST /api/auth/register}.
 *
 * <p>Captura los datos necesarios para crear una nueva cuenta de cliente:
 * credenciales (correo, contraseña) y perfil básico (nombre, teléfono, dirección, fecha nacimiento).
 *
 * <p>Validaciones aplicadas:
 * <ul>
 *   <li>Email: obligatorio, formato válido, máximo 150 caracteres.</li>
 *   <li>Nombre: obligatorio, solo letras y espacios (tildes permitidas), máximo 100 caracteres.</li>
 *   <li>Teléfono: obligatorio, exactamente 10 dígitos, sin prefijo.</li>
 *   <li>Contraseña: obligatoria, mínimo 8 caracteres.</li>
 *   <li>Confirmación de contraseña: obligatoria, debe coincidir con contraseña.</li>
 *   <li>Aceptación de términos: obligatoria, debe ser {@code true}.</li>
 *   <li>Fecha de nacimiento: opcional.</li>
 * </ul>
 *
 * <p>Las validaciones de longitud máxima de nombre se aplican con {@code @Size};
 * la coincidencia de contraseñas se valida a nivel de servicio mediante método getter.
 *
 * @see co.edu.unicauca.backend.modules.auth.service.AuthService#register(RegisterRequest)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    /**
     * Correo electrónico del nuevo cliente; se normaliza a minúsculas.
     * Obligatorio; máximo 150 caracteres; debe ser único en el sistema.
     */
    @NotBlank(message = "Por favor ingresa tu correo electrónico")
    @Email(message = "Ingresa un correo electrónico válido (ej. nombre@dominio.com)")
    @Size(max = 150, message = "El correo no debe exceder 150 caracteres")
    private String email;

    /**
     * Nombre completo del cliente. Obligatorio; solo letras y espacios (tildes/ñ permitidas);
     * máximo 100 caracteres. Únicamente debe validarse contra el patrón permitido.
     */
    @NotBlank(message = "Este campo no puede estar vacío")
    @Size(max = 100, message = "El nombre no puede exceder los 100 caracteres")
    @Pattern(
        regexp = "^[a-záéíóúñA-ZÁÉÍÓÚÑ\\s]+$",
        message = "El nombre solo puede contener letras y espacios"
    )
    private String nombre;

    /**
     * Número de contacto en formato colombiano (10 dígitos sin prefijo).
     * Obligatorio; exactamente 10 dígitos numéricos.
     */
    @NotBlank(message = "Este campo no puede estar vacío")
    @Pattern(
        regexp = "^[0-9]{10}$",
        message = "Ingresa un número de teléfono válido de 10 dígitos"
    )
    private String telefono;

    /**
     * Contraseña en texto plano; se cifra con BCrypt antes de almacenarse.
     * Obligatoria; mínimo 8 caracteres.
     */
    @NotBlank(message = "Por favor, ingresa tu contraseña")
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    private String password;

    /**
     * Confirmación de la contraseña en texto plano.
     * Obligatoria; debe coincidir exactamente con {@code password}.
     * La validación de coincidencia se realiza en la capa de servicio.
     */
    @NotBlank(message = "Por favor, confirma tu contraseña")
    private String passwordConfirmation;

    /**
     * Flag de aceptación de términos y condiciones.
     * Obligatorio; debe ser {@code true} para que el registro sea válido.
     */
    @NotNull(message = "Debes aceptar los términos y condiciones para registrarte")
    @AssertTrue(message = "Debes aceptar los términos y condiciones para registrarte")
    private Boolean aceptaTerminos;

    /**
     * Fecha de nacimiento del cliente (formato ISO 8601: YYYY-MM-DD).
     * Opcional; si se proporciona, se almacena en el perfil del cliente.
     */
    private String fechaNacimiento;
}
