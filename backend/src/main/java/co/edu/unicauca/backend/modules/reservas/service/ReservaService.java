package co.edu.unicauca.backend.modules.reservas.service;

import co.edu.unicauca.backend.modules.inventario.entity.OpcionModificacion;
import co.edu.unicauca.backend.modules.inventario.repository.OpcionModificacionRepository;
import co.edu.unicauca.backend.modules.inventario.repository.ProductoOpcionModificacionRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Comanda;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.ComandaItem;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.ComandaMenuModificacion;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Zona;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ComandaItemRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ComandaMenuModificacionRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ComandaRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ZonaRepository;
import co.edu.unicauca.backend.modules.pagos_caja.entity.Abono;
import co.edu.unicauca.backend.modules.pagos_caja.repository.AbonoRepository;
import co.edu.unicauca.backend.modules.produccion.entity.Producto;
import co.edu.unicauca.backend.modules.produccion.repository.ProductoRepository;
import co.edu.unicauca.backend.modules.reservas.dto.request.CrearReservaRequest;
import co.edu.unicauca.backend.modules.reservas.dto.request.ModificarReservaRequest;
import co.edu.unicauca.backend.modules.reservas.dto.request.PreOrdenItemRequest;
import co.edu.unicauca.backend.modules.reservas.dto.response.DecoracionDisponibleResponse;
import co.edu.unicauca.backend.modules.reservas.dto.response.DisponibilidadResponse;
import co.edu.unicauca.backend.modules.reservas.dto.response.ModificarReservaResponse;
import co.edu.unicauca.backend.modules.reservas.dto.response.ReservaDetalleResponse;
import co.edu.unicauca.backend.modules.reservas.dto.response.ReservaResponse;
import co.edu.unicauca.backend.modules.reservas.dto.response.ZonaDisponibleResponse;
import co.edu.unicauca.backend.modules.reservas.entity.Decoracion;
import co.edu.unicauca.backend.modules.reservas.entity.DecoracionZona;
import co.edu.unicauca.backend.modules.reservas.entity.Reserva;
import co.edu.unicauca.backend.modules.reservas.mapper.ReservaMapper;
import co.edu.unicauca.backend.modules.reservas.repository.BloqueDisponibilidadRepository;
import co.edu.unicauca.backend.modules.reservas.repository.DecoracionRepository;
import co.edu.unicauca.backend.modules.reservas.repository.DecoracionZonaRepository;
import co.edu.unicauca.backend.modules.reservas.repository.ReservaRepository;
import co.edu.unicauca.backend.modules.usuarios.entity.Cliente;
import co.edu.unicauca.backend.modules.usuarios.repository.ClienteRepository;
import co.edu.unicauca.backend.shared.enums.EstadoComanda;
import co.edu.unicauca.backend.shared.enums.EstadoGenerico;
import co.edu.unicauca.backend.shared.enums.EstadoReserva;
import co.edu.unicauca.backend.shared.enums.TipoReserva;
import co.edu.unicauca.backend.shared.exception.BusinessException;
import co.edu.unicauca.backend.shared.exception.ErrorCode;
import co.edu.unicauca.backend.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Servicio de negocio para la gestión de reservas del restaurante.
 *
 * <p>Centraliza la lógica de creación, consulta y validación de reservas,
 * incluyendo el cálculo de disponibilidad de zonas y decoraciones y la gestión
 * de bloqueos administrativos.
 *
 * <p>Cuando la reserva incluye una pre-orden, este servicio crea una {@link Comanda} en
 * estado {@code PRE_RESERVA} con sus {@link ComandaItem} y
 * {@link ComandaMenuModificacion} correspondientes. El mapeo a DTOs se delega en
 * {@link co.edu.unicauca.backend.modules.reservas.mapper.ReservaMapper}.
 *
 * <p>Responsabilidades principales:
 * <ul>
 *   <li>Consultar disponibilidad para una fecha y hora dada.</li>
 *   <li>Crear una reserva validando horario, bloqueos, capacidad y compatibilidad
 *       decoración-zona.</li>
 *   <li>Persistir la pre-orden como comanda en estado {@code PRE_RESERVA}.</li>
 *   <li>Exponer el historial de reservas por cliente.</li>
 *   <li>Devolver las reservas futuras activas del cliente.</li>
 * </ul>
 *
 * @see co.edu.unicauca.backend.modules.reservas.entity.Reserva
 * @see co.edu.unicauca.backend.modules.mesas_comandas.entity.Comanda
 * @see co.edu.unicauca.backend.modules.reservas.mapper.ReservaMapper
 */
@Service
@RequiredArgsConstructor
public class ReservaService {
    private static final List<EstadoReserva> ESTADOS_ACTIVOS = List.of(EstadoReserva.PENDIENTE, EstadoReserva.CONFIRMADA);
    private static final String MSG_DISPONIBILIDAD_CAMBIO = "Lo sentimos, la disponibilidad cambió. Por favor revise nuevamente.";
    private static final String MSG_FUERA_HORARIO = "Lo sentimos, no hay disponibilidad para la fecha y hora seleccionada. " + "Por favor elija otra fecha u hora.";
    private static final int HORA_APERTURA = 17;
    private static final int HORA_CIERRE = 22;
    private static final LocalTime HORA_LIMITE_MODIFICACION = LocalTime.of(16, 0);
    private static final String MSG_NO_MODIFICABLE =
            "Ya no es posible modificar esta reserva. Solo puedes cancelarla.";
    private static final String MSG_ESTADO_NO_MODIFICABLE =
            "Solo puedes modificar reservas con estado PENDIENTE o CONFIRMADA.";
    private static final DateTimeFormatter FORMATTER_WA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ReservaRepository reservaRepository;
    private final DecoracionRepository decoracionRepository;
    private final DecoracionZonaRepository decoracionZonaRepository;
    private final ZonaRepository zonaRepository;
    private final ClienteRepository clienteRepository;
    private final BloqueDisponibilidadRepository bloqueRepository;
    private final ComandaRepository comandaRepository;
    private final ComandaItemRepository comandaItemRepository;
    private final ComandaMenuModificacionRepository comandaMenuModificacionRepository;
    private final ProductoRepository productoRepository;
    private final OpcionModificacionRepository opcionModificacionRepository;
    private final ProductoOpcionModificacionRepository productoOpcionModificacionRepository;
    private final ReservaMapper reservaMapper;
    private final AbonoRepository abonoRepository;

