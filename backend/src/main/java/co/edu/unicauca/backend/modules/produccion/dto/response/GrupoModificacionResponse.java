package co.edu.unicauca.backend.modules.produccion.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.List;


/**
 * Respuesta que agrupa las opciones de modificación de un menú especial por tipo de componente.
 *
 * <p>Un menú especial puede tener varios componentes personalizables. 
 * Este DTO reúne todas las opciones disponibles para un mismo tipo de componente.
 *
 * <p>Reglas de uso:
 * <ul>
 *   <li>{@code tipoComponente} corresponde al nombre del enumerado
 *   <li>{@code opciones} nunca es {@code null}; contiene al menos un elemento.</li>
 * </ul>
 *
 * @see OpcionModificacionResponse
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GrupoModificacionResponse {

    /** Nombre del tipo de componente (valor del enumerado {@code TipoComponente}). */
    private String tipoComponente;

    /** Descripción legible del tipo de componente. */
    private String tipoComponenteDescripcion;

    /**
     * Lista de opciones disponibles para este tipo de componente.
     * Nunca es {@code null}; contiene al menos un elemento.
     *
     * @see OpcionModificacionResponse
     */
    private List<OpcionModificacionResponse> opciones;
}
