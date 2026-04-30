package co.edu.unicauca.backend.modules.mesas_comandas.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO para el detalle completo de una mesa.
 * Usado en el endpoint GET /api/mesas/{mesaId}/detalle.
 */
@Getter
@Builder
public class MesaDetalleResponse {

    /** ID de la mesa (igual a visitaId, pero semánticamente correcto) */
    private final Long mesaId;

    /** ID de la visita (PK de Mesa) */
    private final Long visitaId;

    /** Identificador de la mesa */
    private final String identificador;

    /** Nombre completo del cliente (puede ser null si es walk-in anónimo) */
    private final String nombreCliente;

    /** Fecha y hora de inicio de la visita */
    private final LocalDateTime horaLlegada;

    /** Número de comensales */
    private final Integer numeroPersonas;

    /** Estado de la mesa: "ESPERA", "EN_PREPARACION", "ATENDIDA", "CERRADA" */
    private final String estado;

    /** Notas de la reserva (null si no hay reserva o si la reserva no tiene notas) */
    private final String notasReserva;

    /** Notas generales de la mesa */
    private final String notasMesa;

    /** Notas de todas las comandas concatenadas (separadas por " | ") */
    private final String notasComandas;

    /** Items de comandas en estados: PENDIENTE, EN_PREPARACION, LISTO, COMPLETADO */
    private final List<ItemComandaEnProduccionResponse> itemsComanda;
}
