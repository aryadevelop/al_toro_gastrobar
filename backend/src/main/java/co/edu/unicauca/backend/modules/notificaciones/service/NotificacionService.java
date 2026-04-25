package co.edu.unicauca.backend.modules.notificaciones.service;

import co.edu.unicauca.backend.modules.mesas_comandas.entity.Mesa;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Visita;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.MesaRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.VisitaRepository;
import co.edu.unicauca.backend.modules.notificaciones.dto.response.NotificacionAsistenciaResponse;
import co.edu.unicauca.backend.modules.notificaciones.dto.ws.AsistenciaAtendidaWsMessage;
import co.edu.unicauca.backend.modules.notificaciones.dto.ws.AsistenciaSolicitadaWsMessage;
import co.edu.unicauca.backend.modules.notificaciones.entity.Notificacion;
import co.edu.unicauca.backend.modules.notificaciones.repository.NotificacionRepository;
import co.edu.unicauca.backend.shared.enums.EstadoNotificacion;
import co.edu.unicauca.backend.shared.enums.TipoNotificacion;
import co.edu.unicauca.backend.shared.exception.BusinessException;
import co.edu.unicauca.backend.shared.exception.ErrorCode;
import co.edu.unicauca.backend.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Servicio de negocio para solicitar y atender asistencia de mesero en mesa.
 *
 * <p>Coordina la persistencia de {@link Notificacion} y la publicación
 * de eventos WebSocket vía {@link NotificacionWsPublisher}.
 */
@Service
@RequiredArgsConstructor
public class NotificacionService {

    private final VisitaRepository visitaRepository;
    private final MesaRepository mesaRepository;
    private final NotificacionRepository notificacionRepository;
    private final NotificacionWsPublisher wsPublisher;

    /**
     * Registra una solicitud de asistencia para la mesa de la visita indicada.
     *
     * <p>Valida que:
     * <ul>
     *   <li>La visita exista y pertenezca al cliente autenticado.</li>
     *   <li>La visita tenga una mesa asignada.</li>
     *   <li>No exista ya una solicitud de asistencia ACTIVA para esa mesa.</li>
     * </ul>
     * Al completar, persiste la notificación y publica broadcast en
     * {@code /topic/mesas/asistencia}.
     *
     * @param visitaId     identificador de la visita activa del cliente
     * @param emailCliente correo del cliente autenticado
     * @return DTO con el ID de la notificación creada y su estado
     * @throws ResourceNotFoundException si la visita no existe
     * @throws BusinessException         si el cliente no es dueño, sin mesa, o ya hay solicitud activa
     */
    @Transactional
    public NotificacionAsistenciaResponse solicitarAsistencia(Long visitaId, String emailCliente) {

        // Verifica que la visita exista
        Visita visita = visitaRepository.findById(visitaId)
                .orElseThrow(() -> new ResourceNotFoundException("Visita", visitaId));

        // Solo el cliente dueño de la visita puede solicitar asistencia
        boolean esDelCliente = visita.getCliente() != null &&
                emailCliente.equals(visita.getCliente().getUsuario().getUsuarioEmail());
        if (!esDelCliente) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED,
                    "No tienes acceso a esta visita.", HttpStatus.FORBIDDEN);
        }

        // No se puede duplicar una solicitud mientras haya una ACTIVA
        boolean asistenciaActiva = notificacionRepository
                .findFirstByMesa_VisitaIdAndNotificacionTipoAndNotificacionEstado(
                        visitaId, TipoNotificacion.ATENCION, EstadoNotificacion.ACTIVA)
                .isPresent();
        if (asistenciaActiva) {
            throw new BusinessException(ErrorCode.INVALID_STATE,
                    "Ya existe una solicitud de asistencia activa para esta mesa.",
                    HttpStatus.CONFLICT);
        }

        // La visita debe tener mesa asignada para poder notificar al mesero
        Mesa mesa = mesaRepository.findByVisita_VisitaId(visitaId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BUSINESS_ERROR,
                        "Esta visita no tiene mesa asignada aún.",
                        HttpStatus.CONFLICT));

        // Persiste la notificación de asistencia
        Notificacion notificacion = Notificacion.builder()
                .mesa(mesa)
                .empleado(mesa.getMesero())
                .notificacionTipo(TipoNotificacion.ATENCION)
                .notificacionEstado(EstadoNotificacion.ACTIVA)
                .build();
        notificacion = notificacionRepository.save(notificacion);

        String clienteNombre = visita.getCliente() != null
                ? visita.getCliente().getClienteNombre()
                : "Cliente";

        // Broadcast a todos los empleados conectados con los datos de la mesa
        wsPublisher.publicarAsistenciaSolicitada(AsistenciaSolicitadaWsMessage.builder()
                .visitaId(visitaId)
                .notificacionId(notificacion.getNotificacionId())
                .mesaIdentificador(mesa.getMesaIdentificador())
                .clienteNombre(clienteNombre)
                .fechaHora(LocalDateTime.now())
                .build());

        return NotificacionAsistenciaResponse.builder()
                .notificacionId(notificacion.getNotificacionId())
                .estado("ACTIVA")
                .build();
    }

    /**
     * Marca una solicitud de asistencia como atendida y notifica al cliente.
     *
     * <p>Cambia el estado de ACTIVA a ATENDIDA y publica en
     * {@code /topic/visita/{visitaId}/asistencia} para que el frontend
     * re-habilite el botón "Solicitar asistencia".
     *
     * @param notificacionId identificador de la notificación a atender
     * @param emailEmpleado  correo del mesero autenticado
     * @throws ResourceNotFoundException si la notificación no existe
     * @throws BusinessException         si la notificación ya fue atendida
     */
    @Transactional
    public void atenderAsistencia(Long notificacionId, String emailEmpleado) {

        Notificacion notificacion = notificacionRepository.findById(notificacionId)
                .orElseThrow(() -> new ResourceNotFoundException("Notificacion", notificacionId));

        // No se puede atender una solicitud que ya fue procesada
        if (notificacion.getNotificacionEstado() == EstadoNotificacion.ATENDIDA) {
            throw new BusinessException(ErrorCode.INVALID_STATE,
                    "Esta solicitud de asistencia ya fue atendida.",
                    HttpStatus.CONFLICT);
        }
        
        notificacion.setNotificacionEstado(EstadoNotificacion.ATENDIDA);
        notificacionRepository.save(notificacion);

        Long visitaId = notificacion.getMesa().getVisitaId();

        // Notifica al cliente que puede volver a solicitar asistencia
        wsPublisher.publicarAsistenciaAtendida(visitaId, AsistenciaAtendidaWsMessage.builder()
                .visitaId(visitaId)
                .asistenciaAtendida(true)
                .build());
    }
}
