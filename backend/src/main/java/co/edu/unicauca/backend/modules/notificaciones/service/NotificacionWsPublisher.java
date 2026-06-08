package co.edu.unicauca.backend.modules.notificaciones.service;

import co.edu.unicauca.backend.modules.notificaciones.dto.ws.AsistenciaAtendidaWsMessage;
import co.edu.unicauca.backend.modules.notificaciones.dto.ws.AsistenciaSolicitadaWsMessage;
import co.edu.unicauca.backend.modules.notificaciones.dto.ws.ComandaProduccionEventoWsMessage;
import co.edu.unicauca.backend.modules.notificaciones.dto.ws.CuentaCerradaWsMessage;
import co.edu.unicauca.backend.modules.notificaciones.dto.ws.ReservaActualizadaWsMessage;
import co.edu.unicauca.backend.modules.notificaciones.dto.ws.VisitaActualizadaWsMessage;
import co.edu.unicauca.backend.shared.enums.EstacionComanda;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Publica mensajes WebSocket a los tópicos STOMP del sistema.
 *
 * <p>Tópicos:
 * <ul>
 *   <li>{@code /topic/visita/{visitaId}/orden} — actualización de ítems de comanda.</li>
 *   <li>{@code /topic/visita/{visitaId}/cuenta} — cuenta cerrada.</li>
 *   <li>{@code /topic/visita/{visitaId}/asistencia} — asistencia atendida.</li>
 *   <li>{@code /topic/mesas/asistencia} — broadcast a empleados.</li>
 *   <li>{@code /topic/reservas/cambios} — broadcast de cambios en reservas activas.</li>
 *   <li>{@code /topic/produccion/cocina} y {@code /topic/produccion/barra} —
 *       eventos del ciclo de vida de comandas en cada estación.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class NotificacionWsPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    private static final String TOPIC_VISITA = "/topic/visita/";
    private static final String TOPIC_PRODUCCION_PREFIX = "/topic/produccion/";

    /**
     * Notifica al cliente que la lista de ítems de su visita cambió.
     * Call site: servicio de creación/modificación de comanda.
     */
    public void publicarVisitaActualizada(Long visitaId, VisitaActualizadaWsMessage mensaje) {
        messagingTemplate.convertAndSend(TOPIC_VISITA + visitaId + "/orden", mensaje);
    }

    /**
     * Notifica al cliente que la cuenta fue cerrada.
     * El mensaje incluye {@code puntosActuales} para que el frontend actualice el saldo.
     * Call site: VentaService.cerrarCuenta.
     */
    public void publicarCuentaCerrada(Long visitaId, CuentaCerradaWsMessage mensaje) {
        messagingTemplate.convertAndSend(TOPIC_VISITA + visitaId + "/cuenta", mensaje);
    }

    /**
     * Notifica al cliente que su solicitud de asistencia fue atendida.
     * Call site: NotificacionService.atenderAsistencia.
     */
    public void publicarAsistenciaAtendida(Long visitaId, AsistenciaAtendidaWsMessage mensaje) {
        messagingTemplate.convertAndSend(TOPIC_VISITA + visitaId + "/asistencia", mensaje);
    }

    /**
     * Broadcast a todos los empleados conectados que hay una nueva solicitud de asistencia.
     * Call site: NotificacionService.solicitarAsistencia.
     */
    public void publicarAsistenciaSolicitada(AsistenciaSolicitadaWsMessage mensaje) {
        messagingTemplate.convertAndSend("/topic/mesas/asistencia", mensaje);
    }

    /**
     * Broadcast a todos los meseros conectados que hubo un cambio en las reservas activas.
     * Call site: ReservaService al crear o modificar una reserva CONFIRMADA/PENDIENTE.
     */
    public void publicarReservaActualizada(ReservaActualizadaWsMessage mensaje) {
        messagingTemplate.convertAndSend("/topic/reservas/cambios", mensaje);
    }

    /**
     * Emite un evento de producción al tópico correspondiente a la estación
     * indicada. El sufijo del tópico es la versión en minúscula del nombre de
     * la estación: {@code cocina} o {@code barra}.
     *
     * <p>El contrato unificado del mensaje permite a los suscriptores
     * distinguir entre la aparición de una comanda nueva, su retirada del
     * tablero o el registro de su servicio sin acoplarse a tópicos por tipo
     * de evento.
     *
     * @param estacion estación destino; determina el sufijo del tópico y
     *                 debe coincidir con el valor declarado en {@code mensaje.estacion()}
     * @param mensaje  payload del evento
     */
    public void publicarEventoProduccion(EstacionComanda estacion, ComandaProduccionEventoWsMessage mensaje) {
        messagingTemplate.convertAndSend(TOPIC_PRODUCCION_PREFIX + estacion.name().toLowerCase(), mensaje);
    }
}
