package co.edu.unicauca.backend.modules.inventario.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


/**
 * Respuesta que representa una opción de modificación disponible dentro de un grupo de componente.
 *
 * <p>Cada opción pertenece a un {@link GrupoModificacionResponse} y corresponde a un ingrediente
 * o variante concreta que el cliente puede elegir para personalizar un menú especial
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpcionModificacionResponse {

    /** Identificador único de la opción de modificación. */
    private Long opcionId;

    /** Nombre de la opción a mostrar. */
    private String opcionNombre;
}
