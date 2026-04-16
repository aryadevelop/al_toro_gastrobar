package co.edu.unicauca.backend.modules.reservas.service;

import co.edu.unicauca.backend.modules.mesas_comandas.entity.Zona;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ZonaRepository;
import co.edu.unicauca.backend.modules.reservas.dto.request.CrearReservaRequest;
import co.edu.unicauca.backend.modules.reservas.dto.response.DecoracionDisponibleResponse;
import co.edu.unicauca.backend.modules.reservas.dto.response.DisponibilidadResponse;
import co.edu.unicauca.backend.modules.reservas.dto.response.ReservaResponse;
import co.edu.unicauca.backend.modules.reservas.dto.response.ZonaDisponibleResponse;
import co.edu.unicauca.backend.modules.reservas.entity.Decoracion;
import co.edu.unicauca.backend.modules.reservas.entity.DecoracionZona;
import co.edu.unicauca.backend.modules.reservas.entity.PreOrdenDetalle;
import co.edu.unicauca.backend.modules.reservas.entity.Reserva;
import co.edu.unicauca.backend.modules.reservas.mapper.ReservaMapper;
import co.edu.unicauca.backend.modules.reservas.repository.BloqueDisponibilidadRepository;
import co.edu.unicauca.backend.modules.reservas.repository.DecoracionRepository;
import co.edu.unicauca.backend.modules.reservas.repository.DecoracionZonaRepository;
import co.edu.unicauca.backend.modules.reservas.repository.ReservaRepository;
import co.edu.unicauca.backend.modules.usuarios.entity.Cliente;
import co.edu.unicauca.backend.modules.usuarios.repository.ClienteRepository;
import co.edu.unicauca.backend.shared.enums.EstadoGenerico;
import co.edu.unicauca.backend.shared.enums.EstadoReserva;
import co.edu.unicauca.backend.shared.enums.TipoReserva;
import co.edu.unicauca.backend.shared.exception.BusinessException;
import co.edu.unicauca.backend.shared.exception.ErrorCode;
import co.edu.unicauca.backend.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Servicio de negocio para la gestión de reservas del restaurante.
 *
 * <p>Centraliza la lógica de creación, consulta y validación de reservas,
 * incluyendo el cálculo de disponibilidad de zonas y decoraciones y la gestión
 * de bloqueos administrativos. La lógica de pre-órdenes se delega en
 * {@link PreOrdenService} y el mapeo a DTOs en {@link ReservaMapper}.
 *
 * <p>Responsabilidades principales:
 * <ul>
 *   <li>Consultar disponibilidad para una fecha y hora dada.</li>
 *   <li>Crear una reserva validando horario, bloqueos, capacidad y compatibilidad
 *       decoración-zona.</li>
 *   <li>Exponer el historial de reservas por cliente o de todo el sistema.</li>
 * </ul>
 *
 * @see PreOrdenService
 * @see ReservaMapper
 * @see co.edu.unicauca.backend.modules.reservas.entity.Reserva
 */
@Service
@RequiredArgsConstructor
public class ReservaService {

    /** Estados de reserva considerados como ocupación activa para el cálculo de disponibilidad. */
    private static final List<EstadoReserva> ESTADOS_ACTIVOS = List.of(EstadoReserva.PENDIENTE, EstadoReserva.CONFIRMADA);

    /** Mensaje enviado al cliente cuando la disponibilidad cambió entre la consulta y la creación. */
    private static final String MSG_DISPONIBILIDAD_CAMBIO = "Lo sentimos, la disponibilidad cambió. Por favor revise nuevamente.";

    /** Mensaje enviado al cliente cuando la hora solicitada está fuera del horario de atención. */
    private static final String MSG_FUERA_HORARIO =
            "Lo sentimos, no hay disponibilidad para la fecha y hora seleccionada. " +
            "Por favor elija otra fecha u hora.";

    /** Hora de apertura del restaurante (inclusiva): {@code 17 = 5:00 PM}. */
    private static final int HORA_APERTURA = 17;

    /** Hora de cierre del restaurante (exclusiva): {@code 22 = 10:00 PM}. */
    private static final int HORA_CIERRE = 22;

