package co.edu.unicauca.backend.modules.inventario.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Respuesta de una operación de cambio de estado.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CambioEstadoResponse {

    private Long id;

    private String mensaje;

    private Integer pedidosPendientes;

    private Integer preparacionesAfectadas;

    private Boolean notificarClientes;
}
