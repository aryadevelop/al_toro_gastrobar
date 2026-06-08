package co.edu.unicauca.backend.modules.inventario.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * Resultado del buscador de inventario para el formulario de ajuste manual.
 *
 * <p>Representa un {@code Producto} o un {@code Insumo}. El campo {@code tipo}
 * discrimina el origen y determina qué identificador usar en la solicitud de
 * ajuste.
 */
@Getter
@Builder
public class ItemAjusteInventarioResponse {

    /** Discriminador: {@code "PRODUCTO"} o {@code "INSUMO"}. */
    private final String tipo;

    /** Identificador del producto o insumo. */
    private final Long id;

    /** Nombre del producto o insumo. */
    private final String nombre;

    /** Stock actual disponible; {@code null} si el producto no gestiona stock. */
    private final BigDecimal stockActual;

    /**
     * Unidad de medida: {@code "UNIDAD"} para productos, o el nombre del enum
     * {@code UnidadMedida} para insumos.
     */
    private final String unidadMedida;
}
