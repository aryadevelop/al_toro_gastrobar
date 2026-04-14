package co.edu.unicauca.backend.modules.reservas.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Payload que envía el frontend al crear una nueva reserva.
 */
@Getter
@Setter
@NoArgsConstructor
public class CrearReservaRequest {

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
