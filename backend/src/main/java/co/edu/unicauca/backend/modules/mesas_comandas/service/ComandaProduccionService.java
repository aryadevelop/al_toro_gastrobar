package co.edu.unicauca.backend.modules.mesas_comandas.service;

import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.ComandaProduccionDetalleResponse;
import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.ComandaProduccionResumenResponse;
import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.TableroProduccionResponse;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Comanda;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.ComandaItem;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Mesa;
import co.edu.unicauca.backend.modules.mesas_comandas.mapper.ComandaProduccionMapper;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ComandaItemRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ComandaRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.MesaRepository;
import co.edu.unicauca.backend.shared.enums.EstacionComanda;
import co.edu.unicauca.backend.shared.enums.EstadoComanda;
import co.edu.unicauca.backend.shared.exception.BusinessException;
import co.edu.unicauca.backend.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
