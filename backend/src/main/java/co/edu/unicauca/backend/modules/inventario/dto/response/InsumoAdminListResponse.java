package co.edu.unicauca.backend.modules.inventario.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO de respuesta para el listado de insumos en el panel de administración.
 */
@Getter
@Builder
public class InsumoAdminListResponse {

    private final Long insumoId;
    private final String insumoNombre;
    private final String insumoUnidad;
    private final BigDecimal insumoStockActual;
    private final String insumoEstado;
    private final String tipoInsumo;
    private final LocalDate insumoFechaVencimiento;

    /** {@code true} si la fecha de vencimiento está dentro de los próximos 7 días. */
    private final Boolean vencimientoProximo;
}
