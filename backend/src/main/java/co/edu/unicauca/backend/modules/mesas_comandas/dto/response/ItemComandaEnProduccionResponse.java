package co.edu.unicauca.backend.modules.mesas_comandas.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * DTO para representar un item de comanda en producción.
 *
 * <p>Items en estados EN_PREPARACION, LISTO, COMPLETADO se agrupan
 * Items en estado PENDIENTE NO se agrupan.
 */
@Getter
@Builder
public class ItemComandaEnProduccionResponse {

    /** Nombre del producto */
    private final String nombreProducto;

    /** Descripción con modificaciones (puede ser null) */
    private final String descripcion;

    /** Categoría del producto: "PLATO", "BEBIDA", "OTRO" */
    private final String categoriaProducto;

    /** Cantidad total */
    private final Integer cantidad;

    /** Estado de la comanda: "PENDIENTE", "EN_PREPARACION", "LISTO", "COMPLETADO" */
    private final String estadoComanda;
}
