package co.edu.unicauca.backend.modules.usuarios.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrearEmpleadoRequest {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no debe exceder 100 caracteres")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$", message = "El nombre solo puede contener letras y espacios")
    private String nombre;

    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "Ingresa un correo electrónico válido")
    @Size(max = 150, message = "El correo no debe exceder 150 caracteres")
    private String correoElectronico;

    @NotBlank(message = "El teléfono es obligatorio")
    @Pattern(regexp = "^[0-9]{10}$", message = "Ingresa un número de teléfono válido (10 dígitos)")
    private String telefono;

    @Size(max = 255, message = "La dirección no debe exceder 255 caracteres")
    private String direccion;

    @NotEmpty(message = "El rol es obligatorio")
    private List<@NotBlank(message = "El rol no puede estar vacío") String> roles;

    @NotNull(message = "La fecha de ingreso es obligatoria")
    private LocalDate fechaIngreso;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    private String password;

    @NotBlank(message = "La confirmación de contraseña es obligatoria")
    private String passwordConfirmacion;
}
