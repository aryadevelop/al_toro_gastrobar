package co.edu.unicauca.backend.modules.inventario.dto.request;

import co.edu.unicauca.backend.shared.enums.EstadoGenerico;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request para cambiar el estado de un producto o un insumo.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CambioEstadoRequest {

    @NotNull(message = "El estado es obligatorio")
    private EstadoGenerico estado;

    /**
     * Motivo opcional del cambio de estado.
     */
    private String motivo;

    /**
     * Indicador de si se debe notificar a los clientes afectados.
     */
    private Boolean notificarClientes;
}
