package co.edu.unicauca.backend.modules.reservas.service;

import co.edu.unicauca.backend.modules.mesas_comandas.entity.ComandaItem;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ComandaItemRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ComandaRepository;
import co.edu.unicauca.backend.modules.pagos_caja.entity.Abono;
import co.edu.unicauca.backend.modules.pagos_caja.repository.AbonoRepository;
import co.edu.unicauca.backend.modules.reservas.dto.response.ListadoReservasResponse;
import co.edu.unicauca.backend.modules.reservas.dto.response.ReservaConsultaResponse;
import co.edu.unicauca.backend.modules.reservas.dto.response.ReservaDetalleResponse;
import co.edu.unicauca.backend.modules.reservas.dto.response.ResumenZonaResponse;
import co.edu.unicauca.backend.modules.reservas.entity.Reserva;
import co.edu.unicauca.backend.modules.reservas.mapper.ReservaConsultaMapper;
import co.edu.unicauca.backend.modules.reservas.mapper.ReservaMapper;
import co.edu.unicauca.backend.modules.reservas.repository.ReservaRepository;
import co.edu.unicauca.backend.shared.enums.EstadoComanda;
import co.edu.unicauca.backend.shared.enums.EstadoReserva;
import co.edu.unicauca.backend.shared.exception.BusinessException;
import co.edu.unicauca.backend.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Servicio de consulta de reservas para el módulo de meseros.
 *
 * <p>Provee operaciones de lectura para listar las reservas del día con resumen por zona
 * y obtener el detalle completo de una reserva incluyendo su pre-orden y abonos.
 *
 * <p>Todas las operaciones son de solo lectura ({@code @Transactional(readOnly = true)}).
 *
 * @see ReservaConsultaMapper
 * @see ReservaMapper
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReservaConsultaService {

    private final ReservaRepository reservaRepository;
    private final ComandaRepository comandaRepository;
    private final ComandaItemRepository comandaItemRepository;
    private final AbonoRepository abonoRepository;
    private final ReservaConsultaMapper reservaConsultaMapper;
    private final ReservaMapper reservaMapper;

    /**
     * Lista las reservas activas del día con resumen por zona.
     *
     * <p>Si no se especifica fecha, retorna las reservas del día actual. Si se proporciona
     * un identificador, ignora la fecha y busca solo por ID de reserva.
     *
     * <p>El resumen por zona agrupa las reservas activas por {@code zonaId} y muestra
     * la cantidad de reservas en cada zona (excluye reservas sin zona asignada).
     *
     * @param fecha         fecha a consultar; {@code null} para hoy
     * @param identificador ID de reserva a buscar; {@code null} para listar por fecha
     * @return {@link ListadoReservasResponse} con reservas y resumen por zona
     * @throws BusinessException si no hay reservas programadas para la fecha o identificador
     */
    public ListadoReservasResponse listarReservasDelDia(LocalDate fecha, Long identificador) {
        return listarReservasDelDia(fecha, identificador, false);
    }

    /**
     * Lista las reservas del día con resumen por zona, diferenciando la vista según el rol.
     *
     * <p>En la vista de mesero ({@code vistaCajero = false}) solo se consideran las reservas
     * activas ({@code PENDIENTE} o {@code CONFIRMADA}) y cada ítem incluye el indicador de
     * inasistencia. En la vista de cajero ({@code vistaCajero = true}) se incluyen las reservas
     * en cualquier estado y cada ítem expone el {@code tipo} y los botones de acción del cajero;
     * para el botón de devolución se resuelve en una sola consulta qué reservas tienen abono.</p>
     *
     * @param fecha         fecha a consultar; {@code null} para hoy
     * @param identificador ID de reserva a buscar; {@code null} para listar por fecha
     * @param vistaCajero   {@code true} para la vista de cajero (todos los estados); {@code false}
     *                      para la vista de mesero (solo activas)
     * @return {@link ListadoReservasResponse} con reservas y resumen por zona
     * @throws BusinessException si la fecha es pasada (400) o no hay reservas (404)
     */
    public ListadoReservasResponse listarReservasDelDia(LocalDate fecha, Long identificador, boolean vistaCajero) {
        // El cajero ve reservas en cualquier estado; el mesero solo las activas (PENDIENTE/CONFIRMADA)
        List<EstadoReserva> estados = vistaCajero
                ? List.of(EstadoReserva.values())
                : List.of(EstadoReserva.PENDIENTE, EstadoReserva.CONFIRMADA);

        List<Reserva> reservas;

        // Si se proporciona identificador, buscar por ID
        if (identificador != null) {
            reservas = reservaRepository.findReservasActivasPorIdentificador(identificador, estados);
        } else {
            if (fecha != null && fecha.isBefore(LocalDate.now())) {
                throw new BusinessException(
                        ErrorCode.BUSINESS_ERROR,
                        "No se pueden consultar reservas para fechas pasadas",
                        HttpStatus.BAD_REQUEST
                );
            }

            // Si no se especifica fecha, usar hoy
            LocalDate fechaConsulta = fecha != null ? fecha : LocalDate.now();
            LocalDateTime inicio = fechaConsulta.atTime(LocalTime.MIN);
            LocalDateTime fin = fechaConsulta.atTime(LocalTime.MAX);

            reservas = reservaRepository.findReservasActivasDelDia(inicio, fin, estados);
        }

        // Validar que se encontraron reservas
        if (reservas.isEmpty()) {
            throw new BusinessException(
                    ErrorCode.ENTITY_NOT_FOUND,
                    "No hay reservas para la fecha o identificador especificado",
                    HttpStatus.NOT_FOUND
            );
        }

        // Convertir reservas a DTOs según la vista
        List<ReservaConsultaResponse> reservasDto = vistaCajero
                ? mapearParaCajero(reservas)
                : reservas.stream()
                        .map(reservaConsultaMapper::toConsultaResponse)
                        .collect(Collectors.toList());

        // Calcular resumen por zona
        List<ResumenZonaResponse> resumen = calcularResumenPorZona(reservas);

        return ListadoReservasResponse.builder()
                .reservas(reservasDto)
                .resumenZonas(resumen)
                .build();
    }

    /**
     * Convierte las reservas a DTOs para la vista de cajero, resolviendo en una sola consulta
     * qué reservas tienen abono registrado (necesario para el botón de devolución).
     *
     * @param reservas reservas a convertir (no vacía)
     * @return lista de {@link ReservaConsultaResponse} con los botones de la vista de cajero
     */
    private List<ReservaConsultaResponse> mapearParaCajero(List<Reserva> reservas) {
        // Recopilar los IDs y consultar en lote qué reservas tienen al menos un abono
        List<Long> reservaIds = reservas.stream()
                .map(Reserva::getReservaId)
                .collect(Collectors.toList());
        Set<Long> reservasConAbono = new HashSet<>(abonoRepository.findReservaIdsConAbono(reservaIds));

        return reservas.stream()
                .map(r -> reservaConsultaMapper.toCajeroConsultaResponse(
                        r, reservasConAbono.contains(r.getReservaId())))
                .collect(Collectors.toList());
    }

    /**
     * Obtiene el detalle completo de una reserva, incluyendo pre-orden y abonos.
     *
     * <p>Si la reserva no existe, lanza {@link BusinessException} con código
     * {@code ENTITY_NOT_FOUND} y status {@code 404 NOT_FOUND}.
     *
     * <p>Los campos de pre-orden y abonos pueden ser {@code null} si la reserva
     * no tiene pre-orden o no se registraron anticipos.
     *
     * @param reservaId identificador de la reserva
     * @return {@link ReservaDetalleResponse} con datos completos de la reserva
     * @throws BusinessException si la reserva no existe
     */
    public ReservaDetalleResponse obtenerDetalleReserva(Long reservaId) {
        // Buscar reserva
        Reserva reserva = reservaRepository.findById(reservaId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.ENTITY_NOT_FOUND,
                        "Reserva no encontrada con ID: " + reservaId,
                        HttpStatus.NOT_FOUND
                ));

        // Obtener pre-orden (puede haber más de una comanda PRE_RESERVA tras el split por estación)
        List<ComandaItem> preOrden = comandaRepository
                .findByReserva_ReservaIdAndComandaEstado(reservaId, EstadoComanda.PRE_RESERVA)
                .stream()
                .flatMap(c -> comandaItemRepository.findByComanda_ComandaId(c.getComandaId()).stream())
                .toList();

        // Obtener abonos
        List<Abono> abonos = abonoRepository.findByReserva_ReservaIdOrderByAbonoFechaHoraAsc(reservaId);

        // Delegar construcción del DTO al mapper
        return reservaMapper.toDetalleResponse(reserva, preOrden, abonos);
    }

    /**
     * Calcula el resumen de reservas por zona.
     *
     * <p>Agrupa las reservas por {@code zonaNombre} y cuenta la cantidad en cada zona.
     * Las reservas sin zona asignada se agrupan bajo el nombre "Sin asignar".
     *
     * @param reservas lista de reservas a agrupar
     * @return lista de {@link ResumenZonaResponse} con cantidad por zona
     */
    private List<ResumenZonaResponse> calcularResumenPorZona(List<Reserva> reservas) {
        // Agrupar por zonaNombre (usar "Sin asignar" para reservas sin zona)
        Map<String, List<Reserva>> reservasPorZona = reservas.stream()
                .collect(Collectors.groupingBy(r ->
                        r.getZona() != null ? r.getZona().getZonaNombre() : "Sin asignar"
                ));

        // Construir DTOs de resumen
        return reservasPorZona.entrySet().stream()
                .map(entry -> {
                    String zonaNombre = entry.getKey();
                    List<Reserva> reservasZona = entry.getValue();

                    // Obtener zonaId (null para "Sin asignar")
                    Long zonaId = reservasZona.stream()
                            .filter(r -> r.getZona() != null)
                            .findFirst()
                            .map(r -> r.getZona().getZonaId())
                            .orElse(null);

                    Integer cantidadReservas = reservasZona.size();

                    return reservaConsultaMapper.toResumenZona(zonaId, zonaNombre, cantidadReservas);
                })
                .collect(Collectors.toList());
    }
}
