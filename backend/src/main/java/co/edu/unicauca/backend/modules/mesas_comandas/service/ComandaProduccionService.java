package co.edu.unicauca.backend.modules.mesas_comandas.service;

import co.edu.unicauca.backend.modules.inventario.service.InventarioDescuentoService;
import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.ComandaProduccionDetalleResponse;
import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.ComandaProduccionResumenResponse;
import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.ItemVisitaResponse;
import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.TableroProduccionResponse;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Comanda;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.ComandaItem;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Mesa;
import co.edu.unicauca.backend.modules.mesas_comandas.mapper.ComandaProduccionMapper;
import co.edu.unicauca.backend.modules.mesas_comandas.mapper.VisitaEstadoMapper;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ComandaItemRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ComandaRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.MesaRepository;
import co.edu.unicauca.backend.modules.notificaciones.dto.ws.ComandaProduccionEventoWsMessage;
import co.edu.unicauca.backend.modules.notificaciones.dto.ws.TipoEventoProduccion;
import co.edu.unicauca.backend.modules.notificaciones.dto.ws.VisitaActualizadaWsMessage;
import co.edu.unicauca.backend.modules.notificaciones.entity.Notificacion;
import co.edu.unicauca.backend.modules.notificaciones.repository.NotificacionRepository;
import co.edu.unicauca.backend.modules.notificaciones.service.MesaWsPublisher;
import co.edu.unicauca.backend.modules.notificaciones.service.NotificacionWsPublisher;
import co.edu.unicauca.backend.modules.usuarios.entity.Empleado;
import co.edu.unicauca.backend.modules.usuarios.repository.EmpleadoRepository;
import co.edu.unicauca.backend.shared.enums.EstacionComanda;
import co.edu.unicauca.backend.shared.enums.EstadoComanda;
import co.edu.unicauca.backend.shared.enums.EstadoNotificacion;
import co.edu.unicauca.backend.shared.enums.TipoNotificacion;
import co.edu.unicauca.backend.shared.exception.BusinessException;
import co.edu.unicauca.backend.shared.exception.ErrorCode;
import co.edu.unicauca.backend.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Servicio de consulta para las pantallas de producción de comandas. Provee
 * la vista del tablero agrupado por estado y la vista detallada de una
 * comanda individual.
 *
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ComandaProduccionService {

    /** Estados de comanda visibles en las pantallas de producción. */
    private static final Set<EstadoComanda> ESTADOS_VISIBLES =
            EnumSet.of(EstadoComanda.PENDIENTE, EstadoComanda.EN_PREPARACION, EstadoComanda.LISTO);

    private final ComandaRepository comandaRepository;
    private final ComandaItemRepository comandaItemRepository;
    private final MesaRepository mesaRepository;
    private final EstacionResolver estacionResolver;
    private final ComandaProduccionMapper comandaProduccionMapper;
    private final InventarioDescuentoService inventarioDescuentoService;
    private final NotificacionWsPublisher wsPublisher;
    private final EmpleadoRepository empleadoRepository;
    private final VisitaEstadoMapper visitaEstadoMapper;
    private final NotificacionRepository notificacionRepository;
    private final MesaWsPublisher mesaWsPublisher;

    /**
     * Construye el tablero de producción para el usuario autenticado.
     *
     * <p>Las comandas se recuperan por estación, se enriquecen con la mesa y
     * el total de ítems de forma agregada para evitar consultas adicionales
     * por comanda, y se distribuyen en tres listas según su estado actual.
     * Las listas se ordenan con un comparador que prioriza el timestamp de
     * transición correspondiente a la columna y, en su ausencia, recurre a
     * {@code createdAt} como respaldo.
     *
     * @param auth contexto de autenticación de Spring Security
     * @return tablero con las tres columnas pobladas
     * @throws BusinessException cuando el usuario carece de rol de producción
     */
    public TableroProduccionResponse obtenerTableroProduccion(Authentication auth) {
        // Resuelve las estaciones del usuario; lanza 403 si no tiene rol de producción
        Set<EstacionComanda> estaciones = estacionResolver.resolverEstaciones(auth);

        // Acumula las comandas de todas las estaciones del usuario en una sola lista
        List<Comanda> comandas = new ArrayList<>();
        for (EstacionComanda estacion : estaciones) {
            comandas.addAll(comandaRepository.findByEstacionAndEstadoIn(estacion, ESTADOS_VISIBLES));
        }

        // Pre-carga en lote la mesa y los totales de ítems 
        Map<Long, Mesa> mesasPorVisita = cargarMesas(comandas);
        Map<Long, Integer> totalesPorComanda = cargarTotalesItems(comandas);

        // Distribuye las comandas en las tres columnas del tablero según su estado actual
        List<ComandaProduccionResumenResponse> pendientes = new ArrayList<>();
        List<ComandaProduccionResumenResponse> enPreparacion = new ArrayList<>();
        List<ComandaProduccionResumenResponse> listos = new ArrayList<>();

        for (Comanda c : comandas) {
            ComandaProduccionResumenResponse resumen = comandaProduccionMapper.toResumen(
                    c,
                    mesasPorVisita.get(c.getVisita().getVisitaId()),
                    totalesPorComanda.getOrDefault(c.getComandaId(), 0));
            switch (c.getComandaEstado()) {
                case PENDIENTE -> pendientes.add(resumen);
                case EN_PREPARACION -> enPreparacion.add(resumen);
                case LISTO -> listos.add(resumen);
                default -> { /* estados no visibles se descartan */ }
            }
        }

        // Ordena cada columna por su timestamp de referencia con respaldo en createdAt
        pendientes.sort(comparadorPorFecha(ComandaProduccionResumenResponse::getFechaHoraInicio));
        enPreparacion.sort(comparadorPorFecha(ComandaProduccionResumenResponse::getFechaHoraInicio));
        listos.sort(comparadorPorFecha(ComandaProduccionResumenResponse::getFechaHoraListo));

        // Las estaciones se ordenan alfabéticamente para mantener un contrato estable de respuesta
        return TableroProduccionResponse.builder()
                .estaciones(estaciones.stream()
                        .map(Enum::name)
                        .sorted()
                        .collect(Collectors.toList()))
                .pendientes(pendientes)
                .enPreparacion(enPreparacion)
                .listos(listos)
                .build();
    }

    /**
     * Recupera el detalle completo de una comanda para el usuario autenticado.
     *
     * <p>El método valida secuencialmente que la comanda exista, que su
     * estación pertenezca a las del solicitante y que su estado sea visible
     * en producción.
     *
     * @param comandaId identificador de la comanda
     * @param auth      contexto de autenticación de Spring Security
     * @return detalle de la comanda con ítems agrupados por categoría
     * @throws BusinessException con {@code AUTH-002} si el usuario no tiene
     *                           acceso a la estación de la comanda, o con
     *                           {@code ENT-001} si la comanda no existe o
     *                           no está en un estado visible
     */
    public ComandaProduccionDetalleResponse obtenerDetalleComanda(Long comandaId, Authentication auth) {
        // Resuelve las estaciones del usuario antes de buscar la comanda para no revelar su existencia
        Set<EstacionComanda> estaciones = estacionResolver.resolverEstaciones(auth);

        // 404 cuando la comanda no existe
        Comanda comanda = comandaRepository.findById(comandaId)
                .orElseThrow(() -> notFound(comandaId));

        // 403 cuando la comanda existe pero pertenece a una estación distinta a las del usuario
        if (!estaciones.contains(comanda.getComandaEstacion())) {
            throw new BusinessException(
                    ErrorCode.ACCESS_DENIED,
                    "La comanda no pertenece a una estación accesible para el usuario.",
                    HttpStatus.FORBIDDEN);
        }

        // 404 cuando la comanda está en un estado no visible en producción (BORRADOR, COMPLETADO, etc.)
        if (!ESTADOS_VISIBLES.contains(comanda.getComandaEstado())) {
            throw notFound(comandaId);
        }

        // La mesa puede no estar asignada en momentos de transición; el mapper tolera mesa nula
        Mesa mesa = mesaRepository.findByVisita_VisitaId(comanda.getVisita().getVisitaId())
                .orElse(null);

        // Los ítems se cargan en orden alfabético por nombre de producto para mostrar al detalle
        List<ComandaItem> items = comandaItemRepository
                .findByComanda_ComandaIdOrderByProductoNombreAsc(comandaId);

        return comandaProduccionMapper.toDetalle(comanda, mesa, items);
    }

    /**
     * Transiciona una comanda en estado {@code PENDIENTE} a {@code EN_PREPARACION}.
     *
     * <p>Antes del cambio de estado descuenta el inventario consumido por todos
     * los ítems no pertenecientes a un menú especial. Si el descuento falla la
     * transacción revierte y la comanda permanece en {@code PENDIENTE}.
     *
     * @param comandaId identificador de la comanda
     * @param auth      contexto de autenticación; debe pertenecer a la estación
     *                  de la comanda
     * @return resumen actualizado de la comanda
     * @throws ResourceNotFoundException si la comanda o el empleado no existen
     * @throws BusinessException con {@link ErrorCode#INVALID_STATE} si la comanda
     *                           no está en {@code PENDIENTE}, o con
     *                           {@link ErrorCode#ACCESS_DENIED} si la estación no
     *                           es accesible para el usuario
     */
    @Transactional
    public ComandaProduccionResumenResponse iniciarPreparacion(Long comandaId, Authentication auth) {
        // Resuelve estaciones antes de cargar la comanda para evitar revelar su existencia
        Set<EstacionComanda> estaciones = estacionResolver.resolverEstaciones(auth);

        Comanda comanda = comandaRepository.findById(comandaId)
                .orElseThrow(() -> new ResourceNotFoundException("Comanda", comandaId));

        // Sólo comandas en PENDIENTE pueden transicionar a EN_PREPARACION
        if (comanda.getComandaEstado() != EstadoComanda.PENDIENTE) {
            throw new BusinessException(ErrorCode.INVALID_STATE,
                    "Solo se pueden iniciar comandas en estado PENDIENTE.", HttpStatus.CONFLICT);
        }

        // El usuario debe operar sobre una estación que le pertenezca
        if (!estaciones.contains(comanda.getComandaEstacion())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED,
                    "La comanda no pertenece a una estación accesible para el usuario.",
                    HttpStatus.FORBIDDEN);
        }

        Empleado actor = empleadoRepository.findByUsuario_UsuarioEmail(auth.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Empleado", auth.getName()));

        // El descuento se realiza antes del cambio de estado; si falla la tx revierte
        inventarioDescuentoService.descontarPorComanda(comanda, actor);

        comanda.setComandaEstado(EstadoComanda.EN_PREPARACION);
        comanda.setComandaFechaHoraInicio(LocalDateTime.now());
        comandaRepository.save(comanda);

        Long visitaId = comanda.getVisita().getVisitaId();
        Mesa mesa = mesaRepository.findByVisita_VisitaId(visitaId).orElse(null);
        int totalItems = comandaItemRepository.sumCantidadByComandaIdIn(Set.of(comandaId)).stream()
                .findFirst()
                .map(r -> ((Number) r[1]).intValue())
                .orElse(0);
        ComandaProduccionResumenResponse resumen = comandaProduccionMapper.toResumen(comanda, mesa, totalItems);

        // Notifica al tablero de producción de la estación
        wsPublisher.publicarEventoProduccion(
                comanda.getComandaEstacion(),
                new ComandaProduccionEventoWsMessage(
                        TipoEventoProduccion.ACTUALIZADA,
                        comanda.getComandaEstacion().name(),
                        comandaId,
                        null,
                        EstadoComanda.EN_PREPARACION.name()));

        // Notifica al cliente que el estado de su visita cambió
        publicarVisitaActualizadaCliente(visitaId);

        return resumen;
    }

    /**
     * Transiciona una comanda en estado {@code EN_PREPARACION} a {@code LISTO},
     * crea la notificación correspondiente al mesero según la estación
     * ({@code PLATOS_LISTOS} para cocina, {@code BEBIDAS_LISTAS} para barra) y
     * propaga el cambio por los tópicos de producción y de la visita, además de
     * refrescar el mapa de mesas.
     *
     * @param comandaId identificador de la comanda
     * @param auth      contexto de autenticación; debe pertenecer a la estación
     *                  de la comanda
     * @return resumen actualizado de la comanda
     * @throws ResourceNotFoundException si la comanda no existe
     * @throws BusinessException con {@code INVALID_STATE} si la comanda no está en {@code EN_PREPARACION},
     *                           con {@code ACCESS_DENIED} si la estación no es accesible para el usuario
     */
    @Transactional
    public ComandaProduccionResumenResponse marcarListo(Long comandaId, Authentication auth) {
        Set<EstacionComanda> estaciones = estacionResolver.resolverEstaciones(auth);

        Comanda comanda = comandaRepository.findById(comandaId)
                .orElseThrow(() -> new ResourceNotFoundException("Comanda", comandaId));

        // Sólo comandas en EN_PREPARACION pueden transicionar a LISTO
        if (comanda.getComandaEstado() != EstadoComanda.EN_PREPARACION) {
            throw new BusinessException(ErrorCode.INVALID_STATE,
                    "Solo se pueden marcar listas comandas en EN_PREPARACION.", HttpStatus.CONFLICT);
        }

        // El usuario debe operar sobre una estación que le pertenezca
        if (!estaciones.contains(comanda.getComandaEstacion())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED,
                    "La comanda no pertenece a una estación accesible para el usuario.", HttpStatus.FORBIDDEN);
        }

        comanda.setComandaEstado(EstadoComanda.LISTO);
        comanda.setComandaFechaHoraListo(LocalDateTime.now());
        comandaRepository.save(comanda);

        Long visitaId = comanda.getVisita().getVisitaId();
        Mesa mesa = mesaRepository.findByVisita_VisitaId(visitaId).orElse(null);

        // PLATOS_LISTOS para cocina, BEBIDAS_LISTAS para barra
        TipoNotificacion tipo = comanda.getComandaEstacion() == EstacionComanda.COCINA
                ? TipoNotificacion.PLATOS_LISTOS
                : TipoNotificacion.BEBIDAS_LISTAS;

        notificacionRepository.save(Notificacion.builder()
                .comanda(comanda)
                .mesa(mesa)
                .empleado(mesa != null ? mesa.getMesero() : null)
                .notificacionTipo(tipo)
                .notificacionEstado(EstadoNotificacion.ACTIVA)
                .build());

        int totalItems = comandaItemRepository.sumCantidadByComandaIdIn(Set.of(comandaId)).stream()
                .findFirst()
                .map(r -> ((Number) r[1]).intValue())
                .orElse(0);
        ComandaProduccionResumenResponse resumen = comandaProduccionMapper.toResumen(comanda, mesa, totalItems);

        // Notifica al tablero de producción de la estación
        wsPublisher.publicarEventoProduccion(comanda.getComandaEstacion(),
                new ComandaProduccionEventoWsMessage(
                        TipoEventoProduccion.ACTUALIZADA,
                        comanda.getComandaEstacion().name(),
                        comandaId,
                        null,
                        EstadoComanda.LISTO.name()));

        // Notifica al cliente que el estado de su visita cambió
        publicarVisitaActualizadaCliente(visitaId);

        // Refresca el mapa de mesas con evento de notificación
        mesaWsPublisher.publicarActualizacionMesa(visitaId, MesaWsPublisher.TipoEventoMesa.NOTIFICACION);

        return resumen;
    }

    /**
     * Construye y publica el mensaje WebSocket dirigido al cliente con el
     * estado completo de los ítems activos y el total acumulado de la visita.
     * Centraliza el patrón usado por todas las transiciones que mutan
     * comandas para mantener un único origen de verdad.
     */
    private void publicarVisitaActualizadaCliente(Long visitaId) {
        List<ComandaItem> items = comandaRepository.findAllItemsActivosByVisita(visitaId);
        List<ItemVisitaResponse> itemsResponse = visitaEstadoMapper.toItemsVisitaResponse(items);
        BigDecimal total = items.stream()
                .filter(ci -> ci.getComandaItemPrecio() != null)
                .map(ci -> ci.getComandaItemPrecio().multiply(BigDecimal.valueOf(ci.getComandaItemCantidad())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        wsPublisher.publicarVisitaActualizada(visitaId,
                VisitaActualizadaWsMessage.builder()
                        .visitaId(visitaId)
                        .items(itemsResponse)
                        .total(total)
                        .build());
    }

    private Map<Long, Mesa> cargarMesas(List<Comanda> comandas) {
        // Extrae los identificadores de visita únicos para una consulta batch
        Set<Long> visitaIds = comandas.stream()
                .map(c -> c.getVisita().getVisitaId())
                .collect(Collectors.toSet());

        // Si no hay comandas evita el viaje a la base de datos
        if (visitaIds.isEmpty()) {
            return Collections.emptyMap();
        }

        // El identificador de visita coincide con la PK de Mesa (@MapsId)
        return mesaRepository.findByVisita_VisitaIdIn(visitaIds).stream()
                .collect(Collectors.toMap(Mesa::getVisitaId, Function.identity()));
    }

    private Map<Long, Integer> cargarTotalesItems(List<Comanda> comandas) {
        // Extrae los identificadores de comanda para la consulta agregada
        Set<Long> comandaIds = comandas.stream()
                .map(Comanda::getComandaId)
                .collect(Collectors.toSet());
        if (comandaIds.isEmpty()) {
            return Collections.emptyMap();
        }

        // Convierte las tuplas [comandaId, totalCantidad] en un mapa de acceso directo
        return comandaItemRepository.sumCantidadByComandaIdIn(comandaIds).stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row[0]).longValue(),
                        row -> ((Number) row[1]).intValue()));
    }

    private Comparator<ComandaProduccionResumenResponse> comparadorPorFecha(
            Function<ComandaProduccionResumenResponse, LocalDateTime> extractor) {
        // Las comandas sin el timestamp principal se ubican al final de la columna
        Comparator<LocalDateTime> nullsLast = Comparator.nullsLast(Comparator.naturalOrder());
        
        // El segundo criterio es createdAt para preservar un orden estable entre invocaciones
        return Comparator.comparing(extractor, nullsLast)
                .thenComparing(ComandaProduccionResumenResponse::getCreatedAt, nullsLast);
    }

    private BusinessException notFound(Long comandaId) {
        return new BusinessException(
                ErrorCode.ENTITY_NOT_FOUND,
                "No existe una comanda visible con identificador " + comandaId + ".",
                HttpStatus.NOT_FOUND);
    }
}
