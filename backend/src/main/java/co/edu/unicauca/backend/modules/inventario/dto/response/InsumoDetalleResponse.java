package co.edu.unicauca.backend.modules.inventario.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO de respuesta con el detalle completo de un insumo para administración.
 *
 * <p>Incluye los datos necesarios para el panel de detalle del administrador:
 * stock actual, costo unitario y fecha de vencimiento.
 */
@Getter
@Builder
public class InsumoDetalleResponse {

    /** Identificador único del insumo. */
    private final Long insumoId;

    /** Nombre del insumo. */
    private final String insumoNombre;

    /** Unidad de medida del insumo (KG, L, UNIDAD, etc.). */
    private final String insumoUnidad;

    /** Stock disponible actualmente. */
    private final BigDecimal insumoStockActual;

    /** Stock mínimo para alertas (derivado de la configuración). */
    private final BigDecimal insumoStockMinimo;

    /** Estado operativo: ACTIVO o INACTIVO. */
    private final String insumoEstado;

    /** Tipo de insumo: MATERIA_PRIMA o SEMIELABORADO. */
    private final String tipoInsumo;

    /** Costo unitario actual del insumo; puede ser nulo si no se ha registrado. */
    private final BigDecimal insumoCosoUnitario;

    /** Fecha de vencimiento del insumo; nulo si no aplica. */
    private final LocalDate insumoFechaVencimiento;

    /**
     * Indica si la fecha de vencimiento se aproxima (dentro de los próximos 7 días).
     * {@code null} si no hay fecha de vencimiento registrada.
     */
    private final Boolean vencimientoProximo;
}
