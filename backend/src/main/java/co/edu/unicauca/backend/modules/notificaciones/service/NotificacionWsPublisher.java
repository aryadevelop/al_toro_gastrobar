package co.edu.unicauca.backend.modules.notificaciones.service;

import co.edu.unicauca.backend.modules.notificaciones.dto.ws.AsistenciaAtendidaWsMessage;
import co.edu.unicauca.backend.modules.notificaciones.dto.ws.AsistenciaSolicitadaWsMessage;
import co.edu.unicauca.backend.modules.notificaciones.dto.ws.CuentaCerradaWsMessage;
import co.edu.unicauca.backend.modules.notificaciones.dto.ws.VisitaActualizadaWsMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Publica mensajes WebSocket a los tópicos STOMP del sistema.
 *
 * <p>Tópicos:
 * <ul>
 *   <li>{@code /topic/visita/{visitaId}/orden} — actualización de ítems de comanda.</li>
 *   <li>{@code /topic/visita/{visitaId}/cuenta} — cuenta cerrada (incluye puntos actualizados).</li>
 *   <li>{@code /topic/visita/{visitaId}/asistencia} — asistencia atendida (CA-05).</li>
 *   <li>{@code /topic/mesas/asistencia} — broadcast a empleados.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class NotificacionWsPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Notifica al cliente que la lista de ítems de su visita cambió.
     * Call site: servicio de creación/modificación de comanda (HU siguiente).
     */
    public void publicarVisitaActualizada(Long visitaId, VisitaActualizadaWsMessage mensaje) {
        messagingTemplate.convertAndSend("/topic/visita/" + visitaId + "/orden", mensaje);
    }

    /**
     * Notifica al cliente que la cuenta fue cerrada.
     * El mensaje incluye {@code puntosActuales} para que el frontend actualice el saldo.
     * Call site: VentaService.cerrarCuenta.
     */
    public void publicarCuentaCerrada(Long visitaId, CuentaCerradaWsMessage mensaje) {
        messagingTemplate.convertAndSend("/topic/visita/" + visitaId + "/cuenta", mensaje);
    }

    /**
     * Notifica al cliente que su solicitud de asistencia fue atendida.
     * Call site: NotificacionService.atenderAsistencia.
     */
    public void publicarAsistenciaAtendida(Long visitaId, AsistenciaAtendidaWsMessage mensaje) {
        messagingTemplate.convertAndSend("/topic/visita/" + visitaId + "/asistencia", mensaje);
    }

    /**
     * Broadcast a todos los empleados conectados que hay una nueva solicitud de asistencia.
     * Call site: NotificacionService.solicitarAsistencia.
     */
    public void publicarAsistenciaSolicitada(AsistenciaSolicitadaWsMessage mensaje) {
        messagingTemplate.convertAndSend("/topic/mesas/asistencia", mensaje);
    }
}
