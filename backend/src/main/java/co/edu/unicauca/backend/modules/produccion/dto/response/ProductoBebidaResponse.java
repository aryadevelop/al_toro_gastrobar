package co.edu.unicauca.backend.modules.produccion.dto.response;

import java.math.BigDecimal;

/**
 * Bebida disponible para un menú especial.
 * El precio es el de venta suelta; cuando se elige dentro de un menú,
 * el ComandaItem de BARRA se persiste con precio 0.
 */
public record ProductoBebidaResponse(
    Long productoId,
    String productoNombre,
    BigDecimal productoPrecio
) {}
