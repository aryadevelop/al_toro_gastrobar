package co.edu.unicauca.backend.modules.mesas_comandas.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

/**
 * Estado completo de la visita activa del cliente.
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EstadoVisitaResponse {

    /** Identificador de la visita activa. */
    private final Long visitaId;

    /** Etiqueta física de la mesa; {@code null} si el mesero no ha asignado mesa aún. */
    private final String mesaIdentificador;

    /** {@code true} si la cuenta ya fue cerrada (visitaFechaHoraFin != null). */
    private final boolean visitaCerrada;

    /** Lista de ítems de la visita. */
    private final List<ItemVisitaResponse> items;

    /** Total de la pre-orden (suma de subtotales de ítems activos). */
    private final BigDecimal totalPreorden;

    /** Costo adicional de la decoración; {@code null} si la visita no tiene decoración con costo. */
    private final BigDecimal valorDecoracion;

    /** Total a pagar ({@code totalPreorden + valorDecoracion}). */
    private final BigDecimal totalAPagar;

    /** Monto neto abonado (anticipos − devoluciones); {@code 0} si no hubo abonos. */
    private final BigDecimal montoAbonado;

    /** Saldo pendiente ({@code totalAPagar − montoAbonado}). */
    private final BigDecimal saldoPendiente;

    /**
     * {@code true} si hay una notificación ATENCION ACTIVA para esta mesa.
     * El frontend puede usar este campo al recargar para saber si el botón está deshabilitado.
     */
    private final boolean asistenciaSolicitada;

    /**
     * ID de la notificación de asistencia activa; {@code null} si no hay ninguna.
     */
    private final Long notificacionAsistenciaId;
}