    // -----------------------------------------------------------------------
    // Disponibilidad
    // -----------------------------------------------------------------------

    /**
     * Consulta la disponibilidad del restaurante para una fecha y hora concretas.
     *
     * <p>Devuelve {@code disponible = false} si la hora está fuera del horario de atención,
     * existe un bloqueo administrativo, o ninguna zona tiene capacidad restante.
     *
     * @param fechaHora fecha y hora de llegada solicitada
     * @return {@link DisponibilidadResponse} con el estado y las opciones libres
     */
    @Transactional(readOnly = true)
    public DisponibilidadResponse consultarDisponibilidad(LocalDateTime fechaHora) {

        // Verificar si la fecha y hora están dentro del horario de atención
        if (!esHorarioValido(fechaHora)) {
            return reservaMapper.sinDisponibilidad();
        }

        // Verificar bloqueos administrativos para la fecha
        if (estaBloqueda(fechaHora)) {
            return reservaMapper.sinDisponibilidad();
        }

        // Consultar todas las zonas para determinar cuáles tienen capacidad restante
        List<Zona> todasLasZonas = zonaRepository.findAll();
        if (todasLasZonas.isEmpty()) {
            return reservaMapper.sinDisponibilidad();
        }

        LocalDateTime inicio = fechaHora.toLocalDate().atStartOfDay();
        LocalDateTime fin = fechaHora.toLocalDate().atTime(23, 59, 59);

        // Mapear zonaId a número de personas reservadas en esa zona para el día solicitado
        Map<Long, Integer> personasPorZona = reservaRepository
                .findPersonasPorZonaEnDia(inicio, fin, ESTADOS_ACTIVOS)
                .stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> ((Number) row[1]).intValue()
                ));

        // Filtrar las zonas que tienen capacidad restante (personas reservadas < capacidad total)
        List<Zona> zonasLibres = todasLasZonas.stream()
                .filter(z -> personasPorZona.getOrDefault(z.getZonaId(), 0)
                             < z.getZonaCapacidadPersonas())
                .collect(Collectors.toList());

        // Si no hay zonas libres, el restaurante está completo para esa fecha
        if (zonasLibres.isEmpty()) {
            return reservaMapper.sinDisponibilidad();
        }

        // Obtener los IDs de las zonas libres para determinar compatibilidad con decoraciones
        Set<Long> idsZonasLibres = zonasLibres.stream()
                .map(Zona::getZonaId)
                .collect(Collectors.toSet());

        // Obtener los IDs de decoraciones ocupadas para el día solicitado
        Set<Long> decoracionesOcupadas = Set.copyOf( reservaRepository.findDecoracionesOcupadasEnDia(inicio, fin, ESTADOS_ACTIVOS));

        // Obtener las decoraciones activas que no están ocupadas para el día solicitado
        List<Decoracion> decoracionesActivas = decoracionRepository
                .findByDecoracionEstado(EstadoGenerico.ACTIVO)
                .stream()
                .filter(d -> !decoracionesOcupadas.contains(d.getDecoracionId()))
                .collect(Collectors.toList());

        // Mapear las decoraciones activas a su DTO, incluyendo la información de compatibilidad con zonas libres
        List<DecoracionDisponibleResponse> decoracionesDto = decoracionesActivas.stream()
                .map(d -> {
                    List<DecoracionZona> links = decoracionZonaRepository.findByDecoracionId(d.getDecoracionId());
                    return reservaMapper.toDecoracionDto(d, links, idsZonasLibres);
                })
                .collect(Collectors.toList());

        // Mapear las zonas libres a su DTO de disponibilidad
        List<ZonaDisponibleResponse> zonasDto = zonasLibres.stream()
                .map(reservaMapper::toZonaDto)
                .collect(Collectors.toList());

        // Si hay al menos una zona libre, se considera que hay disponibilidad, aunque las decoraciones compatibles puedan ser limitadas.
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
     * Crea una nueva reserva y, si incluye pre-orden, persiste una {@link Comanda} en estado
     * {@code PRE_RESERVA} con sus {@link ComandaItem} y modificaciones de menú especial.
     *
     * @param emailCliente correo del cliente para quien se crea la reserva
     * @param request      datos de la reserva a crear
     * @return {@link ReservaResponse} con los datos completos de la reserva creada
     * @throws ResourceNotFoundException si el cliente, la decoración, la zona, el producto
     *                                   o la opción de modificación no existen
     * @throws BusinessException si se incumple cualquier regla de negocio
     */
    @Transactional
    public ReservaResponse crearReserva(String emailCliente, CrearReservaRequest request) {

        // Verificar que el cliente exista en la base de datos
        Cliente cliente = clienteRepository.findByUsuario_UsuarioEmail(emailCliente)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente", "email", emailCliente));

        // Validar reglas de negocio relacionadas con horario
        if (!esHorarioValido(request.getFechaHoraLlegada())) {
            throw new BusinessException(ErrorCode.INVALID_STATE, MSG_FUERA_HORARIO,
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }

        // Validar bloqueos administrativos para la fecha solicitada
        if (estaBloqueda(request.getFechaHoraLlegada())) {
            throw new BusinessException(ErrorCode.INVALID_STATE, MSG_FUERA_HORARIO,
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }

        // Validar que la decoración solicitada exista
        Decoracion decoracion = null;
        if (request.getDecoracionId() != null) {
            final Long decId = request.getDecoracionId();
            decoracion = decoracionRepository.findById(decId)
                    .orElseThrow(() -> new ResourceNotFoundException("Decoracion", decId));
        }

        // Validar que la zona solicitada exista
        Zona zona = null;
        if (request.getZonaId() != null) {
            final Long zonaId = request.getZonaId();
            zona = zonaRepository.findById(zonaId)
                    .orElseThrow(() -> new ResourceNotFoundException("Zona", zonaId));
        }

        // Si se asignan decoración y zona, validar que sean compatibles entre sí
        if (decoracion != null && zona != null) {
            validarCompatibilidadDecoracionZona(decoracion, zona);
        }
        
        // Validar que la disponibilidad no haya cambiado desde la última consulta del cliente
        DisponibilidadResponse disponibilidad = consultarDisponibilidad(request.getFechaHoraLlegada());
        
        if (!disponibilidad.getDisponible()) {
            throw new BusinessException(ErrorCode.INVALID_STATE, MSG_DISPONIBILIDAD_CAMBIO,
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }

        // Validar que la decoración solicitada siga estando disponible para la fecha y hora de llegada, considerando las reservas activas para ese día
        if (decoracion != null) {
            final Long decId = decoracion.getDecoracionId();
            boolean decoracionLibre = disponibilidad.getDecoraciones().stream()
                    .anyMatch(d -> d.getDecoracionId().equals(decId));
            if (!decoracionLibre) {
                throw new BusinessException(ErrorCode.INVALID_STATE, MSG_DISPONIBILIDAD_CAMBIO,
                        HttpStatus.UNPROCESSABLE_ENTITY);
            }
        }

        // Validar capacidad según si se especificó zona o no
        LocalDateTime inicioDia = request.getFechaHoraLlegada().toLocalDate().atStartOfDay();
        LocalDateTime finDia = request.getFechaHoraLlegada().toLocalDate().atTime(23, 59, 59);

        if (zona != null) {
            final Long zonaId = zona.getZonaId();
            boolean zonaLibre = disponibilidad.getZonas().stream().anyMatch(z -> z.getZonaId().equals(zonaId));
            if (!zonaLibre) {
                throw new BusinessException(ErrorCode.INVALID_STATE, MSG_DISPONIBILIDAD_CAMBIO,
                        HttpStatus.UNPROCESSABLE_ENTITY);
            }

            // Verificar que la zona tenga capacidad para el número de personas solicitado, sumando las personas de las reservas activas para ese día
            int personasExistentes = reservaRepository.sumPersonasByZonaEnDia(zonaId, inicioDia, finDia, ESTADOS_ACTIVOS);
            if (personasExistentes + request.getNumeroPersonas() > zona.getZonaCapacidadPersonas()) {
                throw new BusinessException(ErrorCode.INVALID_STATE,
                        "La zona seleccionada no tiene capacidad suficiente para " +
                        request.getNumeroPersonas() + " personas en ese día.",
                        HttpStatus.UNPROCESSABLE_ENTITY);
            }
        } else {
            // Sin zona específica: verificar que al menos una zona disponible tenga capacidad
            // suficiente para el número de personas solicitado
            boolean hayZonaConCapacidad = disponibilidad.getZonas().stream()
                    .anyMatch(z -> {
                        int ocupadas = reservaRepository.sumPersonasByZonaEnDia(
                                z.getZonaId(), inicioDia, finDia, ESTADOS_ACTIVOS);
                        return (z.getCapacidad() - ocupadas) >= request.getNumeroPersonas();
                    });
            if (!hayZonaConCapacidad) {
                throw new BusinessException(ErrorCode.INVALID_STATE,
                        "No hay zonas con capacidad suficiente para " +
                        request.getNumeroPersonas() + " personas en ese día.",
                        HttpStatus.UNPROCESSABLE_ENTITY);
            }
        }

        // Determinar tipo de reserva (BASICA o ESPECIAL) según decoración y pre-orden
        TipoReserva tipo     = determinarTipoReserva(decoracion, request.getPreOrden());
        EstadoReserva estado = (tipo == TipoReserva.ESPECIAL)
                ? EstadoReserva.PENDIENTE
                : EstadoReserva.CONFIRMADA;

        // Si se incluye pre-orden, validar que los productos y opciones de modificación existan y sean válidos para el número de personas de la reserva
        if (request.getPreOrden() != null && !request.getPreOrden().isEmpty()) {
            validarPreOrden(request.getPreOrden(), request.getNumeroPersonas());
        }

        // Crear la reserva 
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
        
        // Si se incluye pre-orden, persistirla como comanda en estado PRE_RESERVA vinculada a la reserva creada
        if (request.getPreOrden() != null && !request.getPreOrden().isEmpty()) {
            persistirPreOrden(guardada, request.getPreOrden());
        }

        return reservaMapper.toResponse(guardada);
    }

    // -----------------------------------------------------------------------
    // Dashboard del cliente
    // -----------------------------------------------------------------------

    /**
     * Devuelve el resumen de las reservas futuras activas del cliente, ordenadas de la
     * más próxima a la más lejana.
     *
     * @param emailCliente correo del cliente autenticado
     * @return lista de {@link ReservaDetalleResponse} ordenada ascendentemente por fecha de llegada
     * @throws ResourceNotFoundException si el cliente no existe
     */
    @Transactional(readOnly = true)
    public List<ReservaDetalleResponse> obtenerReservasFuturas(String emailCliente) {

        // Verifica que el cliente exista
        Cliente cliente = clienteRepository.findByUsuario_UsuarioEmail(emailCliente)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente", "email", emailCliente));

        LocalDateTime ahora = LocalDateTime.now();

        // Filtra las reservas del cliente
        return reservaRepository
                .findByCliente_UsuarioIdOrderByReservaFechaHoraLlegadaDesc(cliente.getUsuarioId())
                .stream()
                .filter(r -> ESTADOS_ACTIVOS.contains(r.getReservaEstado())
                          && r.getReservaFechaHoraLlegada().isAfter(ahora))
                .sorted(Comparator.comparing(Reserva::getReservaFechaHoraLlegada))
                .map(reservaMapper::toResumen)
                .collect(Collectors.toList());
    }

    /**
     * Devuelve el detalle completo de una reserva.
     *
     * <p>Si {@code emailAutenticado} no es {@code null} (rol CLIENTE), valida que la reserva
     * pertenezca al cliente autenticado antes de retornar el detalle. Si es {@code null}
     * (otros roles: CAJERO, ADM), el acceso se permite sin restricción de propiedad.
     *
     * @param reservaId        identificador de la reserva
     * @param emailAutenticado correo del cliente autenticado, o {@code null} si el solicitante
     *                         no es CLIENTE
     * @return {@link ReservaDetalleResponse} con el detalle completo
     * @throws ResourceNotFoundException si la reserva no existe
     * @throws BusinessException         si el CLIENTE intenta acceder a una reserva que no es suya
     */
    @Transactional(readOnly = true)
    public ReservaDetalleResponse obtenerDetalleReserva(Long reservaId, String emailAutenticado) {

        Reserva reserva = reservaRepository.findById(reservaId)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva", reservaId));

        if (emailAutenticado != null &&
                !reserva.getCliente().getUsuario().getUsuarioEmail().equalsIgnoreCase(emailAutenticado)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED,
                    "Solo puedes consultar el detalle de tus propias reservas.", HttpStatus.FORBIDDEN);
        }

        List<ComandaItem> preOrden = obtenerItemsPreOrden(reservaId);
        List<Abono> abonos = abonoRepository.findByReserva_ReservaIdOrderByAbonoFechaHoraAsc(reservaId);

        return reservaMapper.toDetalleResponse(reserva, preOrden, abonos);
    }

    /**
     * Modifica una reserva existente del cliente aplicando las mismas validaciones de negocio
     * que la creación y verificando la disponibilidad excluyendo la reserva actual.
     *
     * <p><b>Regla de hora límite:</b> solo es posible modificar antes de las 16:00 del día
     * de la reserva (CA-01).</p>
     *
     * <p><b>Transiciones de tipo y estado:</b>
     * <ul>
     *   <li>BASICA → BASICA: actualiza campos, mantiene CONFIRMADA.</li>
     *   <li>BASICA → ESPECIAL: actualiza campos, cambia a PENDIENTE. Requiere WhatsApp.</li>
     *   <li>ESPECIAL → ESPECIAL: actualiza campos, mantiene PENDIENTE.</li>
     *   <li>ESPECIAL → BASICA: cancela reserva original, crea nueva BASICA CONFIRMADA. Requiere WhatsApp.</li>
     * </ul>
     *
     * @param reservaId    ID de la reserva a modificar
     * @param emailCliente email del cliente autenticado (tomado del token)
     * @param request      nuevos datos de la reserva
     * @return {@link ModificarReservaResponse} con la reserva resultante y flag de WhatsApp si aplica
     * @throws ResourceNotFoundException si la reserva, decoración, zona o productos no existen
     * @throws BusinessException         si se incumple alguna regla de negocio
     */
    @Transactional
    public ModificarReservaResponse modificarReserva(Long reservaId,
                                                      String emailCliente,
                                                      ModificarReservaRequest request) {

        // Verificar existencia de la reserva
        Reserva reserva = reservaRepository.findById(reservaId)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva", reservaId));

        // Verificar ownership (CA-02)
        if (!reserva.getCliente().getUsuario().getUsuarioEmail().equalsIgnoreCase(emailCliente)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED,
                    "Solo puedes modificar tus propias reservas.", HttpStatus.FORBIDDEN);
        }

        // Verificar que la reserva esté en estado activo
        if (!ESTADOS_ACTIVOS.contains(reserva.getReservaEstado())) {
            throw new BusinessException(ErrorCode.INVALID_STATE,
                    MSG_ESTADO_NO_MODIFICABLE, HttpStatus.UNPROCESSABLE_ENTITY);
        }

        // Verificar hora límite de modificación: antes de las 16:00 del día de la reserva (CA-01)
        LocalDateTime limiteModificacion = reserva.getReservaFechaHoraLlegada()
                .toLocalDate().atTime(HORA_LIMITE_MODIFICACION);
        if (!LocalDateTime.now().isBefore(limiteModificacion)) {
            throw new BusinessException(ErrorCode.INVALID_STATE,
                    MSG_NO_MODIFICABLE, HttpStatus.UNPROCESSABLE_ENTITY);
        }

        // Validar horario del restaurante para la nueva fecha/hora (CA-04)
        if (!esHorarioValido(request.getFechaHoraLlegada())) {
            throw new BusinessException(ErrorCode.INVALID_STATE,
                    MSG_FUERA_HORARIO, HttpStatus.UNPROCESSABLE_ENTITY);
        }

        // Validar bloqueos administrativos para la nueva fecha/hora (CA-04)
        if (estaBloqueda(request.getFechaHoraLlegada())) {
            throw new BusinessException(ErrorCode.INVALID_STATE,
                    MSG_FUERA_HORARIO, HttpStatus.UNPROCESSABLE_ENTITY);
        }

        // Validar decoración (CA-04)
        Decoracion nuevaDecoracion = null;
        if (request.getDecoracionId() != null) {
            final Long decId = request.getDecoracionId();
            nuevaDecoracion = decoracionRepository.findById(decId)
                    .orElseThrow(() -> new ResourceNotFoundException("Decoracion", decId));
        }

        // Validar zona (CA-04)
        Zona nuevaZona = null;
        if (request.getZonaId() != null) {
            final Long zId = request.getZonaId();
            nuevaZona = zonaRepository.findById(zId)
                    .orElseThrow(() -> new ResourceNotFoundException("Zona", zId));
        }

        // Validar compatibilidad decoración-zona (CA-04)
        if (nuevaDecoracion != null && nuevaZona != null) {
            validarCompatibilidadDecoracionZona(nuevaDecoracion, nuevaZona);
        }

        // Verificar disponibilidad excluyendo la reserva actual (CA-06, CA-07)
        DisponibilidadResponse disponibilidad =
                consultarDisponibilidadParaModificacion(request.getFechaHoraLlegada(), reservaId);

        if (!disponibilidad.getDisponible()) {
            throw new BusinessException(ErrorCode.INVALID_STATE,
                    MSG_DISPONIBILIDAD_CAMBIO, HttpStatus.UNPROCESSABLE_ENTITY);
        }

        // Verificar que la decoración elegida esté disponible
        if (nuevaDecoracion != null) {
            final Long decId = nuevaDecoracion.getDecoracionId();
            boolean decoracionLibre = disponibilidad.getDecoraciones().stream()
                    .anyMatch(d -> d.getDecoracionId().equals(decId));
            if (!decoracionLibre) {
                throw new BusinessException(ErrorCode.INVALID_STATE,
                        MSG_DISPONIBILIDAD_CAMBIO, HttpStatus.UNPROCESSABLE_ENTITY);
            }
        }

        // Verificar capacidad de zona
        LocalDateTime inicioDia = request.getFechaHoraLlegada().toLocalDate().atStartOfDay();
        LocalDateTime finDia    = request.getFechaHoraLlegada().toLocalDate().atTime(23, 59, 59);

        if (nuevaZona != null) {
            final Long zId = nuevaZona.getZonaId();
            boolean zonaLibre = disponibilidad.getZonas().stream()
                    .anyMatch(z -> z.getZonaId().equals(zId));
            if (!zonaLibre) {
                throw new BusinessException(ErrorCode.INVALID_STATE,
                        MSG_DISPONIBILIDAD_CAMBIO, HttpStatus.UNPROCESSABLE_ENTITY);
            }
            int personasExistentes = reservaRepository.sumPersonasByZonaEnDiaExcluyendo(
                    zId, inicioDia, finDia, ESTADOS_ACTIVOS, reservaId);
            if (personasExistentes + request.getNumeroPersonas() > nuevaZona.getZonaCapacidadPersonas()) {
                throw new BusinessException(ErrorCode.INVALID_STATE,
                        "La zona seleccionada no tiene capacidad suficiente para " +
                        request.getNumeroPersonas() + " personas en ese día.",
                        HttpStatus.UNPROCESSABLE_ENTITY);
            }
        } else {
            boolean hayZonaConCapacidad = disponibilidad.getZonas().stream()
                    .anyMatch(z -> {
                        int ocupadas = reservaRepository.sumPersonasByZonaEnDiaExcluyendo(
                                z.getZonaId(), inicioDia, finDia, ESTADOS_ACTIVOS, reservaId);
                        return (z.getCapacidad() - ocupadas) >= request.getNumeroPersonas();
                    });
            if (!hayZonaConCapacidad) {
                throw new BusinessException(ErrorCode.INVALID_STATE,
                        "No hay zonas con capacidad suficiente para " +
                        request.getNumeroPersonas() + " personas en ese día.",
                        HttpStatus.UNPROCESSABLE_ENTITY);
            }
        }

        // Validar pre-orden (CA-05)
        if (request.getPreOrden() != null && !request.getPreOrden().isEmpty()) {
            validarPreOrden(request.getPreOrden(), request.getNumeroPersonas());
        }

        // Determinar nuevo tipo usando el helper extraído en Task 8.5
        TipoReserva nuevoTipo    = determinarTipoReserva(nuevaDecoracion, request.getPreOrden());
        boolean nuevoEsEspecial  = (nuevoTipo == TipoReserva.ESPECIAL);
        boolean anteriorEraEspecial = reserva.getReservaTipo() == TipoReserva.ESPECIAL;
        boolean requiereWhatsApp = false;
        Reserva reservaResultado;

        if (anteriorEraEspecial && !nuevoEsEspecial) {
            // ESPECIAL → BASICA (CA-07): cancelar original, crear nueva BASICA CONFIRMADA
            reserva.setReservaEstado(EstadoReserva.CANCELADA);
            reservaRepository.save(reserva);
            eliminarPreOrdenExistente(reservaId);

            Reserva nuevaReserva = Reserva.builder()
                    .cliente(reserva.getCliente())
                    .zona(nuevaZona)
                    .decoracion(nuevaDecoracion)
                    .reservaFechaHoraLlegada(request.getFechaHoraLlegada())
                    .reservaNumeroPersonas(request.getNumeroPersonas())
                    .reservaNotas(request.getNotas())
                    .reservaEstado(EstadoReserva.CONFIRMADA)
                    .reservaTipo(TipoReserva.BASICA)
                    .build();
            reservaResultado = reservaRepository.save(nuevaReserva);
            requiereWhatsApp = true;
        } else {
            // BASICA→BASICA, BASICA→ESPECIAL, ESPECIAL→ESPECIAL: actualizar reserva existente
            EstadoReserva nuevoEstado;
            if (!anteriorEraEspecial && nuevoEsEspecial) {
                // BASICA → ESPECIAL (CA-06): cambiar a PENDIENTE
                nuevoEstado      = EstadoReserva.PENDIENTE;
                requiereWhatsApp = true;
            } else {
                // BASICA→BASICA: mantener CONFIRMADA | ESPECIAL→ESPECIAL: mantener PENDIENTE
                nuevoEstado = reserva.getReservaEstado();
            }

            reserva.setZona(nuevaZona);
            reserva.setDecoracion(nuevaDecoracion);
            reserva.setReservaFechaHoraLlegada(request.getFechaHoraLlegada());
            reserva.setReservaNumeroPersonas(request.getNumeroPersonas());
            reserva.setReservaNotas(request.getNotas());
            reserva.setReservaEstado(nuevoEstado);
            reserva.setReservaTipo(nuevoTipo);

            eliminarPreOrdenExistente(reservaId);
            reservaResultado = reservaRepository.save(reserva);
        }

        // Persistir nueva pre-orden si existe (CA-05)
        if (request.getPreOrden() != null && !request.getPreOrden().isEmpty()) {
            persistirPreOrden(reservaResultado, request.getPreOrden());
        }

        // Componer mensaje WhatsApp si la transición lo requiere (CA-06, CA-07)
        String mensajeWhatsApp = requiereWhatsApp ? construirMensajeWhatsApp(reservaResultado) : null;

        return reservaMapper.toModificarResponse(reservaResultado, requiereWhatsApp, mensajeWhatsApp);
    }

    /**
     * Devuelve el historial de reservas canceladas o devueltas del cliente.
     *
     * @param emailCliente correo del cliente
     * @return lista de {@link ReservaDetalleResponse} ordenada descendentemente por fecha
     * @throws ResourceNotFoundException si el cliente no existe
     */
    @Transactional(readOnly = true)
    public List<ReservaDetalleResponse> obtenerReservasCanceladasODevueltas(String emailCliente) {

        // Verifica que el cliente exista
        Cliente cliente = clienteRepository.findByUsuario_UsuarioEmail(emailCliente)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente", "email", emailCliente));

        // Solo se consideran estados terminales para el historial
        List<EstadoReserva> estadosTerminales =
                List.of(EstadoReserva.CANCELADA, EstadoReserva.DEVUELTA);

        return reservaRepository
                .findByCliente_UsuarioIdOrderByReservaFechaHoraLlegadaDesc(cliente.getUsuarioId())
                .stream()
                .filter(r -> estadosTerminales.contains(r.getReservaEstado()))
                .map(reservaMapper::toResumen)
                .collect(Collectors.toList());
    }

    // -----------------------------------------------------------------------
    // Lógica interna de pre-orden (antes en PreOrdenService)
    // -----------------------------------------------------------------------

    /**
     * Devuelve los {@link ComandaItem} de la comanda PRE_RESERVA de una reserva.
     *
     * @param reservaId identificador de la reserva
     * @return lista de ítems de pre-orden; vacía si la reserva no tiene pre-orden
     */
    private List<ComandaItem> obtenerItemsPreOrden(Long reservaId) {
        return comandaRepository
                .findByReserva_ReservaIdAndComandaEstado(reservaId, EstadoComanda.PRE_RESERVA)
                .map(c -> comandaItemRepository.findByComanda_ComandaId(c.getComandaId()))
                .orElse(List.of());
    }

    /**
     * Valida las reglas de negocio de la pre-orden antes de persistirla.
     *
     * <ul>
     *   <li>Todo ítem marcado con {@code esMenuEspecial=true} debe corresponder a un producto
     *       con {@code menu_especial=true} en la base de datos.</li>
     *   <li>Solo un ítem de menú especial por reserva.</li>
     *   <li>El menú especial requiere más de 10 comensales.</li>
     * </ul>
     *
     * @param items          lista de ítems de la pre-orden
     * @param numeroPersonas número de comensales de la reserva
     * @throws ResourceNotFoundException si el producto no existe
     * @throws BusinessException         si se incumple alguna regla
     */
    private void validarPreOrden(List<PreOrdenItemRequest> items, int numeroPersonas) {

        // Validar que cada ítem marcado como menú especial corresponda a un producto menu_especial en BD
        for (PreOrdenItemRequest item : items) {
            if (Boolean.TRUE.equals(item.getEsMenuEspecial())) {
                Producto producto = productoRepository.findById(item.getProductoId())
                        .orElseThrow(() -> new ResourceNotFoundException("Producto", item.getProductoId()));
                if (!Boolean.TRUE.equals(producto.getMenuEspecial())) {
                    throw new BusinessException(ErrorCode.INVALID_STATE,
                            "El producto '" + producto.getProductoNombre() + "' no es un menú especial.",
                            HttpStatus.UNPROCESSABLE_ENTITY);
                }
            }
        }

        // Contar cuántos ítems de menú especial hay en la pre-orden
        long menuEspecialCount = items.stream()
                .filter(i -> Boolean.TRUE.equals(i.getEsMenuEspecial()))
                .count();

        // Validar que no haya más de un ítem de menú especial
        if (menuEspecialCount > 1) {
            throw new BusinessException(ErrorCode.INVALID_STATE,
                    "Solo puede seleccionar un menú especial por reserva.",
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }

        // Validar que si hay un menú especial, el número de personas sea mayor a 10
        if (menuEspecialCount > 0 && numeroPersonas <= 10) {
            throw new BusinessException(ErrorCode.INVALID_STATE,
                    "El menú especial solo está disponible para reservas de más de 10 personas.",
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    /**
     * Persiste la pre-orden como una {@link Comanda} en estado {@code PRE_RESERVA}, con
     * sus {@link ComandaItem} y {@link ComandaMenuModificacion} correspondientes.
     *
     * @param reserva reserva ya persistida a la que se asocia la pre-orden
     * @param items   lista de ítems a guardar
     * @return lista de {@link ComandaItem} persistidos
     * @throws ResourceNotFoundException si un producto o una opción no existen
     * @throws BusinessException si el producto no está activo o la opción no pertenece al menú
     */
    private List<ComandaItem> persistirPreOrden(Reserva reserva, List<PreOrdenItemRequest> items) {
        // Crear la comanda en estado PRE_RESERVA vinculada a la reserva
        Comanda preComanda = Comanda.builder()
                .reserva(reserva)
                .comandaEstado(EstadoComanda.PRE_RESERVA)
                .build();

        // Guardar la comanda para obtener su ID y poder asociar los ítems
        Comanda comandaGuardada = comandaRepository.save(preComanda);

        // Iterar sobre los ítems de la pre-orden para validarlos y persistirlos
        for (PreOrdenItemRequest item : items) {
            // Validar que el producto exista
            Producto producto = productoRepository.findById(item.getProductoId())
                    .orElseThrow(() -> new ResourceNotFoundException("Producto", item.getProductoId()));
            
            // Validar que el producto esté activo
            if (producto.getProductoEstado() != EstadoGenerico.ACTIVO) {
                throw new BusinessException(ErrorCode.INVALID_STATE,
                        "El producto '" + producto.getProductoNombre() + "' no está disponible.",
                        HttpStatus.UNPROCESSABLE_ENTITY);
            }

            // Crear el detalle de la comanda para este ítem
            ComandaItem detalle = ComandaItem.builder()
                    .comanda(comandaGuardada)
                    .producto(producto)
                    .comandaItemCantidad(item.getCantidad())
                    .comandaItemPrecio(producto.getProductoPrecio())
                    .comandaItemDescripcion(item.getDescripcion())
                    .build();

            // Guardar el detalle para obtener su ID y poder asociar las modificaciones
            ComandaItem detalleGuardado = comandaItemRepository.save(detalle);

            // Si el ítem es un menú especial, validar y persistir las opciones de modificación asociadas
            if (Boolean.TRUE.equals(item.getEsMenuEspecial())
                    && item.getOpcionesModificacion() != null
                    && !item.getOpcionesModificacion().isEmpty()) {

                // Validar cada opción de modificación
                for (Long opcionId : item.getOpcionesModificacion()) {

                    // Validar que la opción de modificación exista
                    OpcionModificacion opcion = opcionModificacionRepository.findById(opcionId)
                            .orElseThrow(() -> new ResourceNotFoundException("OpcionModificacion", opcionId));

                    // Validar que la opción de modificación pertenezca al producto del menú especial
                    if (!productoOpcionModificacionRepository.existsByProductoIdAndOpcionId( producto.getProductoId(), opcionId)) {
                        throw new BusinessException(ErrorCode.INVALID_STATE,
                                "La opción de modificación '" + opcion.getOpcionNombre() +
                                "' no pertenece al menú seleccionado.",
                                HttpStatus.UNPROCESSABLE_ENTITY);
                    }

                    // Crear el enlace entre el detalle de la comanda y la opción de modificación seleccionada
                    ComandaMenuModificacion mod = ComandaMenuModificacion.builder()
                            .comandaItem(detalleGuardado)
                            .opcion(opcion)
                            .build();

                    // Guardar el enlace para persistir la selección de modificación
                    comandaMenuModificacionRepository.save(mod);
                }
            }
        }

        // Devolver la lista de ítems de la pre-orden ya persistidos
        return comandaItemRepository.findByComanda_ComandaId(comandaGuardada.getComandaId());
    }

    /**
     * Elimina la pre-orden ({@code PRE_RESERVA} comanda) de una reserva si existe.
     *
     * <p>Borra en orden: modificaciones de menú → ítems → comanda, para respetar
     * las restricciones de FK de la base de datos.
     *
     * @param reservaId identificador de la reserva cuya pre-orden se va a eliminar
     */
    private void eliminarPreOrdenExistente(Long reservaId) {
        // Buscar la comanda PRE_RESERVA de esta reserva; si no existe, no hay nada que eliminar
        comandaRepository
                .findByReserva_ReservaIdAndComandaEstado(reservaId, EstadoComanda.PRE_RESERVA)
                .ifPresent(comanda -> {
                    List<ComandaItem> items =
                            comandaItemRepository.findByComanda_ComandaId(comanda.getComandaId());
                    // Borrar modificaciones de menú especial antes que los ítems (FK constraint)
                    items.forEach(item ->
                            comandaMenuModificacionRepository
                                    .deleteByComandaItem_ComandaItemId(item.getComandaItemId()));
                    // Borrar los ítems antes que la comanda (FK constraint)
                    comandaItemRepository.deleteAll(items);
                    // Eliminar la comanda vacía
                    comandaRepository.delete(comanda);
                });
    }

    // -----------------------------------------------------------------------
    // Validaciones privadas
    // -----------------------------------------------------------------------

    /**
     * Determina el tipo de una reserva según su decoración y pre-orden.
     *
     * <p>Una reserva es ESPECIAL si su decoración tiene costo adicional mayor a cero
     * o si algún ítem de la pre-orden es un menú especial. En cualquier otro caso es BASICA.
     *
     * @param decoracion decoración seleccionada; {@code null} si no se eligió ninguna
     * @param preOrden   ítems de pre-orden; {@code null} o vacía si no hay pre-orden
     * @return {@link TipoReserva#ESPECIAL} o {@link TipoReserva#BASICA}
     */
    private TipoReserva determinarTipoReserva(Decoracion decoracion,
                                               List<PreOrdenItemRequest> preOrden) {
        // Decoración con costo adicional convierte la reserva en ESPECIAL
        boolean tieneDecoracionConCosto = decoracion != null
                && decoracion.getDecoracionCostoAdicional() != null
                && decoracion.getDecoracionCostoAdicional().compareTo(BigDecimal.ZERO) > 0;

        // Pre-orden con al menos un menú especial también convierte la reserva en ESPECIAL
        boolean tieneMenuEspecial = preOrden != null
                && preOrden.stream().anyMatch(i -> Boolean.TRUE.equals(i.getEsMenuEspecial()));

        return (tieneDecoracionConCosto || tieneMenuEspecial)
                ? TipoReserva.ESPECIAL
                : TipoReserva.BASICA;
    }

    /**
     * Consulta disponibilidad para una fecha/hora excluyendo la reserva que se está modificando.
     *
     * <p>Lógica idéntica a {@link #consultarDisponibilidad(LocalDateTime)}, pero usa las
     * queries {@code *Excluyendo} del repositorio para no contar la reserva actual en los
     * cálculos de ocupación de zona y decoración.
     *
     * @param fechaHora        nueva fecha y hora de llegada solicitada
     * @param excludeReservaId ID de la reserva siendo modificada, excluida de los conteos
     * @return {@link DisponibilidadResponse} con zonas y decoraciones disponibles
     */
    private DisponibilidadResponse consultarDisponibilidadParaModificacion(
            LocalDateTime fechaHora, Long excludeReservaId) {

        if (!esHorarioValido(fechaHora) || estaBloqueda(fechaHora)) {
            return reservaMapper.sinDisponibilidad();
        }

        List<Zona> todasLasZonas = zonaRepository.findAll();
        if (todasLasZonas.isEmpty()) {
            return reservaMapper.sinDisponibilidad();
        }

        LocalDateTime inicio = fechaHora.toLocalDate().atStartOfDay();
        LocalDateTime fin    = fechaHora.toLocalDate().atTime(23, 59, 59);

        Map<Long, Integer> personasPorZona = reservaRepository
                .findPersonasPorZonaEnDiaExcluyendo(inicio, fin, ESTADOS_ACTIVOS, excludeReservaId)
                .stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> ((Number) row[1]).intValue()
                ));

        List<Zona> zonasLibres = todasLasZonas.stream()
                .filter(z -> personasPorZona.getOrDefault(z.getZonaId(), 0)
                             < z.getZonaCapacidadPersonas())
                .collect(Collectors.toList());

        if (zonasLibres.isEmpty()) {
            return reservaMapper.sinDisponibilidad();
        }

        Set<Long> idsZonasLibres = zonasLibres.stream()
                .map(Zona::getZonaId)
                .collect(Collectors.toSet());

        Set<Long> decoracionesOcupadas = Set.copyOf(
                reservaRepository.findDecoracionesOcupadasEnDiaExcluyendo(
                        inicio, fin, ESTADOS_ACTIVOS, excludeReservaId));

        List<Decoracion> decoracionesActivas = decoracionRepository
                .findByDecoracionEstado(EstadoGenerico.ACTIVO)
                .stream()
                .filter(d -> !decoracionesOcupadas.contains(d.getDecoracionId()))
                .collect(Collectors.toList());

        List<DecoracionDisponibleResponse> decoracionesDto = decoracionesActivas.stream()
                .map(d -> {
                    List<DecoracionZona> links =
                            decoracionZonaRepository.findByDecoracionId(d.getDecoracionId());
                    return reservaMapper.toDecoracionDto(d, links, idsZonasLibres);
                })
                .collect(Collectors.toList());

        List<ZonaDisponibleResponse> zonasDto = zonasLibres.stream()
                .map(reservaMapper::toZonaDto)
                .collect(Collectors.toList());

        return DisponibilidadResponse.builder()
                .disponible(true)
                .decoraciones(decoracionesDto)
                .zonas(zonasDto)
                .build();
    }

    /**
     * Valida que la fecha y hora de llegada estén dentro del horario de atención del restaurante.
     * @param fechaHora fecha y hora de llegada a validar
     * @return {@code true} si la hora está dentro del horario de atención, {@code false} en caso contrario
     */
    private boolean esHorarioValido(LocalDateTime fechaHora) {
        int hora = fechaHora.getHour();
        return hora >= HORA_APERTURA && hora < HORA_CIERRE;
    }

    /**
     * Verifica si existe un bloqueo administrativo para la fecha y hora de llegada solicitada.
     * @param fechaHora fecha y hora de llegada a validar
     * @return {@code true} si existe un bloqueo, {@code false} en caso contrario
     */
    private boolean estaBloqueda(LocalDateTime fechaHora) {
        LocalDate fecha = fechaHora.toLocalDate();
        LocalTime hora  = fechaHora.toLocalTime();
        return bloqueRepository.countBloquesParaFechaHora(fecha, hora) > 0;
    }

    /**
     * Valida que la decoración seleccionada sea compatible con la zona elegida, según las reglas de negocio:
     * <ul>
     *   <li>Si la decoración solo tiene un enlace a zona, solo puede usarse en esa zona.</li>
     *   <li>Si la decoración tiene múltiples enlaces a zonas, solo puede usarse en las zonas a las que esté vinculada.</li>
     * </ul>
     * Si la decoración no tiene enlaces a zonas, se asume que es compatible con todas las zonas.
     * 
     * @param decoracion la decoración seleccionada para la reserva
     * @param zona la zona seleccionada para la reserva
     */
    private void validarCompatibilidadDecoracionZona(Decoracion decoracion, Zona zona) {

        // Obtener los enlaces entre la decoración y las zonas a las que está vinculada
        List<DecoracionZona> links = decoracionZonaRepository
                .findByDecoracionId(decoracion.getDecoracionId());

        // Si la decoración no tiene enlaces a zonas, se asume que es compatible con todas las zonas.
        if (links.size() == 1) {
            // Si la decoración solo tiene un enlace a zona, solo puede usarse en esa zona.
            Long zonaPermitida = links.get(0).getZonaId();
            if (!zonaPermitida.equals(zona.getZonaId())) {
                throw new BusinessException(ErrorCode.INVALID_STATE,
                        "La decoración seleccionada solo puede usarse en su zona asignada.",
                        HttpStatus.UNPROCESSABLE_ENTITY);
            }
        } else if (links.size() > 1) {
            // Si la decoración tiene múltiples enlaces a zonas, solo puede usarse en las zonas a las que esté vinculada.
            boolean esCompatible = links.stream()
                    .anyMatch(l -> l.getZonaId().equals(zona.getZonaId()));
            if (!esCompatible) {
                throw new BusinessException(ErrorCode.INVALID_STATE,
                        "La decoración seleccionada no es compatible con la zona elegida.",
                        HttpStatus.UNPROCESSABLE_ENTITY);
            }
        }
    }

    /**
     * Construye el mensaje precompuesto para el chat de WhatsApp cuando la reserva
     * requiere confirmación especial (transiciones BASICA→ESPECIAL o ESPECIAL→BASICA).
     *
     * <p>El mensaje incluye: ID de reserva, nombre del cliente, fecha y hora,
     * número de personas, y opcionalmente decoración y zona si están asignadas.
     *
     * @param reserva entidad resultante de la modificación
     * @return mensaje formateado listo para enviar por WhatsApp
     */
    private String construirMensajeWhatsApp(Reserva reserva) {
        StringBuilder sb = new StringBuilder();
        // Identificación del cliente y reserva
        sb.append("Hola, soy ").append(reserva.getCliente().getClienteNombre()).append(".\n");
        sb.append("Quisiera confirmar mi reserva #").append(reserva.getReservaId()).append(":\n");
        // Datos obligatorios de la reserva
        sb.append("- Fecha y hora: ")
          .append(reserva.getReservaFechaHoraLlegada().format(FORMATTER_WA)).append("\n");
        sb.append("- Número de personas: ")
          .append(reserva.getReservaNumeroPersonas()).append("\n");
        // Decoración y zona solo si están asignadas
        if (reserva.getDecoracion() != null) {
            sb.append("- Decoración: ")
              .append(reserva.getDecoracion().getDecoracionNombre()).append("\n");
        }
        if (reserva.getZona() != null) {
            sb.append("- Zona: ").append(reserva.getZona().getZonaNombre()).append("\n");
        }
        sb.append("\nPara confirmar tu reserva especial, debes abonar un valor anticipado, " +
                  "comunicate para definirlo.");
        return sb.toString();
    }
}
