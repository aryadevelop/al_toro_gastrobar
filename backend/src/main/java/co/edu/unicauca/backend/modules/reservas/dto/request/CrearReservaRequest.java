package co.edu.unicauca.backend.modules.reservas.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Payload que envía el frontend al crear una nueva reserva.
 *
 * <p><b>DEV:</b> {@code emailCliente} es un bypass temporal mientras el módulo de
 * autenticación no está implementado. Cuando esté disponible, el email se extraerá
 * del JWT y este campo se eliminará.
 */
@Getter
@Setter
@NoArgsConstructor
public class CrearReservaRequest {

    /**
     * DEV: email del cliente. Se usa cuando no hay token JWT disponible.
     * Eliminar cuando el módulo de auth esté implementado.
     */
    @NotBlank(message = "El email del cliente es obligatorio")
    private String emailCliente;

    @NotNull(message = "La fecha y hora de llegada son obligatorias")
    private LocalDateTime fechaHoraLlegada;

    @NotNull(message = "El número de personas es obligatorio")
    @Min(value = 1, message = "El número de personas debe ser al menos 1")
    private Integer numeroPersonas;

    /** ID de la decoración seleccionada */
    private Long decoracionId;

    /** ID de la zona seleccionada */
    private Long zonaId;

    /** Notas adicionales del cliente */
    private String notas;
}
