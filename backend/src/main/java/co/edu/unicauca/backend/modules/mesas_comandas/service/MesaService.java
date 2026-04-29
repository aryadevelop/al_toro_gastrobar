package co.edu.unicauca.backend.modules.mesas_comandas.service;

import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.*;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.ComandaItem;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Mesa;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Zona;
import co.edu.unicauca.backend.modules.mesas_comandas.mapper.MesaMapper;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ComandaRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.MesaRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ZonaRepository;
import co.edu.unicauca.backend.modules.notificaciones.entity.Notificacion;
import co.edu.unicauca.backend.modules.notificaciones.repository.NotificacionRepository;
import co.edu.unicauca.backend.shared.exception.BusinessException;
import co.edu.unicauca.backend.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Servicio para gestión del mapa de mesas.
 *
 * <p>Proporciona operaciones de consulta para el mapa de mesas del restaurante,
 * incluyendo filtrado por zona, visualización de estado en tiempo real,
 * y detalle de mesas individuales.
 *
 * <p><b>NOTA:</b> Este servicio es de solo lectura (solo GET).
 * Los eventos WebSocket para mesas se publican desde otros servicios que
 * modifican el estado (creación de visita, cambio de estado, etc.).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MesaService {

    private final MesaRepository mesaRepository;
    private final ZonaRepository zonaRepository;
    private final ComandaRepository comandaRepository;
    private final NotificacionRepository notificacionRepository;
    private final MesaMapper mesaMapper;

    // NO inyectar MesaWsPublisher - servicio de consulta

    /**
     * Obtiene el mapa completo de mesas, opcionalmente filtrado por zona.
     *
     * <p>Flujo:
     * <ol>
     *   <li>Obtener zonas (todas o solo la especificada según RN-07)</li>
     *   <li>Obtener mesas activas (filtradas por zona si aplica según RN-01)</li>
     *   <li>Agrupar mesas por zona</li>
     *   <li>Para cada mesa: obtener notificaciones activas y flag de borrador</li>
     *   <li>Delegar mapeo al mapper</li>
     * </ol>
     *
     * <p>Incluye todas las zonas del restaurante, mostrando para cada una:
     * - ID, nombre y cantidad de mesas activas
     * - Lista de mesas con estado, mesero, notificaciones y flag de borrador
     *
     * <p>Si zonaId es null, devuelve todas las zonas (incluso sin mesas).
     * Si zonaId es especificado, devuelve solo esa zona.
     *
     * @param zonaId ID de zona (null = todas las zonas)
     * @param emailMesero email del mesero que hace la petición (para flag esMesaPropia)
     * @return MapaMesasResponse con zonas y mesas
     * @throws BusinessException con ErrorCode.ENTITY_NOT_FOUND si zonaId no existe
     */
    public MapaMesasResponse obtenerMapaMesas(Long zonaId, String emailMesero) {

        // 1. Obtener zonas (RN-07)
        List<Zona> zonas = zonaId != null
                ? zonaRepository.findById(zonaId)
                    .map(List::of)
                    .orElseThrow(() -> new BusinessException(
                            ErrorCode.ENTITY_NOT_FOUND,
                            "Zona no encontrada",
                            HttpStatus.NOT_FOUND))
                : zonaRepository.findAll();

        // 2. Obtener mesas activas (RN-01)
        List<Mesa> mesasActivas = zonaId != null
                ? mesaRepository.findMesasActivasByZona(zonaId)
                : mesaRepository.findAllMesasActivas();

        // 3. Agrupar mesas por zona
        Map<Long, List<Mesa>> mesasPorZona = mesasActivas.stream()
                .collect(Collectors.groupingBy(mesa -> mesa.getZona().getZonaId()));

        // 4. Construir respuesta
        List<ZonaMesasResponse> zonasResponse = zonas.stream()
                .map(zona -> {
                    List<Mesa> mesasZona = mesasPorZona.getOrDefault(zona.getZonaId(), List.of());
                    List<MesaMapaResponse> mesasDto = mapearMesas(mesasZona, emailMesero);

                    return ZonaMesasResponse.builder()
                            .zonaId(zona.getZonaId())
                            .zonaNombre(zona.getZonaNombre())
                            .cantidadMesasActivas(mesasZona.size())
                            .mesas(mesasDto)
                            .build();
                })
                .collect(Collectors.toList());

        return MapaMesasResponse.builder()
                .zonas(zonasResponse)
                .build();
    }

    /**
     * Obtiene el detalle completo de una mesa.
     *
     * <p>Flujo:
     * <ol>
     *   <li>Buscar mesa por ID</li>
     *   <li>Obtener items en producción</li>
     *   <li>Agrupar items según RN-06</li>
     *   <li>Delegar mapeo al mapper</li>
     * </ol>
     *
     * <p>Incluye:
     * - Identificador de mesa, nombre del cliente (si existe), hora de llegada
     * - Número de personas, estado de la mesa, notas de reserva
     * - Items de comandas en producción (PENDIENTE, EN_PREPARACION, LISTO, COMPLETADO)
     *   agrupados por nombre y descripción según RN-06
     *
     * @param visitaId ID de la visita (PK de Mesa)
     * @return MesaDetalleResponse con información completa
     * @throws BusinessException con ErrorCode.ENTITY_NOT_FOUND si la mesa no existe
     */
    public MesaDetalleResponse obtenerDetalleMesa(Long visitaId) {

        // 1. Obtener mesa
        Mesa mesa = mesaRepository.findById(visitaId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.ENTITY_NOT_FOUND,
                        "Mesa no encontrada",
                        HttpStatus.NOT_FOUND));

        // 2. Obtener items en producción (RN-05)
        List<ComandaItem> items = comandaRepository.findItemsEnProduccionByVisita(visitaId);

        // 3. Agrupar items (RN-06)
        List<ItemComandaEnProduccionResponse> itemsAgrupados =
            mesaMapper.agruparItemsEnProduccion(items);

        // 4. Mapear a DTO
        return mesaMapper.toMesaDetalleResponse(mesa, itemsAgrupados);
    }

    /**
     * Obtiene información de items en producción de una mesa.
     *
     * <p>Flujo:
     * <ol>
     *   <li>Buscar mesa por ID</li>
     *   <li>Obtener items en producción</li>
     *   <li>Agrupar items según RN-06</li>
     *   <li>Delegar mapeo al mapper</li>
     * </ol>
     *
     * <p>Devuelve:
     * - Identificador de la mesa
     * - Resumen de items enviados a producción (no modificables)
     *
     * <p>Solo items en estados PENDIENTE, EN_PREPARACION, LISTO, COMPLETADO.
     *
     * @param visitaId ID de la visita (PK de Mesa)
     * @return MesaItemsProduccionResponse con identificador y items en producción
     * @throws BusinessException con ErrorCode.ENTITY_NOT_FOUND si la mesa no existe
     */
    public MesaItemsProduccionResponse obtenerItemsProduccion(Long visitaId) {

        // 1. Obtener mesa
        Mesa mesa = mesaRepository.findById(visitaId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.ENTITY_NOT_FOUND,
                        "Mesa no encontrada",
                        HttpStatus.NOT_FOUND));

        // 2. Obtener items en producción (RN-05)
        List<ComandaItem> items = comandaRepository.findItemsEnProduccionByVisita(visitaId);

        // 3. Agrupar items (RN-06)
        List<ItemComandaEnProduccionResponse> itemsAgrupados =
            mesaMapper.agruparItemsEnProduccion(items);

        // 4. Mapear a DTO
        return mesaMapper.toMesaItemsProduccionResponse(
                mesa.getMesaIdentificador(),
                itemsAgrupados
        );
    }

    /**
     * Mapea lista de mesas a DTOs, obteniendo notificaciones y flag de borrador.
     * Método privado auxiliar para obtenerMapaMesas.
     */
    private List<MesaMapaResponse> mapearMesas(List<Mesa> mesas, String emailMesero) {
        return mesas.stream()
                .map(mesa -> {
                    Long visitaId = mesa.getVisitaId();

                    // Obtener notificaciones activas (RN-02)
                    List<Notificacion> notificaciones =
                        notificacionRepository.findNotificacionesActivasByMesa(visitaId);

                    // Verificar si tiene comanda en borrador (RN-03)
                    boolean tieneBorrador = mesaRepository.existeComandaBorradorEnMesa(visitaId);

                    // Mapear a DTO (RN-04: nombreMesero condicional)
                    return mesaMapper.toMesaMapaResponse(mesa, notificaciones,
                                                          tieneBorrador, emailMesero);
                })
                .collect(Collectors.toList());
    }
}
