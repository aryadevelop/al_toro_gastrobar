package co.edu.unicauca.backend.modules.reservas.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Payload para crear un bloqueo de disponibilidad.
 * Si horaInicio y horaFin son nulos, se bloquea el día completo.
 */
@Getter
@Setter
@NoArgsConstructor
public class CrearBloqueRequest {

    @NotNull(message = "La fecha de inicio es obligatoria")
    private LocalDate fechaInicio;

    @NotNull(message = "La fecha de fin es obligatoria")
    private LocalDate fechaFin;

    /**
     * Hora de inicio del bloqueo (inclusive).
     * Si es null, el bloqueo aplica al día completo.
     */
    private LocalTime horaInicio;

    /**
     * Hora de fin del bloqueo (exclusiva).
     * Si es null, el bloqueo aplica al día completo.
     */
    private LocalTime horaFin;

    private String motivo;
}
