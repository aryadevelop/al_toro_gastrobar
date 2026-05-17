package co.edu.unicauca.backend.modules.mesas_comandas.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * Opción de modificación seleccionada sobre el plato de un menú especial
 * tal como se muestra en el formulario de modificar comanda.
 */
@Getter @Builder
public class OpcionMenuSeleccionadaResponse {
    /** Identificador de la opción dentro del catálogo de modificaciones. */
    private final Long opcionId;

    /** Nombre legible de la opción. */
    private final String opcionNombre;
    
    /** Tipo de componente del menú: {@code "ARROZ"}, {@code "SALSA"}, etc. */
    private final String tipoComponente;
}
