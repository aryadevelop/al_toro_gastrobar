package co.edu.unicauca.backend.modules.reservas.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Representa un ítem de la pre-orden enviado por el cliente al crear una reserva.
 *
 * Reglas de uso:
 * - productoId es siempre obligatorio.
 * - descripcion: null si es ítem normal; "Nombre - modificación" si tiene modificación libre.
 * - esMenuEspecial: true cuando el ítem es un menú especial.
 * - opcionesModificacion: IDs de OpcionModificacion seleccionados.
 *   Solo aplica cuando esMenuEspecial = true.
 */
@Getter
@Setter
@NoArgsConstructor
public class PreOrdenItemRequest {

    @NotNull(message = "El producto es obligatorio")
    private Long productoId;

    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad mínima es 1")
    @Max(value = 250, message = "La cantidad máxima por producto es 250")
    private Integer cantidad;

    /** Texto de modificación libre embebido: "Bandeja Paisa - sin huevo". Null si sin modificación. */
    private String descripcion;

    /** true cuando el ítem corresponde a un menú especial. */
    private Boolean esMenuEspecial;

    /** IDs de OpcionModificacion seleccionados mediante checkboxes (CA-07). */
    private List<Long> opcionesModificacion;
}
