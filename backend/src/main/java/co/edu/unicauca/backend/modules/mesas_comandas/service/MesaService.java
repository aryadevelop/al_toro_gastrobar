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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
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
    private final MesaValidador mesaValidador;
    
    /**
     * Obtiene el mapa completo de mesas, opcionalmente filtrado por zona.
     *
     * <p>Si zonaId es null, devuelve todas las zonas (incluso sin mesas).
     * Si zonaId es especificado, devuelve solo esa zona.
     *
     * @param zonaId ID de zona (null = todas las zonas)
     * @param authentication contexto de seguridad del request (para email y detección de rol CAJERO)
     * @return MapaMesasResponse con zonas y mesas
     * @throws BusinessException con ErrorCode.ENTITY_NOT_FOUND si zonaId no existe
     */
    public MapaMesasResponse obtenerMapaMesas(Long zonaId, Authentication authentication) {
        String emailMesero = authentication.getName();
        boolean esCajero = esCajero(authentication);

        // 1. Obtener zonas
        List<Zona> zonas = zonaId != null
                ? zonaRepository.findById(zonaId)
                    .map(List::of)
                    .orElseThrow(() -> new BusinessException(
                            ErrorCode.ENTITY_NOT_FOUND,
                            "Zona no encontrada",
                            HttpStatus.NOT_FOUND))
                : zonaRepository.findAll();

        // 2. Obtener mesas activas
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
                    List<MesaMapaResponse> mesasDto = mapearMesas(mesasZona, emailMesero, esCajero);

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
     * <p>Incluye información de la mesa, visita, mesero, zona, y items en producción.
     * 
     * @param visitaId ID de la visita (PK de Mesa)
     * @param authentication contexto de seguridad (para detección de rol CAJERO y campos extra del DTO)
     * @return MesaDetalleResponse con información completa
     * @throws BusinessException con ErrorCode.ENTITY_NOT_FOUND si la mesa no existe o está cerrada
     */
    public MesaDetalleResponse obtenerDetalleMesa(Long visitaId, Authentication authentication) {
        boolean esCajero = esCajero(authentication);

        // 1. Obtener mesa
        Mesa mesa = mesaRepository.findById(visitaId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.ENTITY_NOT_FOUND,
                        "Mesa no encontrada",
                        HttpStatus.NOT_FOUND));

        // 2. Validar que la visita esté activa
        if (mesa.getVisita().getVisitaFechaHoraFin() != null) {
            throw new BusinessException(
                    ErrorCode.ENTITY_NOT_FOUND,
                    "La visita ya está cerrada",
                    HttpStatus.NOT_FOUND);
        }

        // 3. Obtener items en producción
        List<ComandaItem> items = comandaRepository.findItemsEnProduccionByVisita(visitaId);

        // 4. Agrupar items
        List<ItemComandaEnProduccionResponse> itemsAgrupados =
            mesaMapper.agruparItemsEnProduccion(items);

        // 5. Mapear a DTO (pasando items originales para extraer notas)
        return mesaMapper.toMesaDetalleResponse(mesa, items, itemsAgrupados, esCajero);
    }

    /**
     * Obtiene información de items en producción de una mesa.
     *
     * <p>Solo items en estados PENDIENTE, EN_PREPARACION, LISTO, COMPLETADO.
     *
     * <p>Solo el mesero asignado a la mesa puede acceder. El rol ADMIN tiene acceso sin restricciones.
     *
     * @param visitaId ID de la visita (PK de Mesa)
     * @param authentication autenticación del usuario (para validar rol y email)
     * @return MesaItemsProduccionResponse con identificador y items en producción
     * @throws BusinessException con ErrorCode.ENTITY_NOT_FOUND si la mesa no existe o está cerrada
     * @throws BusinessException con ErrorCode.ACCESS_DENIED si el mesero no es el asignado (ADMIN siempre tiene acceso)
     */
    public MesaItemsProduccionResponse obtenerItemsProduccion(Long visitaId, Authentication authentication) {

        Mesa mesa = mesaValidador.validarOwnership(visitaId, authentication);

        // Obtener items en producción
        List<ComandaItem> items = comandaRepository.findItemsEnProduccionByVisita(visitaId);

        // Agrupar items
        List<ItemComandaEnProduccionResponse> itemsAgrupados =
            mesaMapper.agruparItemsEnProduccion(items);

        // Mapear a DTO
        return mesaMapper.toMesaItemsProduccionResponse(
                mesa.getMesaIdentificador(),
                itemsAgrupados
        );
    }

    /**
     * Mapea lista de mesas a DTOs, obteniendo notificaciones y flag de borrador.
     * Método privado auxiliar para obtenerMapaMesas.
     */
    private List<MesaMapaResponse> mapearMesas(List<Mesa> mesas, String emailMesero, boolean esCajero) {
        return mesas.stream()
                .map(mesa -> {
                    Long visitaId = mesa.getVisitaId();

                    // Obtener notificaciones activas
                    List<Notificacion> notificaciones =
                        notificacionRepository.findNotificacionesActivasByMesa(visitaId);

                    // Verificar si tiene comanda en borrador
                    boolean tieneBorrador = mesaRepository.existeComandaBorradorEnMesa(visitaId);

                    // Mapear a DTO
                    return mesaMapper.toMesaMapaResponse(mesa, notificaciones, tieneBorrador, emailMesero, esCajero);
                })
                .collect(Collectors.toList());
    }

    /**
     * Indica si el principal autenticado tiene rol CAJERO.
     *
     * @param authentication contexto de seguridad del request
     * @return true si entre las authorities aparece ROLE_CAJERO
     */
    private boolean esCajero(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_CAJERO"::equals);
    }
}
