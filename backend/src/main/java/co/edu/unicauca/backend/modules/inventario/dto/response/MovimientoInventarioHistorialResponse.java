package co.edu.unicauca.backend.modules.inventario.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO de respuesta para el historial de movimientos de inventario.
 */
@Getter
@Builder
public class MovimientoInventarioHistorialResponse {

    private final Long movimientoId;
    private final String tipo;
    private final BigDecimal cantidad;
    private final LocalDateTime movimientoFechaHora;
    private final String observaciones;
    private final Long productoId;
    private final Long insumoId;
}
