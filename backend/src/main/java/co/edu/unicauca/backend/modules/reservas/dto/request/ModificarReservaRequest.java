package co.edu.unicauca.backend.modules.reservas.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Datos enviados por el cliente al modificar una reserva existente.
 *
 * <p>Aplica las mismas reglas de validación que {@link CrearReservaRequest},
 * salvo que el email del cliente se toma siempre del token de autenticación.
 */
@Getter
@NoArgsConstructor
public class ModificarReservaRequest {

    /** Nueva fecha y hora de llegada; debe ser una fecha futura. */
    @NotNull
    @Future
    private LocalDateTime fechaHoraLlegada;

    /** Número de comensales; debe ser al menos 1. */
    @NotNull
    @Min(1)
    private Integer numeroPersonas;

    /** Identificador de la decoración solicitada; {@code null} si no se elige decoración. */
    private Long decoracionId;

    /** Identificador de la zona solicitada; {@code null} si no se elige zona. */
    private Long zonaId;

    /** Observaciones o peticiones especiales del cliente; {@code null} si no hay notas. */
    private String notas;

    /** Ítems de pre-orden; {@code null} o vacía si no se solicita pre-orden. */
    @Valid
    private List<PreOrdenItemRequest> preOrden;
}
