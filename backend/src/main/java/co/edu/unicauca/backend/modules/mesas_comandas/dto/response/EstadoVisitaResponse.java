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

    /** Lista de ítems de la visita (todas las comandas activas). */
    private final List<ItemVisitaResponse> items;

    /** Suma de todos los subtotales; {@code BigDecimal.ZERO} si no hay ítems. */
    private final BigDecimal total;

    /**
     * {@code true} si hay una notificación ATENCION ACTIVA para esta mesa.
     * El frontend usa este campo al recargar para saber si el botón está deshabilitado.
     */
    private final boolean asistenciaSolicitada;

    /**
     * ID de la notificación de asistencia activa; {@code null} si no hay ninguna.
     */
    private final Long notificacionAsistenciaId;
}