    private final ReservaRepository reservaRepository;
    private final DecoracionRepository decoracionRepository;
    private final DecoracionZonaRepository decoracionZonaRepository;
    private final ZonaRepository zonaRepository;
    private final ClienteRepository clienteRepository;
    private final BloqueDisponibilidadRepository bloqueRepository;
    private final PreOrdenService preOrdenService;
    private final ReservaMapper reservaMapper;

    // -----------------------------------------------------------------------
    // Disponibilidad
    // -----------------------------------------------------------------------

    /**
     * Consulta la disponibilidad del restaurante para una fecha y hora concretas.
     *
     * <p>Devuelve {@code disponible = false} si se cumple alguna de estas condiciones:
     * <ul>
     *   <li>La hora está fuera del horario de atención (5:00 PM – 10:00 PM).</li>
     *   <li>Existe un bloqueo administrativo activo para esa fecha/hora.</li>
     *   <li>Ninguna zona tiene capacidad restante en ese día.</li>
     * </ul>
     * Cuando hay disponibilidad, la respuesta incluye las listas de zonas y decoraciones libres.
     *
     * @param fechaHora fecha y hora de llegada solicitada por el cliente
     * @return {@link DisponibilidadResponse} con el estado de disponibilidad y las opciones libres
     */
    @Transactional(readOnly = true)
    public DisponibilidadResponse consultarDisponibilidad(LocalDateTime fechaHora) {

        // Rechaza horas fuera del horario de atención del restaurante
        if (!esHorarioValido(fechaHora)) {
            return reservaMapper.sinDisponibilidad();
        }

        // Rechaza fechas/horas bloqueadas administrativamente
        if (estaBloqueda(fechaHora)) {
            return reservaMapper.sinDisponibilidad();
        }

        // Sin zonas registradas no hay nada que ofrecer
        List<Zona> todasLasZonas = zonaRepository.findAll();
        if (todasLasZonas.isEmpty()) {
            return reservaMapper.sinDisponibilidad();
        }

        // Rango del día completo para consultar reservas existentes
        LocalDateTime inicio = fechaHora.toLocalDate().atStartOfDay();
        LocalDateTime fin = fechaHora.toLocalDate().atTime(23, 59, 59);

        // Suma de personas ya reservadas agrupada por zona en ese día
        Map<Long, Integer> personasPorZona = reservaRepository
                .findPersonasPorZonaEnDia(inicio, fin, ESTADOS_ACTIVOS)
                .stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> ((Number) row[1]).intValue()
                ));

        // IDs de decoraciones ya asignadas a otra reserva en el mismo día
        Set<Long> decoracionesOcupadas = Set.copyOf(
                reservaRepository.findDecoracionesOcupadasEnDia(inicio, fin, ESTADOS_ACTIVOS));

        // Filtra zonas que aún tienen capacidad disponible
        List<Zona> zonasLibres = todasLasZonas.stream()
                .filter(z -> personasPorZona.getOrDefault(z.getZonaId(), 0)
                             < z.getZonaCapacidadPersonas())
                .collect(Collectors.toList());

        // Si todas las zonas están llenas, no hay disponibilidad
        if (zonasLibres.isEmpty()) {
            return reservaMapper.sinDisponibilidad();
        }

        // Decoraciones activas que no están ocupadas en el día solicitado
        List<Decoracion> decoracionesActivas = decoracionRepository
                .findByDecoracionEstado(EstadoGenerico.ACTIVO)
                .stream()
                .filter(d -> !decoracionesOcupadas.contains(d.getDecoracionId()))
                .collect(Collectors.toList());

        // IDs de zonas libres para filtrar compatibilidad con decoraciones
        Set<Long> idsZonasLibres = zonasLibres.stream()
                .map(Zona::getZonaId)
                .collect(Collectors.toSet());

        // Carga los links de cada decoración y delega la conversión al mapper
        List<DecoracionDisponibleResponse> decoracionesDto = decoracionesActivas.stream()
                .map(d -> {
                    List<DecoracionZona> links = decoracionZonaRepository.findByDecoracionId(d.getDecoracionId());
                    return reservaMapper.toDecoracionDto(d, links, idsZonasLibres);
                })
                .collect(Collectors.toList());

        // Convierte las zonas libres a DTOs de respuesta
        List<ZonaDisponibleResponse> zonasDto = zonasLibres.stream()
                .map(reservaMapper::toZonaDto)
                .collect(Collectors.toList());

        return DisponibilidadResponse.builder()
                .disponible(true)
                .decoraciones(decoracionesDto)
                .zonas(zonasDto)
                .build();
    }

    // -----------------------------------------------------------------------
    // Creación
    // -----------------------------------------------------------------------

    /**
     * Crea una nueva reserva para el cliente identificado por su correo electrónico.
     *
     * <p>El flujo de validación previo a la persistencia es:
     * <ol>
     *   <li>Verificar que la hora esté dentro del horario de atención.</li>
     *   <li>Verificar que no exista un bloqueo administrativo activo.</li>
     *   <li>Confirmar existencia de decoración y zona en BD (lanza {@code 404} si no existen).</li>
     *   <li>Validar compatibilidad decoración-zona (si ambas fueron seleccionadas).</li>
     *   <li>Confirmar disponibilidad general en tiempo real.</li>
     *   <li>Confirmar disponibilidad específica de decoración y capacidad de zona.</li>
     * </ol>
     *
     * @param emailCliente correo del cliente para quien se crea la reserva
     * @param request      datos de la reserva a crear
     * @return {@link ReservaResponse} con los datos completos de la reserva creada
     * @throws ResourceNotFoundException si el cliente, la decoración o la zona no existen
     * @throws BusinessException si se incumple cualquier regla de negocio
     */
    @Transactional
    public ReservaResponse crearReserva(String emailCliente, CrearReservaRequest request) {

        // Verifica que el cliente exista en la base de datos
        Cliente cliente = clienteRepository.findByUsuario_UsuarioEmail(emailCliente)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente", "email", emailCliente));

        // Rechaza la reserva si la hora está fuera del horario de atención
        if (!esHorarioValido(request.getFechaHoraLlegada())) {
            throw new BusinessException(ErrorCode.INVALID_STATE, MSG_FUERA_HORARIO,
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }

        // Rechaza la reserva si existe un bloqueo administrativo activo
        if (estaBloqueda(request.getFechaHoraLlegada())) {
            throw new BusinessException(ErrorCode.INVALID_STATE, MSG_FUERA_HORARIO,
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }

        // Carga la decoración solo si el cliente la seleccionó
        Decoracion decoracion = null;
        if (request.getDecoracionId() != null) {
            final Long decId = request.getDecoracionId();
            decoracion = decoracionRepository.findById(decId)
                    .orElseThrow(() -> new ResourceNotFoundException("Decoracion", decId));
        }

        // Carga la zona solo si el cliente la seleccionó
        Zona zona = null;
        if (request.getZonaId() != null) {
            final Long zonaId = request.getZonaId();
            zona = zonaRepository.findById(zonaId)
                    .orElseThrow(() -> new ResourceNotFoundException("Zona", zonaId));
        }

        // Valida que decoración y zona sean compatibles entre sí
        if (decoracion != null && zona != null) {
            validarCompatibilidadDecoracionZona(decoracion, zona);
        }

        // Consulta disponibilidad en tiempo real para detectar cambios de último momento
        DisponibilidadResponse disponibilidad = consultarDisponibilidad(request.getFechaHoraLlegada());

        // Si la disponibilidad general cambió, notifica al cliente para que reintente
        if (!disponibilidad.getDisponible()) {
            throw new BusinessException(ErrorCode.INVALID_STATE, MSG_DISPONIBILIDAD_CAMBIO,
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }

        // Verifica que la decoración específica siga libre en tiempo real
        if (decoracion != null) {
            final Long decId = decoracion.getDecoracionId();
            boolean decoracionLibre = disponibilidad.getDecoraciones().stream()
                    .anyMatch(d -> d.getDecoracionId().equals(decId));
            if (!decoracionLibre) {
                throw new BusinessException(ErrorCode.INVALID_STATE, MSG_DISPONIBILIDAD_CAMBIO,
                        HttpStatus.UNPROCESSABLE_ENTITY);
            }
        }

        if (zona != null) {
            // Verifica que la zona específica siga disponible en tiempo real
            final Long zonaId = zona.getZonaId();
            boolean zonaLibre = disponibilidad.getZonas().stream()
                    .anyMatch(z -> z.getZonaId().equals(zonaId));
            if (!zonaLibre) {
                throw new BusinessException(ErrorCode.INVALID_STATE, MSG_DISPONIBILIDAD_CAMBIO,
                        HttpStatus.UNPROCESSABLE_ENTITY);
            }

            // Comprueba que la zona tenga cupo suficiente para el número de personas solicitado
            LocalDateTime inicio = request.getFechaHoraLlegada().toLocalDate().atStartOfDay();
            LocalDateTime fin = request.getFechaHoraLlegada().toLocalDate().atTime(23, 59, 59);
            int personasExistentes = reservaRepository
                    .sumPersonasByZonaEnDia(zonaId, inicio, fin, ESTADOS_ACTIVOS);
            if (personasExistentes + request.getNumeroPersonas() > zona.getZonaCapacidadPersonas()) {
                throw new BusinessException(ErrorCode.INVALID_STATE,
                        "La zona seleccionada no tiene capacidad suficiente para " +
                        request.getNumeroPersonas() + " personas en ese día.",
                        HttpStatus.UNPROCESSABLE_ENTITY);
            }
        }

        // La reserva es ESPECIAL si tiene decoración con costo o algún ítem de menú especial
        boolean tieneDecoracionConCosto = decoracion != null &&
                decoracion.getDecoracionCostoAdicional() != null &&
                decoracion.getDecoracionCostoAdicional().compareTo(java.math.BigDecimal.ZERO) > 0;

        boolean tieneMenuEspecial = request.getPreOrden() != null &&
                request.getPreOrden().stream()
                        .anyMatch(item -> Boolean.TRUE.equals(item.getEsMenuEspecial()));

        boolean esEspecial = tieneDecoracionConCosto || tieneMenuEspecial;

        // Las reservas especiales quedan PENDIENTE (requieren confirmación); las básicas se confirman de inmediato
        TipoReserva tipo = esEspecial ? TipoReserva.ESPECIAL : TipoReserva.BASICA;
        EstadoReserva estado = esEspecial ? EstadoReserva.PENDIENTE : EstadoReserva.CONFIRMADA;

        // Valida la pre-orden antes de persistir la reserva
        if (request.getPreOrden() != null && !request.getPreOrden().isEmpty()) {
            preOrdenService.validarPreOrden(request.getPreOrden(), request.getNumeroPersonas());
        }

        // Construye y persiste la entidad de reserva
        Reserva reserva = Reserva.builder()
                .cliente(cliente)
                .zona(zona)
                .decoracion(decoracion)
                .reservaFechaHoraLlegada(request.getFechaHoraLlegada())
                .reservaNumeroPersonas(request.getNumeroPersonas())
                .reservaNotas(request.getNotas())
                .reservaEstado(estado)
                .reservaTipo(tipo)
                .build();

        Reserva guardada = reservaRepository.save(reserva);

        // Guarda los ítems de pre-orden vinculados a la reserva recién creada
        if (request.getPreOrden() != null && !request.getPreOrden().isEmpty()) {
            preOrdenService.persistirPreOrden(guardada, request.getPreOrden());
        }

        // Carga los detalles persistidos y delega el mapeo al mapper
        List<PreOrdenDetalle> detalles = preOrdenService.obtenerDetallesPorReservaId(guardada.getReservaId());
        return reservaMapper.toResponse(guardada, detalles);
    }

    // -----------------------------------------------------------------------
    // Historial
    // -----------------------------------------------------------------------

    /**
     * Devuelve el historial de reservas del cliente, ordenadas de más reciente a más antigua.
     *
     * @param emailCliente correo del cliente autenticado
     * @return lista de {@link ReservaResponse} con las reservas del cliente; vacía si no tiene ninguna
     * @throws ResourceNotFoundException si el cliente no existe
     */
    @Transactional(readOnly = true)
    public List<ReservaResponse> obtenerReservasCliente(String emailCliente) {
        // Recupera el cliente por email para obtener su ID
        Cliente cliente = clienteRepository.findByUsuario_UsuarioEmail(emailCliente)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cliente", "email", emailCliente));

        // Devuelve las reservas del cliente ordenadas de más reciente a más antigua
        return reservaRepository
                .findByCliente_UsuarioIdOrderByReservaFechaHoraLlegadaDesc(cliente.getUsuarioId())
                .stream()
                .map(r -> reservaMapper.toResponse(r,
                        preOrdenService.obtenerDetallesPorReservaId(r.getReservaId())))
                .collect(Collectors.toList());
    }

    /**
     * Devuelve el historial completo de todas las reservas del sistema, ordenadas de más reciente
     * a más antigua.
     *
     * <p>Solo accesible por personal del restaurante (cajero, mesero, administrador).
     *
     * @return lista de {@link ReservaResponse} con todas las reservas registradas; vacía si no hay ninguna
     */
    @Transactional(readOnly = true)
    public List<ReservaResponse> obtenerTodasLasReservas() {
        // Recupera todas las reservas ordenadas de más reciente a más antigua
        return reservaRepository
                .findAll(Sort.by(Sort.Direction.DESC, "reservaFechaHoraLlegada"))
                .stream()
                .map(r -> reservaMapper.toResponse(r,
                        preOrdenService.obtenerDetallesPorReservaId(r.getReservaId())))
                .collect(Collectors.toList());
    }

    // -----------------------------------------------------------------------
    // Validaciones de negocio
    // -----------------------------------------------------------------------

    /**
     * Verifica que la fecha y hora estén dentro del horario de atención del restaurante.
     *
     * @param fechaHora fecha y hora a verificar
     * @return {@code true} si la hora cae dentro del horario de atención; {@code false} en caso contrario
     */
    private boolean esHorarioValido(LocalDateTime fechaHora) {
        // Extrae solo la hora para comparar contra los límites de apertura y cierre
        int hora = fechaHora.getHour();
        return hora >= HORA_APERTURA && hora < HORA_CIERRE;
    }

    /**
     * Verifica si existe un bloqueo administrativo activo que cubra la fecha y hora indicadas.
     *
     * @param fechaHora fecha y hora a verificar
     * @return {@code true} si hay al menos un bloqueo activo para esa fecha/hora; {@code false} en caso contrario
     */
    private boolean estaBloqueda(LocalDateTime fechaHora) {
        // Separa fecha y hora para consultar los bloqueos que cubran ese instante
        LocalDate fecha = fechaHora.toLocalDate();
        LocalTime hora  = fechaHora.toLocalTime();
        
        // Retorna true si al menos un bloqueo administrativo coincide
        return bloqueRepository.countBloquesParaFechaHora(fecha, hora) > 0;
    }

    /**
     * Verifica que la decoración seleccionada sea compatible con la zona elegida.
     *
     * @param decoracion decoración seleccionada por el cliente
     * @param zona       zona elegida por el cliente
     * @throws BusinessException si la decoración no es compatible con la zona indicada
     */
    private void validarCompatibilidadDecoracionZona(Decoracion decoracion, Zona zona) {

        // Obtiene todas las zonas a las que está vinculada la decoración
        List<DecoracionZona> links = decoracionZonaRepository
                .findByDecoracionId(decoracion.getDecoracionId());

        if (links.size() == 1) {
            // Decoración exclusiva: solo puede usarse en la única zona asignada
            Long zonaPermitida = links.get(0).getZonaId();
            if (!zonaPermitida.equals(zona.getZonaId())) {
                throw new BusinessException(ErrorCode.INVALID_STATE,
                        "La decoración seleccionada solo puede usarse en su zona asignada.",
                        HttpStatus.UNPROCESSABLE_ENTITY);
            }
        } else if (links.size() > 1) {
            // Decoración compartida: verifica que la zona elegida esté entre las permitidas
            boolean esCompatible = links.stream()
                    .anyMatch(l -> l.getZonaId().equals(zona.getZonaId()));
            if (!esCompatible) {
                throw new BusinessException(ErrorCode.INVALID_STATE,
                        "La decoración seleccionada no es compatible con la zona elegida.",
                        HttpStatus.UNPROCESSABLE_ENTITY);
            }
        }
    }
}
