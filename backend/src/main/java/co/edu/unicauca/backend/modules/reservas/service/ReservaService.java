package co.edu.unicauca.backend.modules.reservas.service;

import co.edu.unicauca.backend.modules.mesas_comandas.entity.ComandaItem;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Zona;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ComandaItemRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ComandaRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ZonaRepository;
import co.edu.unicauca.backend.modules.pagos_caja.entity.Abono;
import co.edu.unicauca.backend.modules.pagos_caja.repository.AbonoRepository;
import co.edu.unicauca.backend.modules.reservas.dto.request.CrearReservaRequest;
import co.edu.unicauca.backend.modules.reservas.dto.response.CancelarReservaResponse;
import co.edu.unicauca.backend.modules.reservas.dto.request.ModificarReservaRequest;
import co.edu.unicauca.backend.modules.reservas.dto.request.PreOrdenItemRequest;
import co.edu.unicauca.backend.modules.reservas.dto.response.DisponibilidadResponse;
import co.edu.unicauca.backend.modules.reservas.dto.response.ModificarReservaResponse;
import co.edu.unicauca.backend.modules.reservas.dto.response.ReservaDetalleResponse;
import co.edu.unicauca.backend.modules.reservas.dto.response.ReservaResponse;
import co.edu.unicauca.backend.modules.reservas.entity.Decoracion;
import co.edu.unicauca.backend.modules.reservas.entity.Reserva;
import co.edu.unicauca.backend.modules.reservas.mapper.ReservaMapper;
import co.edu.unicauca.backend.modules.reservas.repository.DecoracionRepository;
import co.edu.unicauca.backend.modules.reservas.repository.ReservaRepository;
import co.edu.unicauca.backend.modules.usuarios.entity.Cliente;
import co.edu.unicauca.backend.modules.usuarios.repository.ClienteRepository;
import co.edu.unicauca.backend.shared.enums.EstadoComanda;
import co.edu.unicauca.backend.shared.enums.EstadoReserva;
import co.edu.unicauca.backend.shared.enums.TipoAbono;
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
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio de negocio para la gestión de reservas del restaurante.
 *
 * <p>Actúa como orquestador central: delega la lógica de validación en
 * {@link ReservaValidador}, la consulta de disponibilidad en {@link DisponibilidadConsultador},
 * la gestión de la pre-orden en {@link PreOrdenGestor} y la construcción de mensajes de
 * WhatsApp en {@link MensajeWhatsAppBuilder}.
 *
 * <p>Responsabilidades directas:
 * <ul>
 *   <li>Consultar disponibilidad para una fecha y hora dada.</li>
 *   <li>Crear o modificar una reserva coordinando las validaciones y persistencia.</li>
 *   <li>Exponer el historial de reservas del cliente.</li>
 *   <li>Devolver las reservas futuras activas del cliente.</li>
 * </ul>
 *
 * @see ReservaValidador
 * @see DisponibilidadConsultador
 * @see PreOrdenGestor
 * @see MensajeWhatsAppBuilder
 * @see co.edu.unicauca.backend.modules.reservas.mapper.ReservaMapper
 */
@Service
@RequiredArgsConstructor
public class ReservaService {
    private static final List<EstadoReserva> ESTADOS_ACTIVOS = List.of(EstadoReserva.PENDIENTE, EstadoReserva.CONFIRMADA);
    private static final String MSG_DISPONIBILIDAD_CAMBIO = "Lo sentimos, la disponibilidad cambió. Por favor revise nuevamente.";
    private static final String MSG_FUERA_HORARIO = "Lo sentimos, no hay disponibilidad para la fecha y hora seleccionada. " + "Por favor elija otra fecha u hora.";
    private static final String MSG_ANTICIPACION_MINIMA = "La reserva debe realizarse con al menos un día de anticipación.";
    private static final LocalTime HORA_APERTURA = LocalTime.of(17, 0);
    private static final LocalTime HORA_CIERRE = LocalTime.of(22, 0);
    private static final LocalTime HORA_LIMITE_MENU_ESPECIAL = LocalTime.of(23, 0);
    private static final LocalTime HORA_LIMITE_ESTANDAR = LocalTime.of(16, 0);

