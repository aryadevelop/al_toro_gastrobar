package co.edu.unicauca.backend.modules.mesas_comandas.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * DTO para representar un item de comanda en producción.
 *
 * <p>Items en estados EN_PREPARACION, LISTO, COMPLETADO se agrupan por
 * (nombreProducto + descripcion + estadoComanda).
 * Items en estado PENDIENTE NO se agrupan (RN-06).
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

    /** Cantidad total (agrupada si aplica según RN-06) */
    private final Integer cantidad;

    /** Estado de la comanda: "PENDIENTE", "EN_PREPARACION", "LISTO", "COMPLETADO" */
    private final String estadoComanda;
}
