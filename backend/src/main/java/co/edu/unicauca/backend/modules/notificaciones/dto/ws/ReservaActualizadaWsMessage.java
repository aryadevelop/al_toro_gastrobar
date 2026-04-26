package co.edu.unicauca.backend.modules.notificaciones.dto.ws;

import lombok.Builder;
import lombok.Getter;

/**
 * Payload WebSocket para notificar cambios en reservas activas.
 *
 * <p>Se publica en {@code /topic/reservas/cambios} cuando se crea o modifica
 * una reserva en estado CONFIRMADA o PENDIENTE, permitiendo que el frontend
 * de meseros actualice la lista automáticamente.
 */
@Getter
@Builder
public class ReservaActualizadaWsMessage {

    /** Identificador de la reserva que cambió. */
    private final Long reservaId;

    /** Tipo de cambio: {@code CREADA} o {@code MODIFICADA}. */
    private final String tipoEvento;

    /** Nombre del cliente de la reserva. */
    private final String clienteNombre;

    /** Hora de llegada en formato {@code HH:mm}. */
    private final String horaLlegada;

    /** Nombre de la zona; {@code null} si no fue asignada. */
    private final String zonaNombre;
}
