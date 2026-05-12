package co.edu.unicauca.backend.modules.notificaciones.service;

import co.edu.unicauca.backend.modules.mesas_comandas.entity.Comanda;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Mesa;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Visita;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ComandaRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.MesaRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.VisitaRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.service.MesaAsignarService;
import co.edu.unicauca.backend.modules.notificaciones.dto.response.AtenderCambioResponse;
import co.edu.unicauca.backend.modules.notificaciones.dto.response.NotificacionAsistenciaResponse;
import co.edu.unicauca.backend.modules.notificaciones.dto.ws.AsistenciaAtendidaWsMessage;
import co.edu.unicauca.backend.modules.notificaciones.dto.ws.AsistenciaSolicitadaWsMessage;
import co.edu.unicauca.backend.modules.notificaciones.entity.Notificacion;
import co.edu.unicauca.backend.modules.notificaciones.repository.NotificacionRepository;
import co.edu.unicauca.backend.shared.enums.EstadoComanda;
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
    private final ComandaRepository comandaRepository;
    private final MesaAsignarService mesaAsignarService;
    private final MesaWsPublisher mesaWsPublisher;

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

        // Refresca el mapa de mesas de TODOS los meseros (eliminar ícono de campana)
        mesaWsPublisher.publicarActualizacionMesa(visitaId, MesaWsPublisher.TipoEventoMesa.NOTIFICACION);

                // Re-evalúa si la mesa puede pasar a ATENDIDA
                mesaAsignarService.evaluarYActualizarEstadoMesa(visitaId);
    }

    /**
     * Devuelve la comanda asociada a la notificación o lanza {@link BusinessException}
     * si la relación es {@code null}. Las notificaciones {@code PLATOS_LISTOS},
     * {@code BEBIDAS_LISTAS} y {@code CAMBIO} siempre deben tener una comanda asignada
     * por el flujo de creación (cocinero, bartender o cliente).
     *
     * @param notificacion notificación de la que se extrae la comanda
     * @return comanda no nula
     * @throws BusinessException si {@code notificacion.getComanda() == null}
     */
    private Comanda obtenerComandaObligatoria(Notificacion notificacion) {
        Comanda comanda = notificacion.getComanda();
        if (comanda == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR,
                    "La notificación no tiene una comanda asociada.", HttpStatus.BAD_REQUEST);
        }
        return comanda;
    }

    /**
     * Registra que el mesero sirvió los platos listos de una comanda.
     *
     * <p>Flujo:
     * <ol>
     *   <li>Localiza la notificación; lanza {@link ResourceNotFoundException} si no existe.</li>
     *   <li>Valida que sea de tipo {@code PLATOS_LISTOS} y esté {@code ACTIVA}.</li>
     *   <li>Recupera la comanda asociada; si es {@code null}, lanza {@link BusinessException}.</li>
     *   <li>Marca la comanda como {@code COMPLETADO} y la notificación como {@code ATENDIDA}.</li>
     *   <li>Publica al tópico {@code /topic/comandas/completado} para que el dashboard
     *       del cocinero elimine la comanda de la columna "Listas".</li>
     *   <li>Refresca el mapa de mesas para todos los meseros.</li>
     *   <li>Invoca el evaluador de estado de mesa para una posible transición a {@code ATENDIDA}.</li>
     * </ol>
     *
     * <p>Cualquier mesero o admin puede ejecutar esta operación; no se valida
     * ownership del mesero asignado a la mesa.
     *
     * @param notificacionId identificador de la notificación PLATOS_LISTOS
     * @param emailEmpleado  correo del empleado autenticado (registro/auditoría)
     * @throws ResourceNotFoundException si la notificación no existe
     * @throws BusinessException         si el tipo no es PLATOS_LISTOS, ya está atendida o no tiene comanda
     */
    @Transactional
    public void servirPlatos(Long notificacionId, String emailEmpleado) {

        // Localiza la notificación o lanza excepción si no existe
        Notificacion notificacion = notificacionRepository.findById(notificacionId)
                .orElseThrow(() -> new ResourceNotFoundException("Notificacion", notificacionId));

        // Solo notificaciones de platos listos pueden activar este flujo
        if (notificacion.getNotificacionTipo() != TipoNotificacion.PLATOS_LISTOS) {
            throw new BusinessException(ErrorCode.INVALID_STATE,
                    "La notificación no es de tipo PLATOS_LISTOS.", HttpStatus.CONFLICT);
        }

        // Una notificación ATENDIDA no se puede volver a procesar
        if (notificacion.getNotificacionEstado() != EstadoNotificacion.ACTIVA) {
            throw new BusinessException(ErrorCode.INVALID_STATE,
                    "La notificación ya fue atendida.", HttpStatus.CONFLICT);
        }

        // Obtener la comanda asociada o lanzar excepción si no existe
        Comanda comanda = obtenerComandaObligatoria(notificacion);

        // Actualizar estado de la comanda a COMPLETADO
        comanda.setComandaEstado(EstadoComanda.COMPLETADO);
        comandaRepository.save(comanda);

        // Marcar la notificación como ATENDIDA para que no se procese de nuevo
        notificacion.setNotificacionEstado(EstadoNotificacion.ATENDIDA);
        notificacionRepository.save(notificacion);

        // Notifica al dashboard del cocinero para eliminar la comanda de "Listas"
        wsPublisher.publicarComandaCompletada(comanda.getComandaId(), comanda.getComandaEstacion().name());

        Long visitaId = notificacion.getMesa().getVisitaId();

        // Refresca el mapa de mesas de TODOS los meseros
        mesaWsPublisher.publicarActualizacionMesa(visitaId, MesaWsPublisher.TipoEventoMesa.NOTIFICACION);

        // Re-evalúa si la mesa puede pasar a ATENDIDA
        mesaAsignarService.evaluarYActualizarEstadoMesa(visitaId);
    }

    /**
     * Registra que el mesero sirvió las bebidas listas de una comanda.
     *
     * <p>Idéntico a {@link #servirPlatos(Long, String)} pero valida tipo {@code BEBIDAS_LISTAS}
     * y publica la estación {@code BARRA} en el evento WS.
     *
     * @param notificacionId identificador de la notificación BEBIDAS_LISTAS
     * @param emailEmpleado  correo del empleado autenticado
     * @throws ResourceNotFoundException si la notificación no existe
     * @throws BusinessException         si el tipo no es BEBIDAS_LISTAS, ya está atendida o no tiene comanda
     */
    @Transactional
    public void servirBebidas(Long notificacionId, String emailEmpleado) {

        // Localiza la notificación o lanza excepción si no existe
        Notificacion notificacion = notificacionRepository.findById(notificacionId)
                .orElseThrow(() -> new ResourceNotFoundException("Notificacion", notificacionId));

        // Solo notificaciones de bebidas listas pueden activar este flujo
        if (notificacion.getNotificacionTipo() != TipoNotificacion.BEBIDAS_LISTAS) {
            throw new BusinessException(ErrorCode.INVALID_STATE,
                    "La notificación no es de tipo BEBIDAS_LISTAS.", HttpStatus.CONFLICT);
        }

        // Una notificación ATENDIDA no se puede volver a procesar
        if (notificacion.getNotificacionEstado() != EstadoNotificacion.ACTIVA) {
            throw new BusinessException(ErrorCode.INVALID_STATE,
                    "La notificación ya fue atendida.", HttpStatus.CONFLICT);
        }

        // Obtener la comanda asociada o lanzar excepción si no existe
        Comanda comanda = obtenerComandaObligatoria(notificacion);

        // Actualizar estado de la comanda a COMPLETADO
        comanda.setComandaEstado(EstadoComanda.COMPLETADO);
        comandaRepository.save(comanda);

        // Marcar la notificación como ATENDIDA para que no se procese de nuevo
        notificacion.setNotificacionEstado(EstadoNotificacion.ATENDIDA);
        notificacionRepository.save(notificacion);

        // Notifica al dashboard del bartender para eliminar la comanda de "Listas"
        wsPublisher.publicarComandaCompletada(comanda.getComandaId(), comanda.getComandaEstacion().name());

        Long visitaId = notificacion.getMesa().getVisitaId();

        // Refresca el mapa de mesas de TODOS los meseros (eliminar ícono de bebidas listas)
        mesaWsPublisher.publicarActualizacionMesa(visitaId, MesaWsPublisher.TipoEventoMesa.NOTIFICACION);

        // Re-evalúa si la mesa puede pasar a ATENDIDA
        mesaAsignarService.evaluarYActualizarEstadoMesa(visitaId);
    }

    /**
     * Atiende una notificación de cambio de comanda.
     *
     * <p>Marca la notificación como {@code ATENDIDA} y devuelve el {@code comandaId}
     * para que el frontend cargue la comanda en modo edición.
     *
     * <p>A diferencia de {@code servirPlatos}/{@code servirBebidas}, este flujo:
     * <ul>
     *   <li>NO cambia el estado de la comanda.</li>
     *   <li>NO publica eventos al dashboard de producción.</li>
     *   <li>NO evalúa transición de estado de mesa.</li>
     * </ul>
     *
     * @param notificacionId identificador de la notificación CAMBIO
     * @param emailEmpleado  correo del empleado autenticado
     * @return DTO con el ID de la comanda a modificar
     * @throws ResourceNotFoundException si la notificación no existe
     * @throws BusinessException         si el tipo no es CAMBIO, ya está atendida o no tiene comanda
     */
    @Transactional
    public AtenderCambioResponse atenderCambio(Long notificacionId, String emailEmpleado) {

        // Localiza la notificación o lanza excepción si no existe
        Notificacion notificacion = notificacionRepository.findById(notificacionId)
                .orElseThrow(() -> new ResourceNotFoundException("Notificacion", notificacionId));

        // Solo notificaciones de cambio pueden activar este flujo
        if (notificacion.getNotificacionTipo() != TipoNotificacion.CAMBIO) {
            throw new BusinessException(ErrorCode.INVALID_STATE,
                    "La notificación no es de tipo CAMBIO.", HttpStatus.CONFLICT);
        }

        // Una notificación ATENDIDA no se puede volver a procesar
        if (notificacion.getNotificacionEstado() != EstadoNotificacion.ACTIVA) {
            throw new BusinessException(ErrorCode.INVALID_STATE,
                    "La notificación ya fue atendida.", HttpStatus.CONFLICT);
        }

        // Obtener la comanda asociada o lanzar excepción si no existe
        Comanda comanda = obtenerComandaObligatoria(notificacion);

        // Marcar la notificación como ATENDIDA para que no se procese de nuevo
        notificacion.setNotificacionEstado(EstadoNotificacion.ATENDIDA);
        notificacionRepository.save(notificacion);

        // Refresca el mapa de mesas de TODOS los meseros (eliminar ícono de cambio)
        mesaWsPublisher.publicarActualizacionMesa(
                notificacion.getMesa().getVisitaId(),
                MesaWsPublisher.TipoEventoMesa.NOTIFICACION);

        // No se publica evento a producción porque el mesero debe cargar la comanda en modo edición
        return AtenderCambioResponse.builder()
                .comandaId(comanda.getComandaId())
                .build();
    }
}
