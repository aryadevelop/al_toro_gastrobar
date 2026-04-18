package co.edu.unicauca.backend.modules.reservas.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Payload que envía el frontend al crear una nueva reserva.
 *
 * <p>Los campos {@code decoracionId}, {@code zonaId}, {@code notas} y {@code preOrden}
 * son opcionales; se admite {@code null} para indicar que el cliente no los seleccionó.
 *
 * <p>Reglas de uso:
 * <ul>
 *   <li>{@code fechaHoraLlegada} debe ser una fecha/hora futura.</li>
 *   <li>{@code numeroPersonas} debe ser al menos 1.</li>
 *   <li>{@code preOrden} {@code null} o lista vacía equivale a reserva sin pre-orden.</li>
 *   <li>Cada ítem de {@code preOrden} es validado en cascada mediante {@code @Valid}.</li>
 * </ul>
 *
 * @see PreOrdenItemRequest
 */
@Getter
@Setter
@NoArgsConstructor
public class CrearReservaRequest {

    /**
     * Correo del cliente para quien se crea la reserva.
     * Obligatorio cuando quien crea la reserva es CAJERO o ADMIN; ignorado si es CLIENTE.
     */
    @Email(message = "El email del cliente no tiene un formato válido")
    private String emailCliente;

    /**
     * Fecha y hora en que el cliente planea llegar al restaurante.
     * Debe ser una fecha/hora futura; obligatorio.
     */
    @NotNull(message = "La fecha y hora de llegada son obligatorias")
    @Future(message = "La fecha y hora de llegada deben ser en el futuro")
    private LocalDateTime fechaHoraLlegada;

    /**
     * Número de comensales para la reserva.
     * Valor mínimo: {@code 1}; obligatorio.
     */
    @NotNull(message = "El número de personas es obligatorio")
    @Min(value = 1, message = "El número de personas debe ser al menos 1")
    private Integer numeroPersonas;

    /** ID de la decoración seleccionada por el cliente; {@code null} si no aplica. */
    private Long decoracionId;

    /** ID de la zona del restaurante preferida por el cliente; {@code null} si no aplica. */
    private Long zonaId;

    /** Observaciones o peticiones especiales del cliente; {@code null} si no hay notas. */
    private String notas;

    /**
     * Lista de ítems que conforman la pre-orden del cliente.
     * {@code null} o lista vacía indica que la reserva no incluye pre-orden.
     * Cada elemento es validado en cascada mediante {@code @Valid}.
     *
     * @see PreOrdenItemRequest
     */
    @Valid
    private List<PreOrdenItemRequest> preOrden;
}
