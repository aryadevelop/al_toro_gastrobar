package co.edu.unicauca.backend.modules.usuarios.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Payload de entrada para el endpoint {@code PUT /api/clientes/me}.
 *
 * <p>Captura los datos actualizables del perfil del cliente: nombre, correo,
 * teléfono, dirección y aceptación de términos.
 *
 * <p>Validaciones aplicadas:
 * <ul>
 *   <li>Nombre: obligatorio, solo letras y espacios, máximo 100 caracteres.</li>
 *   <li>Email: obligatorio, formato válido, máximo 150 caracteres, no duplicado.</li>
 *   <li>Teléfono: obligatorio, exactamente 10 dígitos, no duplicado.</li>
 *   <li>Dirección: opcional, máximo 255 caracteres.</li>
 *   <li>Acepta términos: obligatorio, debe ser {@code true}.</li>
 * </ul>
 *
 * @see co.edu.unicauca.backend.modules.usuarios.service.ClienteProfileService#actualizarPerfil(Long, UpdateClienteRequest)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateClienteRequest {

    /**
     * Nombre completo del cliente. Obligatorio; solo letras y espacios
     * (tildes/ñ permitidas); máximo 100 caracteres.
     */
    @NotBlank(message = "Este campo es obligatorio")
    @Size(max = 100, message = "El nombre no puede exceder los 100 caracteres")
    @Pattern(
        regexp = "^[a-záéíóúñA-ZÁÉÍÓÚÑ\\s]+$",
        message = "El nombre solo puede contener letras y espacios"
    )
    private String nombre;

    /**
     * Correo electrónico del cliente; se normaliza a minúsculas.
     * Obligatorio; máximo 150 caracteres; debe ser único en el sistema
     * (excepto el del cliente actual).
     */
    @NotBlank(message = "Este campo es obligatorio")
    @Email(message = "Ingresa un correo electrónico válido (ej. nombre@dominio.com)")
    @Size(max = 150, message = "El correo no debe exceder 150 caracteres")
    private String email;

    /**
     * Número de contacto en formato colombiano (10 dígitos sin prefijo).
     * Obligatorio; exactamente 10 dígitos numéricos; debe ser único en el sistema
     * (excepto el del cliente actual).
     */
    @NotBlank(message = "Este campo es obligatorio")
    @Pattern(
        regexp = "^[0-9]{10}$",
        message = "Ingresa un número de teléfono válido de 10 dígitos"
    )
    private String telefono;

    /**
     * Dirección de domicilio del cliente.
     * Opcional; máximo 255 caracteres.
     */
    @Size(max = 255, message = "La dirección no puede exceder 255 caracteres")
    private String direccion;

    /**
     * Flag de aceptación de términos y condiciones.
     * Obligatorio; debe ser {@code true}.
     */
    @NotNull(message = "Debes aceptar los términos y condiciones")
    @AssertTrue(message = "Debes aceptar los términos y condiciones")
    private Boolean aceptaTerminos;
}