    private final ReservaRepository reservaRepository;
    private final DecoracionRepository decoracionRepository;
    private final ZonaRepository zonaRepository;
    private final ClienteRepository clienteRepository;
    private final ComandaRepository comandaRepository;
    private final ComandaItemRepository comandaItemRepository;
    private final AbonoRepository abonoRepository;
    private final ReservaMapper reservaMapper;
    private final ReservaValidador reservaValidador;
    private final DisponibilidadConsultador disponibilidadConsultador;
    private final PreOrdenGestor preOrdenGestor;
    private final MensajeWhatsAppBuilder mensajeWhatsAppBuilder;

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
        return disponibilidadConsultador.consultarParaNuevaReserva(fechaHora, 0, HORA_APERTURA, HORA_CIERRE);
    }

    // -----------------------------------------------------------------------
    // CRUD - CREATE
    // -----------------------------------------------------------------------

    /**
     * Crea una nueva reserva y, si incluye pre-orden, persiste una comanda en estado
     * {@code PRE_RESERVA} con sus ítems y modificaciones de menú especial.
     *
     * @param emailCliente correo del cliente para quien se crea la reserva
     * @param request datos de la reserva a crear
     * @return {@link ReservaResponse} con los datos completos de la reserva creada
     * @throws ResourceNotFoundException si el cliente, la decoración, la zona, el producto
     *                                   o la opción de modificación no existen
     * @throws BusinessException si la fecha de llegada no es al menos un día posterior a hoy,
     *                           o si se incumple cualquier otra regla de negocio
     */
    @Transactional
    public ReservaResponse crearReserva(String emailCliente, CrearReservaRequest request) {

        // Verificar que el cliente exista en la base de datos
        Cliente cliente = clienteRepository.findByUsuario_UsuarioEmail(emailCliente)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente", "email", emailCliente));

        // Validar horarios y bloqueos administrativos para la fecha/hora solicitada
        ParDecoracionZona dz = validarYCargarDecoracionZona(request.getFechaHoraLlegada(), request.getDecoracionId(), request.getZonaId());
        Decoracion decoracion = dz.decoracion();
        Zona zona = dz.zona();

        // Validar que la reserva cumpla con las reglas de capacidad y disponibilidad
        validarDisponibilidadYCapacidad(request.getFechaHoraLlegada(), decoracion, zona, request.getNumeroPersonas(), null);

        // Determinar tipo de reserva (BASICA o ESPECIAL) según decoración y pre-orden
        TipoReserva tipo = determinarTipoReserva(decoracion, request.getPreOrden());
        EstadoReserva estado = (tipo == TipoReserva.ESPECIAL)
                ? EstadoReserva.PENDIENTE
                : EstadoReserva.CONFIRMADA;

        // La fecha de llegada debe ser mínimo 1 día de anticipación para reservas especiales
        if (!request.getFechaHoraLlegada().toLocalDate().isAfter(LocalDate.now()) && tipo == TipoReserva.ESPECIAL) {
            throw new BusinessException(ErrorCode.INVALID_STATE,
                    MSG_ANTICIPACION_MINIMA, HttpStatus.UNPROCESSABLE_ENTITY);
        }

        // Validar pre-orden
        if (request.getPreOrden() != null && !request.getPreOrden().isEmpty()) {
            preOrdenGestor.validarPreOrden(request.getPreOrden(), request.getNumeroPersonas());
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
            preOrdenGestor.persistirPreOrden(guardada, request.getPreOrden());
        }

        // Si la reserva es ESPECIAL, se requiere enviar mensaje de WhatsApp para coordinar el anticipo
        String mensajeWhatsApp;
        if (tipo == TipoReserva.ESPECIAL) {
            mensajeWhatsApp = mensajeWhatsAppBuilder.construirMensaje(guardada, MensajeWhatsAppBuilder.MSG_WA_ANTICIPO);
            return reservaMapper.toResponse(guardada, true, mensajeWhatsApp);
        }

        return reservaMapper.toResponse(guardada, false, null);
    }

    // -----------------------------------------------------------------------
    // CRUD - READ
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
    // CRUD - UPDATE
    // -----------------------------------------------------------------------

    /**
     * Modifica una reserva existente del cliente aplicando las mismas validaciones de negocio
     * que la creación y verificando la disponibilidad excluyendo la reserva actual.
     *
     * <p><b>Regla de hora límite:</b>
     * <ul>
     *   <li>Reserva actual o nueva es ESPECIAL (menú especial o decoración con costo &gt; 0):
     *       antes de las 11:00 pm del día anterior.</li>
     *   <li>Reserva BÁSICA sin ningún componente especial: antes de la 1:00 pm del mismo día.</li>
     * </ul>
     *
     * <p><b>Transiciones de tipo, estado y WhatsApp:</b>
     * <ul>
     *   <li>BASICA → BASICA: mantiene CONFIRMADA; WhatsApp solo si abono neto &gt; platos.</li>
     *   <li>BASICA → ESPECIAL: cambia a PENDIENTE; siempre requiere WhatsApp (anticipo).</li>
     *   <li>ESPECIAL → ESPECIAL: mantiene PENDIENTE; WhatsApp solo si el valor total
     *       (costo decoración + precio menú × personas) cambió.</li>
     *   <li>ESPECIAL → BASICA: cambia a CONFIRMADA; WhatsApp si abono neto &gt; valor de platos.</li>
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

        // 1. Verificar existencia y elegibilidad (ownership + estado activo)
        Reserva reserva = reservaRepository.findById(reservaId)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva", reservaId));
        reservaValidador.validarElegibilidadModificacion(reserva, emailCliente);

        // 2. Evaluar si la configuración actual o la nueva involucra menú especial o decoración con costo
        boolean anteriorEraEspecial = reserva.getReservaTipo() == TipoReserva.ESPECIAL;
        boolean nuevoTieneMenuEspecial = request.getPreOrden() != null &&
                request.getPreOrden().stream().anyMatch(i -> Boolean.TRUE.equals(i.getEsMenuEspecial()));
        boolean nuevoDecoracionTieneCosto = reservaValidador.tieneDecoracionConCosto(
                request.getDecoracionId() == null ? null
                        : decoracionRepository.findById(request.getDecoracionId()).orElse(null));

        // 3. Verificar hora límite — se usa la fecha de llegada original porque el límite se calcula sobre ella
        boolean aplicaLimiteEspecial = anteriorEraEspecial || nuevoTieneMenuEspecial || nuevoDecoracionTieneCosto;
        LocalDateTime limiteModificacion = calcularLimiteModificacion(aplicaLimiteEspecial, reserva.getReservaFechaHoraLlegada());
        if (!LocalDateTime.now().isBefore(limiteModificacion)) {
            throw new BusinessException(ErrorCode.INVALID_STATE,
                    "No es posible modificar esta reserva. Solo cancelarla", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        // 4. Validar disponibilidad, decoración y zona excluyendo la reserva actual del conteo
        ParDecoracionZona dz = validarYCargarDecoracionZona(
                request.getFechaHoraLlegada(), request.getDecoracionId(), request.getZonaId());
        Decoracion nuevaDecoracion = dz.decoracion();
        Zona nuevaZona = dz.zona();
        validarDisponibilidadYCapacidad(
                request.getFechaHoraLlegada(), nuevaDecoracion, nuevaZona, request.getNumeroPersonas(), reservaId);

        // 5. Validar pre-orden antes de modificar la reserva
        if (request.getPreOrden() != null && !request.getPreOrden().isEmpty()) {
            preOrdenGestor.validarPreOrden(request.getPreOrden(), request.getNumeroPersonas());
        }

        // 6. Determinar nuevo tipo y capturar el valorAnterior
        TipoReserva nuevoTipo = determinarTipoReserva(nuevaDecoracion, request.getPreOrden());
        boolean nuevoEsEspecial = nuevoTipo == TipoReserva.ESPECIAL;
        BigDecimal valorAnterior = (anteriorEraEspecial && nuevoEsEspecial)
                ? calcularValorEspecial(reserva.getDecoracion(),
                                        obtenerItemsPreOrden(reservaId),
                                        reserva.getReservaNumeroPersonas())
                : BigDecimal.ZERO;

        // 7. Actualizar campos, eliminar pre-orden anterior y persistir
        reserva.setZona(nuevaZona);
        reserva.setDecoracion(nuevaDecoracion);
        reserva.setReservaFechaHoraLlegada(request.getFechaHoraLlegada());
        reserva.setReservaNumeroPersonas(request.getNumeroPersonas());
        reserva.setReservaNotas(request.getNotas());
        reserva.setReservaEstado(determinarNuevoEstado(anteriorEraEspecial, nuevoEsEspecial, reserva.getReservaEstado()));
        reserva.setReservaTipo(nuevoTipo);

        preOrdenGestor.eliminarPreOrdenExistente(reservaId);
        Reserva reservaResultado = reservaRepository.save(reserva);

        // 8. Persistir nueva pre-orden si el cliente incluyó una
        if (request.getPreOrden() != null && !request.getPreOrden().isEmpty()) {
            preOrdenGestor.persistirPreOrden(reservaResultado, request.getPreOrden());
        }

        // 9. Construir notificación de WhatsApp según la transición de tipo
        String mensajeWhatsApp = construirNotificacionWhatsApp(
                anteriorEraEspecial, nuevoEsEspecial,
                valorAnterior, nuevaDecoracion,
                reservaResultado, request.getNumeroPersonas(), reservaId);

        return reservaMapper.toModificarResponse(reservaResultado, mensajeWhatsApp != null, mensajeWhatsApp);
    }

    // -----------------------------------------------------------------------
    // CRUD - DELETE (cancelación)
    // -----------------------------------------------------------------------

    /**
     * Cancela una reserva activa del cliente, actualizando su estado a
     * {@link EstadoReserva#CANCELADA}.
     *
     * <p>Reglas de redirección a WhatsApp según tipo y momento de cancelación:
     * <ul>
     *   <li><b>BÁSICA sin abono neto</b>: no requiere WhatsApp.</li>
     *   <li><b>BÁSICA con abono neto &gt; 0</b>: requiere WhatsApp para gestionar el
     *       reembolso (CA-02).</li>
     *   <li><b>ESPECIAL antes de las 16:00 del día de la reserva</b>: requiere
     *       WhatsApp para gestionar el reembolso (CA-03).</li>
     *   <li><b>ESPECIAL después/igual a las 16:00 del día de la reserva</b>: sin
     *       reembolso, no requiere WhatsApp (CA-04).</li>
     * </ul>
     *
     * <p>A diferencia de la modificación, no hay hora límite para cancelar: se puede
     * cancelar en cualquier momento mientras el estado sea {@code PENDIENTE} o
     * {@code CONFIRMADA}.
     *
     * @param reservaId    identificador de la reserva a cancelar
     * @param emailCliente email del cliente autenticado (tomado del token)
     * @return {@link CancelarReservaResponse} con el estado final y el indicador de WhatsApp
     * @throws ResourceNotFoundException si la reserva no existe
     * @throws BusinessException         si el cliente no es el propietario (403)
     *                                   o el estado no es cancelable (422)
     */
    @Transactional
    public CancelarReservaResponse cancelarReserva(Long reservaId, String emailCliente) {

        // 1. Verificar existencia de la reserva
        Reserva reserva = reservaRepository.findById(reservaId)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva", reservaId));

        // 2. Validar ownership y estado activo (PENDIENTE o CONFIRMADA)
        reservaValidador.validarElegibilidadCancelacion(reserva, emailCliente);

        // 3. Cambiar estado a CANCELADA y persistir
        reserva.setReservaEstado(EstadoReserva.CANCELADA);
        Reserva guardada = reservaRepository.save(reserva);

        // 4. Eliminar la pre-orden asociada si existe (la cancelación invalida el pedido anticipado)
        preOrdenGestor.eliminarPreOrdenExistente(reservaId);

        // 5. Determinar política de reembolso según tipo y momento de cancelación
        boolean esEspecial = reserva.getReservaTipo() == TipoReserva.ESPECIAL;
        boolean requiereWhatsApp;
        
        if (esEspecial) {
            // ESPECIAL: el límite de 16:00 del día de la reserva define si hay reembolso
            LocalDateTime limite16h = reserva.getReservaFechaHoraLlegada()
                    .toLocalDate().atTime(HORA_LIMITE_ESTANDAR);
            requiereWhatsApp = LocalDateTime.now().isBefore(limite16h);
        } else {
            // BÁSICA: depende de si hay abono neto pendiente de reembolso
            requiereWhatsApp = calcularAbonoNeto(reservaId).compareTo(BigDecimal.ZERO) > 0;
        }

        String mensajeWhatsApp = requiereWhatsApp
                ? mensajeWhatsAppBuilder.construirMensaje(guardada,
                        MensajeWhatsAppBuilder.MSG_WA_CANCELACION_REEMBOLSO)
                : null;

        return reservaMapper.toCancelarResponse(guardada, requiereWhatsApp, mensajeWhatsApp);
    }

    // -----------------------------------------------------------------------
    // Lógica interna de pre-orden
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

    // -----------------------------------------------------------------------
    // Cálculos financieros internos
    // -----------------------------------------------------------------------

    /**
     * Calcula el abono neto de una reserva: suma los abonos de tipo ANTICIPO
     * y resta los de cualquier otro tipo.
     *
     * @param reservaId identificador de la reserva
     * @return importe neto de los abonos; {@link BigDecimal#ZERO} si no hay abonos
     */
    private BigDecimal calcularAbonoNeto(Long reservaId) {
        List<Abono> abonos = abonoRepository.findByReserva_ReservaIdOrderByAbonoFechaHoraAsc(reservaId);
        return abonos.stream()
                .map(a -> TipoAbono.ANTICIPO.equals(a.getAbonoTipo())
                          ? a.getAbonoMonto() : a.getAbonoMonto().negate())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Calcula el valor total de los platos de la pre-orden de una reserva
     * (precio por ítem × cantidad, sumado sobre todos los ítems).
     *
     * @param reservaId identificador de la reserva
     * @return total de los platos; {@link BigDecimal#ZERO} si no hay pre-orden
     */
    private BigDecimal calcularTotalPlatos(Long reservaId) {
        return obtenerItemsPreOrden(reservaId).stream()
                .map(i -> i.getComandaItemPrecio().multiply(BigDecimal.valueOf(i.getComandaItemCantidad())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // -----------------------------------------------------------------------
    // Validaciones privadas
    // -----------------------------------------------------------------------

    /** Par de resultados de {@link #validarYCargarDecoracionZona}. */
    private record ParDecoracionZona(Decoracion decoracion, Zona zona) {}

    /**
     * Valida horario y bloqueos para {@code fechaHora}, luego carga y valida la decoración y
     * la zona indicadas por sus IDs. Si ambas están presentes, verifica su compatibilidad.
     *
     * @param fechaHora    fecha y hora de llegada a validar
     * @param decoracionId ID de decoración solicitada; puede ser {@code null}
     * @param zonaId       ID de zona solicitada; puede ser {@code null}
     * @return par con las entidades cargadas (cada campo puede ser {@code null})
     * @throws BusinessException         si el horario no es válido o existe un bloqueo
     * @throws ResourceNotFoundException si la decoración o la zona no existen
     */
    private ParDecoracionZona validarYCargarDecoracionZona(
            LocalDateTime fechaHora, Long decoracionId, Long zonaId) {

        if (!reservaValidador.esHorarioValido(fechaHora, HORA_APERTURA, HORA_CIERRE)) {
            throw new BusinessException(ErrorCode.INVALID_STATE, MSG_FUERA_HORARIO, HttpStatus.UNPROCESSABLE_ENTITY);
        }
        if (reservaValidador.estaBloqueda(fechaHora)) {
            throw new BusinessException(ErrorCode.INVALID_STATE, MSG_FUERA_HORARIO, HttpStatus.UNPROCESSABLE_ENTITY);
        }

        Decoracion decoracion = null;
        if (decoracionId != null) {
            final Long decId = decoracionId;
            decoracion = decoracionRepository.findById(decId)
                    .orElseThrow(() -> new ResourceNotFoundException("Decoracion", decId));
        }

        Zona zona = null;
        if (zonaId != null) {
            final Long zId = zonaId;
            zona = zonaRepository.findById(zId)
                    .orElseThrow(() -> new ResourceNotFoundException("Zona", zId));
        }

        if (decoracion != null && zona != null) {
            reservaValidador.validarCompatibilidadDecoracionZona(decoracion, zona.getZonaId());
        }

        return new ParDecoracionZona(decoracion, zona);
    }

    /**
     * Verifica que haya disponibilidad para {@code fechaHora} y que la decoración y zona
     * elegidas estén libres y tengan capacidad para {@code numeroPersonas}.
     *
     * <p>Cuando {@code excludeReservaId} es {@code null} (creación), usa las queries normales
     * del repositorio. Cuando tiene valor (modificación), usa las variantes {@code *Excluyendo}
     * para no contar la reserva que se está editando.
     *
     * @param fechaHora        nueva fecha y hora de llegada
     * @param decoracion       decoración elegida; puede ser {@code null}
     * @param zona             zona elegida; puede ser {@code null}
     * @param numeroPersonas   comensales de la reserva
     * @param excludeReservaId ID a excluir de los conteos; {@code null} en creación
     * @throws BusinessException si la disponibilidad cambió o no hay capacidad suficiente
     */
    private void validarDisponibilidadYCapacidad(
            LocalDateTime fechaHora, Decoracion decoracion, Zona zona,
            int numeroPersonas, Long excludeReservaId) {

        // Consultar disponibilidad según el contexto (creación o modificación)
        DisponibilidadResponse disponibilidad = excludeReservaId == null
                ? consultarDisponibilidad(fechaHora)
                : consultarDisponibilidadParaModificacion(fechaHora, excludeReservaId);

        // Verificar que siga habiendo disponibilidad para la fecha/hora solicitada
        if (!disponibilidad.getDisponible()) {
            throw new BusinessException(ErrorCode.INVALID_STATE,
                    MSG_DISPONIBILIDAD_CAMBIO, HttpStatus.UNPROCESSABLE_ENTITY);
        }

        // Si se eligió decoración, verificar que siga estando disponible
        if (decoracion != null) {
            final Long decId = decoracion.getDecoracionId();
            boolean decoracionLibre = disponibilidad.getDecoraciones().stream()
                    .anyMatch(d -> d.getDecoracionId().equals(decId));
            if (!decoracionLibre) {
                throw new BusinessException(ErrorCode.INVALID_STATE,
                        MSG_DISPONIBILIDAD_CAMBIO, HttpStatus.UNPROCESSABLE_ENTITY);
            }
        }


        LocalDateTime inicioDia = fechaHora.toLocalDate().atStartOfDay();
        LocalDateTime finDia    = fechaHora.toLocalDate().atTime(23, 59, 59);

        if (zona != null) {
            final Long zId = zona.getZonaId();

            // Verificar que la zona siga estando disponible
            boolean zonaLibre = disponibilidad.getZonas().stream()
                    .anyMatch(z -> z.getZonaId().equals(zId));
            if (!zonaLibre) {
                throw new BusinessException(ErrorCode.INVALID_STATE,
                        MSG_DISPONIBILIDAD_CAMBIO, HttpStatus.UNPROCESSABLE_ENTITY);
            }

            // Verificar que la zona tenga capacidad para el número de personas solicitado
            // Si se modifica se excluye la reserva propia
            int personasExistentes = excludeReservaId == null
                    ? reservaRepository.sumPersonasByZonaEnDia(zId, inicioDia, finDia, ESTADOS_ACTIVOS)
                    : reservaRepository.sumPersonasByZonaEnDiaExcluyendo(zId, inicioDia, finDia, ESTADOS_ACTIVOS, excludeReservaId);

            // Si el numero actual de la zona más el número de personas de la reserva que se está creando/modificando supera la capacidad, no se permite el cambio
            if (personasExistentes + numeroPersonas > zona.getZonaCapacidadPersonas()) {
                throw new BusinessException(ErrorCode.INVALID_STATE,
                        "La zona seleccionada no tiene capacidad suficiente para " +
                        numeroPersonas + " personas en ese día.",
                        HttpStatus.UNPROCESSABLE_ENTITY);
            }
        } else {
            // Verificar que exista al menos una zona con capacidad suficiente para el número de personas solicitado
            boolean hayZonaConCapacidad = disponibilidad.getZonas().stream()
                    .anyMatch(z -> {
                        int ocupadas = excludeReservaId == null
                                ? reservaRepository.sumPersonasByZonaEnDia(
                                        z.getZonaId(), inicioDia, finDia, ESTADOS_ACTIVOS)
                                : reservaRepository.sumPersonasByZonaEnDiaExcluyendo(
                                        z.getZonaId(), inicioDia, finDia, ESTADOS_ACTIVOS, excludeReservaId);
                        return (z.getCapacidad() - ocupadas) >= numeroPersonas;
                    });
            if (!hayZonaConCapacidad) {
                throw new BusinessException(ErrorCode.INVALID_STATE,
                        "No hay zonas con capacidad suficiente para " +
                        numeroPersonas + " personas en ese día.",
                        HttpStatus.UNPROCESSABLE_ENTITY);
            }
        }
    }

    /**
     * Calcula la hora límite hasta la cual se puede modificar o cancelar una reserva.
     *
     * <ul>
     *   <li>Si aplica límite especial (reserva actual o nueva involucra ESPECIAL):
     *       antes de las {@link #HORA_LIMITE_MENU_ESPECIAL} del día <em>anterior</em> a la reserva.</li>
     *   <li>Si es puramente BASICA: antes de las {@link #HORA_LIMITE_ESTANDAR} del mismo día.</li>
     * </ul>
     *
     * @param aplicaLimiteEspecial {@code true} si la reserva actual o la nueva configuración es ESPECIAL
     * @param fechaHoraLlegada     fecha y hora de llegada de la reserva
     * @return instante límite hasta el cual se permite la operación
     */
    private LocalDateTime calcularLimiteModificacion(boolean aplicaLimiteEspecial, LocalDateTime fechaHoraLlegada) {
        // Las reservas especiales requieren coordinación extra: el límite es el día anterior
        if (aplicaLimiteEspecial) {
            return fechaHoraLlegada.toLocalDate().minusDays(1).atTime(HORA_LIMITE_MENU_ESPECIAL);
        }
        // Las reservas básicas pueden modificarse el mismo día hasta la hora límite estándar
        return fechaHoraLlegada.toLocalDate().atTime(HORA_LIMITE_ESTANDAR);
    }

    /**
     * Calcula el valor monetario total de los componentes especiales de una reserva:
     * costo de la decoración más el precio del menú especial multiplicado por el número de personas.
     *
     * @param decoracion decoración asignada a la reserva; puede ser {@code null}
     * @param items      ítems de la pre-orden a evaluar
     * @param personas   número de personas para escalar el precio del menú especial
     * @return suma del costo de decoración más precio × personas de los ítems especiales
     */
    private BigDecimal calcularValorEspecial(Decoracion decoracion, List<ComandaItem> items, int personas) {
        BigDecimal valor = BigDecimal.ZERO;
        // Agregar el costo adicional de la decoración si tiene uno definido
        if (decoracion != null && decoracion.getDecoracionCostoAdicional() != null) {
            valor = valor.add(decoracion.getDecoracionCostoAdicional());
        }
        // Agregar precio × personas solo para ítems de menú especial
        for (ComandaItem item : items) {
            if (Boolean.TRUE.equals(item.getProducto().getMenuEspecial())) {
                valor = valor.add(item.getComandaItemPrecio().multiply(BigDecimal.valueOf(personas)));
            }
        }
        return valor;
    }

    /**
     * Determina el nuevo estado de la reserva tras una modificación según la transición de tipo.
     *
     * <ul>
     *   <li>BASICA → ESPECIAL: pasa a {@code PENDIENTE} (el restaurante debe confirmar el servicio).</li>
     *   <li>ESPECIAL → BASICA: pasa a {@code CONFIRMADA} (no requiere preparación especial).</li>
     *   <li>Sin cambio de categoría: conserva el estado actual.</li>
     * </ul>
     *
     * @param anteriorEraEspecial {@code true} si la reserva era ESPECIAL antes de la modificación
     * @param nuevoEsEspecial     {@code true} si la reserva resulta ESPECIAL tras la modificación
     * @param estadoActual        estado actual de la reserva
     * @return estado resultante tras la transición
     */
    private EstadoReserva determinarNuevoEstado(boolean anteriorEraEspecial, boolean nuevoEsEspecial,
                                                 EstadoReserva estadoActual) {
        // BASICA → ESPECIAL: requiere confirmación manual del restaurante antes de proceder
        if (!anteriorEraEspecial && nuevoEsEspecial) return EstadoReserva.PENDIENTE;
        // ESPECIAL → BASICA: se confirma automáticamente al eliminar el componente especial
        if (anteriorEraEspecial && !nuevoEsEspecial) return EstadoReserva.CONFIRMADA;
        // Sin cambio de categoría: BASICA→BASICA mantiene CONFIRMADA, ESPECIAL→ESPECIAL mantiene PENDIENTE
        return estadoActual;
    }

    /**
     * Decide si corresponde enviar una notificación de WhatsApp al cliente y construye
     * el mensaje según la transición de tipo ocurrida en la modificación.
     *
     * <ul>
     *   <li>BASICA → ESPECIAL: siempre notifica (el cliente debe abonar anticipo).</li>
     *   <li>ESPECIAL → ESPECIAL: notifica solo si el valor total (decoración + menú × personas) cambió.</li>
     *   <li>ESPECIAL → BASICA: notifica si el abono neto supera el total de platos (posible devolución).</li>
     *   <li>BASICA → BASICA: notifica si el abono neto supera el total de platos (posible ajuste).</li>
     * </ul>
     *
     * @param anteriorEraEspecial  {@code true} si la reserva era ESPECIAL antes de la modificación
     * @param nuevoEsEspecial      {@code true} si la reserva resulta ESPECIAL tras la modificación
     * @param valorAnterior        valor especial calculado antes de eliminar la pre-orden original
     * @param nuevaDecoracion      decoración resultante de la reserva ya modificada
     * @param reservaResultado     entidad de reserva guardada con los datos actualizados
     * @param nuevasPersonas       número de personas actualizado
     * @param reservaIdOriginal    ID de la reserva (para calcular el abono neto histórico)
     * @return mensaje precompuesto para WhatsApp, o {@code null} si no aplica notificación
     */
    private String construirNotificacionWhatsApp(
            boolean anteriorEraEspecial, boolean nuevoEsEspecial,
            BigDecimal valorAnterior, Decoracion nuevaDecoracion,
            Reserva reservaResultado, int nuevasPersonas, Long reservaIdOriginal) {

        // BASICA → ESPECIAL: el cliente debe abonar un anticipo para confirmar el servicio especial
        if (!anteriorEraEspecial && nuevoEsEspecial) {
            return mensajeWhatsAppBuilder.construirMensaje(reservaResultado, MensajeWhatsAppBuilder.MSG_WA_ANTICIPO);
        }

        // ESPECIAL → ESPECIAL: notificar solo si el costo total cambió
        if (anteriorEraEspecial && nuevoEsEspecial) {
            BigDecimal valorNuevo = calcularValorEspecial(
                    nuevaDecoracion,
                    obtenerItemsPreOrden(reservaResultado.getReservaId()),
                    nuevasPersonas);
            if (valorAnterior.compareTo(valorNuevo) != 0) {
                return mensajeWhatsAppBuilder.construirMensaje(reservaResultado, MensajeWhatsAppBuilder.MSG_WA_CAMBIO_ESPECIAL);
            }
            return null;
        }

        // ESPECIAL → BASICA: si hay abono neto que supera los platos, puede corresponder devolución
        if (anteriorEraEspecial) {
            BigDecimal totalAbonos = calcularAbonoNeto(reservaIdOriginal);
            BigDecimal totalPlatos = calcularTotalPlatos(reservaResultado.getReservaId());
            if (totalAbonos.compareTo(BigDecimal.ZERO) > 0 && totalPlatos.compareTo(totalAbonos) < 0) {
                return mensajeWhatsAppBuilder.construirMensaje(reservaResultado, MensajeWhatsAppBuilder.MSG_WA_CAMBIO_ESPECIAL);
            }
            return null;
        }

        // BASICA → BASICA: si hay abono neto que supera los platos, el cliente puede pedir ajuste
        BigDecimal totalAbonos = calcularAbonoNeto(reservaIdOriginal);
        BigDecimal totalPlatos = calcularTotalPlatos(reservaResultado.getReservaId());
        if (totalAbonos.compareTo(BigDecimal.ZERO) > 0 && totalPlatos.compareTo(totalAbonos) < 0) {
            return mensajeWhatsAppBuilder.construirMensaje(reservaResultado, MensajeWhatsAppBuilder.MSG_WA_ABONO_AJUSTE);
        }
        return null;
    }

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
        boolean tieneDecoracionConCosto = reservaValidador.tieneDecoracionConCosto(decoracion);

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
     * <p>Lógica idéntica a {@link #consultarDisponibilidad(LocalDateTime)}, pero excluye
     * la reserva actual de los cálculos de ocupación de zona y decoración.
     *
     * @param fechaHora        nueva fecha y hora de llegada solicitada
     * @param excludeReservaId ID de la reserva siendo modificada, excluida de los conteos
     * @return {@link DisponibilidadResponse} con zonas y decoraciones disponibles
     */
    private DisponibilidadResponse consultarDisponibilidadParaModificacion(
            LocalDateTime fechaHora, Long excludeReservaId) {
        return disponibilidadConsultador.consultarParaModificacion(fechaHora, 0, excludeReservaId, HORA_APERTURA, HORA_CIERRE);
    }
}
