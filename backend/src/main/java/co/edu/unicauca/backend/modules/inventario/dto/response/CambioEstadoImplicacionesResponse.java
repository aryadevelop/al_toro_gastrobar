package co.edu.unicauca.backend.modules.inventario.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Información de implicaciones antes de cambiar el estado de un producto o insumo.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CambioEstadoImplicacionesResponse {

    private Long id;

    private String estadoActual;

    private String estadoSolicitado;

    private Integer pedidosPendientes;

    private Integer preparacionesAfectadas;

    private String mensaje;
}
