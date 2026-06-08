package co.edu.unicauca.backend.modules.mesas_comandas.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO de entrada para asignar identificador a una mesa.
 *
 * <p>Este request se utiliza tanto para mesas walk-in
 * como para mesas creadas al marcar la llegada de una reserva confirmada.
 *
 * <p>Validaciones:
 * <ul>
 *   <li>{@code mesaIdentificador}: obligatorio, máximo 20 caracteres</li>
 *   <li>{@code zonaId}: obligatorio</li>
 *   <li>{@code numeroPersonas}: obligatorio, mínimo 1</li>
 *   <li>{@code reservaId}: opcional; si presente, debe ser reserva CONFIRMADA del día</li>
 *   <li>{@code mesaNotas}: opcional</li>
 * </ul>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AsignarMesaRequest {

    /**
     * Identificador físico de la mesa.
     * Obligatorio; máximo 20 caracteres.
     */
    @NotBlank(message = "El identificador de mesa es obligatorio")
    @Size(max = 20, message = "El identificador no puede superar los 20 caracteres")
    private String mesaIdentificador;

    /**
     * ID de la zona del restaurante donde se ubica la mesa.
     * Obligatorio; debe existir en la base de datos.
     */
    @NotNull(message = "La zona es obligatoria")
    private Long zonaId;

    /**
     * Número de personas que ocuparán la mesa.
     * Obligatorio; mínimo 1.
     */
    @NotNull(message = "El número de personas es obligatorio")
    @Min(value = 1, message = "El número de personas debe ser al menos 1")
    private Integer numeroPersonas;

    /**
     * ID de la reserva confirmada.
     * Opcional; {@code null} para mesas walk-in.
     */
    private Long reservaId;

    /**
     * Notas adicionales de la mesa/visita.
     * Opcional; {@code null} si no hay observaciones.
     */
    private String mesaNotas;
}
