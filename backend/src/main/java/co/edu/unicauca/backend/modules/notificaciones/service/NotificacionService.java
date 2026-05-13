package co.edu.unicauca.backend.modules.notificaciones.service;

import co.edu.unicauca.backend.modules.mesas_comandas.entity.Comanda;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.ComandaItem;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.ComandaMenuModificacion;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Mesa;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Visita;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ComandaItemRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ComandaRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.MesaRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.VisitaRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.service.DescripcionNormalizer;
import co.edu.unicauca.backend.modules.mesas_comandas.service.MesaAsignarService;
import co.edu.unicauca.backend.modules.notificaciones.dto.response.AtenderCambioResponse;
import co.edu.unicauca.backend.modules.notificaciones.dto.response.NotificacionAsistenciaResponse;
import co.edu.unicauca.backend.modules.notificaciones.dto.ws.AsistenciaAtendidaWsMessage;
import co.edu.unicauca.backend.modules.notificaciones.dto.ws.AsistenciaSolicitadaWsMessage;
import co.edu.unicauca.backend.modules.notificaciones.dto.ws.ComandaProduccionEventoWsMessage;
import co.edu.unicauca.backend.modules.notificaciones.dto.ws.TipoEventoProduccion;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private final ComandaItemRepository comandaItemRepository;
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

        // La validación previa de ownership garantiza que el cliente está presente.
        // Broadcast a todos los empleados conectados con los datos de la mesa
        wsPublisher.publicarAsistenciaSolicitada(AsistenciaSolicitadaWsMessage.builder()
                .visitaId(visitaId)
                .notificacionId(notificacion.getNotificacionId())
                .mesaIdentificador(mesa.getMesaIdentificador())
                .clienteNombre(visita.getCliente().getClienteNombre())
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

        // Notifica al tablero de producción de la estación para retirar la comanda de "Listas"
        wsPublisher.publicarEventoProduccion(
                comanda.getComandaEstacion(),
                new ComandaProduccionEventoWsMessage(
                        TipoEventoProduccion.COMPLETADA,
                        comanda.getComandaEstacion().name(),
                        comanda.getComandaId(),
                        null));

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

        // Notifica al tablero de producción de la estación para retirar la comanda de "Listas"
        wsPublisher.publicarEventoProduccion(
                comanda.getComandaEstacion(),
                new ComandaProduccionEventoWsMessage(
                        TipoEventoProduccion.COMPLETADA,
                        comanda.getComandaEstacion().name(),
                        comanda.getComandaId(),
                        null));

        Long visitaId = notificacion.getMesa().getVisitaId();

        // Refresca el mapa de mesas de TODOS los meseros (eliminar ícono de bebidas listas)
        mesaWsPublisher.publicarActualizacionMesa(visitaId, MesaWsPublisher.TipoEventoMesa.NOTIFICACION);

        // Re-evalúa si la mesa puede pasar a ATENDIDA
        mesaAsignarService.evaluarYActualizarEstadoMesa(visitaId);
    }

    /**
     * Atiende una notificación de cambio y reabsorbe la comanda asociada al
     * borrador activo de la misma visita y estación.
     *
     * <p>Cuando la comanda referenciada está en estado {@code PENDIENTE}:
     * <ul>
     *   <li>Si la visita no tiene un borrador para esa estación, la comanda
     *       se devuelve a {@code BORRADOR} (limpiando su timestamp de envío
     *       a producción) y la notificación queda {@code ATENDIDA}.</li>
     *   <li>Si la visita tiene un borrador, los ítems de la comanda
     *       {@code PENDIENTE} se fusionan dentro del borrador y la comanda
     *       {@code PENDIENTE} se elimina físicamente (en cascada también la
     *       notificación). La regla de fusión empareja ítems por
     *       {@code (productoId, descripción normalizada)}; los coincidentes
     *       suman cantidades y los no coincidentes se clonan en el borrador
     *       junto con sus modificaciones de menú especial.</li>
     * </ul>
     *
     * <p>En ambos casos se emite un evento {@code ELIMINADA} al tópico de la
     * estación para que el tablero retire la comanda de su columna.
     *
     * @param notificacionId identificador de la notificación CAMBIO
     * @param emailEmpleado  correo del empleado autenticado
     * @return DTO con el identificador de la comanda que el frontend debe
     *         cargar en modo edición (el borrador resultante)
     * @throws ResourceNotFoundException si la notificación no existe
     * @throws BusinessException         si el tipo no es CAMBIO, ya está
     *                                   atendida o no tiene comanda asociada
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
        Long visitaId = notificacion.getMesa().getVisitaId();
        Long comandaIdOriginal = comanda.getComandaId();
        Long resultadoComandaId = comandaIdOriginal;

        if (comanda.getComandaEstado() == EstadoComanda.PENDIENTE) {

            java.util.Optional<Comanda> borradorOpt = comandaRepository
                    .findBorradorActivoByVisitaYEstacion(visitaId, comanda.getComandaEstacion());

            if (borradorOpt.isPresent()) {
                // Caso fusión: los ítems de la pendiente migran al borrador y la pendiente desaparece
                Comanda borrador = borradorOpt.get();
                fusionarItemsEnBorrador(comanda, borrador);
                resultadoComandaId = borrador.getComandaId();

                // Se borra primero la notificación gestionada por la sesión Hibernate para evitar
                // dejar una entidad colgada cuando el cascade FK eliminaría su fila. Acto seguido
                // se borra la comanda; el cascade FK limpiará los ítems y sus modificaciones.
                notificacionRepository.delete(notificacion);
                comandaRepository.delete(comanda);
            } else {
                // Caso devolución directa: la pendiente vuelve a BORRADOR
                comanda.setComandaEstado(EstadoComanda.BORRADOR);
                comanda.setComandaFechaHoraInicio(null);
                comandaRepository.save(comanda);

                notificacion.setNotificacionEstado(EstadoNotificacion.ATENDIDA);
                notificacionRepository.save(notificacion);
            }

            // Aviso al tablero de producción para retirar la comanda pendiente
            wsPublisher.publicarEventoProduccion(
                    comanda.getComandaEstacion(),
                    new ComandaProduccionEventoWsMessage(
                            TipoEventoProduccion.ELIMINADA,
                            comanda.getComandaEstacion().name(),
                            comandaIdOriginal,
                            null));
        } else {
            // Flujo defensivo: si la comanda no está en PENDIENTE, la notificación se marca atendida
            notificacion.setNotificacionEstado(EstadoNotificacion.ATENDIDA);
            notificacionRepository.save(notificacion);
        }

        // Refresca el mapa de mesas de TODOS los meseros (eliminar ícono de cambio)
        mesaWsPublisher.publicarActualizacionMesa(visitaId, MesaWsPublisher.TipoEventoMesa.NOTIFICACION);

        return AtenderCambioResponse.builder()
                .comandaId(resultadoComandaId)
                .build();
    }

    /**
     * Migra los ítems de la comanda {@code PENDIENTE} dentro del borrador
     * activo de la misma visita y estación. Los ítems se emparejan por
     * producto y descripción normalizada; los coincidentes suman cantidades
     * y los no coincidentes se clonan junto con sus modificaciones.
     *
     * @param pendiente comanda en estado {@code PENDIENTE} cuyos ítems se
     *                  migran y que será eliminada físicamente después
     * @param borrador  comanda destino, ya bloqueada por la transacción
     */
    private void fusionarItemsEnBorrador(Comanda pendiente, Comanda borrador) {
        // Carga los ítems de ambas comandas con el producto para construir la clave de fusión
        List<ComandaItem> itemsPendiente = comandaItemRepository
                .findByComanda_ComandaIdOrderByProductoNombreAsc(pendiente.getComandaId());
        List<ComandaItem> itemsBorrador = comandaItemRepository
                .findByComanda_ComandaIdOrderByProductoNombreAsc(borrador.getComandaId());

        // Indexa el borrador por (productoId, descripción normalizada) para emparejamiento rápido
        Map<String, ComandaItem> indice = new HashMap<>();
        for (ComandaItem item : itemsBorrador) {
            indice.put(claveFusion(item), item);
        }

        // Itera los ítems de la pendiente y los fusiona en el borrador usando el índice
        for (ComandaItem item : itemsPendiente) {
            String clave = claveFusion(item);
            ComandaItem coincidente = indice.get(clave);
            if (coincidente != null) {
                // Coincidencia: las cantidades se acumulan sobre el ítem del borrador existente
                coincidente.setComandaItemCantidad(
                        coincidente.getComandaItemCantidad() + item.getComandaItemCantidad());
                comandaItemRepository.save(coincidente);
            } else {
                // Sin coincidencia: el ítem se clona dentro del borrador con todas sus modificaciones
                ComandaItem clon = clonarItem(item, borrador);
                ComandaItem persistido = comandaItemRepository.save(clon);
                // El clon recién persistido se registra en el índice para evitar duplicados intra-pendiente
                indice.put(clave, persistido);
            }
        }
    }

    /**
     * Construye la clave de fusión de un ítem combinando el identificador de
     * producto y la descripción normalizada
     */
    private String claveFusion(ComandaItem item) {
        return item.getProducto().getProductoId() + "|"
                + DescripcionNormalizer.normalizar(item.getComandaItemDescripcion());
    }

    /**
     * Crea una copia transitoria de un {@link ComandaItem} apuntando a una
     * comanda destino distinta. Las {@link ComandaMenuModificacion} asociadas
     * se replican como entidades nuevas vinculadas al clon.
     */
    private ComandaItem clonarItem(ComandaItem origen, Comanda destino) {
        // Copia campos de identidad y precio congelado; la nueva entidad apunta al borrador destino
        ComandaItem clon = ComandaItem.builder()
                .comanda(destino)
                .producto(origen.getProducto())
                .comandaItemCantidad(origen.getComandaItemCantidad())
                .comandaItemPrecio(origen.getComandaItemPrecio())
                .comandaItemDescripcion(origen.getComandaItemDescripcion())
                .comandaItemMenuGrupo(origen.getComandaItemMenuGrupo())
                .modificaciones(new ArrayList<>())
                .build();

        // Replica las modificaciones de menú especial como entidades nuevas vinculadas al clon
        if (origen.getModificaciones() != null) {
            for (ComandaMenuModificacion mod : origen.getModificaciones()) {
                clon.getModificaciones().add(ComandaMenuModificacion.builder()
                        .comandaItem(clon)
                        .opcion(mod.getOpcion())
                        .build());
            }
        }
        return clon;
    }
}
