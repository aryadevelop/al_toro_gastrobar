package co.edu.unicauca.backend.modules.mesas_comandas.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * Bebida del menú especial fusionada con el plato del menú dentro del ítem
 * de borrador. La bebida no aporta precio: el cobro completo del menú vive
 * en el ítem del plato. Solo se expone identidad para que el frontend pinte
 * la bebida elegida.
 */
@Getter @Builder
public class ItemBebidaMenuResponse {
    /** Identificador del producto bebida. */
    private final Long productoId;
    /** Nombre del producto bebida. */
    private final String productoNombre;
}
