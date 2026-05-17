package co.edu.unicauca.backend.modules.inventario.dto.response;

/**
 * Bebida disponible para un menú especial.
 */
public record ProductoBebidaResponse(
    Long productoId,
    String productoNombre
) {}
