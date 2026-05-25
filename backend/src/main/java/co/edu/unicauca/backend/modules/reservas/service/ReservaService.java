package co.edu.unicauca.backend.modules.reservas.service;

import co.edu.unicauca.backend.modules.mesas_comandas.entity.ComandaItem;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Zona;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ComandaItemRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ComandaRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ZonaRepository;
import co.edu.unicauca.backend.modules.notificaciones.dto.ws.ReservaActualizadaWsMessage;
import co.edu.unicauca.backend.modules.notificaciones.service.NotificacionWsPublisher;
import co.edu.unicauca.backend.modules.pagos_caja.entity.Abono;
import co.edu.unicauca.backend.modules.pagos_caja.repository.AbonoRepository;
import co.edu.unicauca.backend.modules.reservas.dto.request.CrearReservaRequest;
import co.edu.unicauca.backend.modules.reservas.dto.response.CancelarReservaResponse;
import co.edu.unicauca.backend.modules.reservas.dto.response.ConfirmarReservaResponse;
import co.edu.unicauca.backend.modules.reservas.dto.request.ModificarReservaRequest;
import co.edu.unicauca.backend.modules.reservas.dto.request.PreOrdenItemRequest;
import co.edu.unicauca.backend.modules.reservas.dto.request.RegistrarAbonoRequest;
import co.edu.unicauca.backend.modules.reservas.dto.response.DisponibilidadResponse;
import co.edu.unicauca.backend.modules.reservas.dto.response.MarcarInasistenciaResponse;
import co.edu.unicauca.backend.modules.reservas.dto.response.ModificarReservaResponse;
import co.edu.unicauca.backend.modules.reservas.dto.response.RegistrarAbonoResponse;
import co.edu.unicauca.backend.modules.reservas.dto.response.ReservaDetalleResponse;
import co.edu.unicauca.backend.modules.reservas.dto.response.ReservaResponse;
import co.edu.unicauca.backend.modules.reservas.dto.response.ResumenPagoResponse;
import co.edu.unicauca.backend.modules.reservas.entity.Decoracion;
import co.edu.unicauca.backend.modules.reservas.entity.Reserva;
import co.edu.unicauca.backend.modules.reservas.mapper.AbonoMapper;
import co.edu.unicauca.backend.modules.reservas.mapper.ReservaMapper;
import co.edu.unicauca.backend.modules.reservas.repository.DecoracionRepository;
import co.edu.unicauca.backend.modules.reservas.repository.ReservaRepository;
import co.edu.unicauca.backend.modules.usuarios.entity.Cliente;
import co.edu.unicauca.backend.modules.usuarios.entity.Empleado;
import co.edu.unicauca.backend.modules.usuarios.repository.ClienteRepository;
import co.edu.unicauca.backend.modules.usuarios.repository.EmpleadoRepository;
import co.edu.unicauca.backend.shared.enums.EstadoComanda;
import co.edu.unicauca.backend.shared.enums.EstadoReserva;
import co.edu.unicauca.backend.shared.enums.TipoAbono;
import co.edu.unicauca.backend.shared.enums.TipoReserva;
import co.edu.unicauca.backend.shared.exception.BusinessException;
import co.edu.unicauca.backend.shared.exception.ErrorCode;
import co.edu.unicauca.backend.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
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
    private final EmpleadoRepository empleadoRepository;
    private final ReservaMapper reservaMapper;
    private final AbonoMapper abonoMapper;
    private final ReservaValidador reservaValidador;
    private final DisponibilidadConsultador disponibilidadConsultador;
    private final PreOrdenGestor preOrdenGestor;
    private final MensajeWhatsAppBuilder mensajeWhatsAppBuilder;
    private final NotificacionWsPublisher wsPublisher;

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

        // Publicar evento WS si la reserva está activa
        if (guardada.getReservaEstado() == EstadoReserva.CONFIRMADA
                || guardada.getReservaEstado() == EstadoReserva.PENDIENTE) {
            publicarCambioReserva(guardada, "CREADA");
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
                                        obtenerItemsPreOrden(reservaId))
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
                reservaResultado, reservaId);

        // Publicar evento WS si la reserva sigue activa
        if (reservaResultado.getReservaEstado() == EstadoReserva.CONFIRMADA
                || reservaResultado.getReservaEstado() == EstadoReserva.PENDIENTE) {
            publicarCambioReserva(reservaResultado, "MODIFICADA");
        }

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
     *       reembolso.</li>
     *   <li><b>ESPECIAL antes de las 16:00 del día de la reserva</b>: requiere
     *       WhatsApp para gestionar el reembolso}.</li>
     *   <li><b>ESPECIAL después/igual a las 16:00 del día de la reserva</b>: sin
     *       reembolso, no requiere WhatsApp.</li>
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

        // 6. Publicar evento WebSocket para refrescar los tableros de reservas en tiempo real
        publicarCambioReserva(guardada, "CANCELADA");

        return reservaMapper.toCancelarResponse(guardada, requiereWhatsApp, mensajeWhatsApp);
    }

    // -----------------------------------------------------------------------
    // Confirmar reserva
    // -----------------------------------------------------------------------

    /**
     * Confirma una reserva especial pendiente, cambiando su estado a {@code CONFIRMADA}.
     *
     * <p>Operación destinada al cajero: una reserva {@code ESPECIAL} en estado {@code PENDIENTE}
     * pasa a {@code CONFIRMADA}. No exige el registro de un abono previo; el anticipo se gestiona
     * de forma independiente. No aplica validación de ownership: el cajero puede confirmar
     * cualquier reserva.
     *
     * @param reservaId identificador de la reserva a confirmar
     * @return {@link ConfirmarReservaResponse} con el estado resultante de la reserva
     * @throws ResourceNotFoundException si la reserva no existe (404)
     * @throws BusinessException         si la reserva no es {@code ESPECIAL} o no está en estado
     *                                   {@code PENDIENTE} (código {@code INVALID_STATE}, status 422)
     */
    @Transactional
    public ConfirmarReservaResponse confirmarReserva(Long reservaId) {

        // 1. Verificar existencia de la reserva
        Reserva reserva = reservaRepository.findById(reservaId)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva", reservaId));

        // 2. Validar que sea una reserva ESPECIAL en estado PENDIENTE
        reservaValidador.validarElegibilidadConfirmacion(reserva);

        // 3. Cambiar estado a CONFIRMADA y persistir
        reserva.setReservaEstado(EstadoReserva.CONFIRMADA);
        Reserva guardada = reservaRepository.save(reserva);

        // 4. Publicar evento WebSocket para refrescar los tableros de reservas en tiempo real
        publicarCambioReserva(guardada, "CONFIRMADA");

        return reservaMapper.toConfirmarResponse(guardada);
    }

    // -----------------------------------------------------------------------
    // Marcar inasistencia
    // -----------------------------------------------------------------------

    /**
     * Marca una reserva confirmada como inasistencia tras el periodo de tolerancia de 30 minutos.
     *
     * <p>Este cambio es irreversible. Los recursos (zona y decoración) quedan liberados
     * automáticamente al cambiar el estado, ya que solo se cuentan reservas {@code PENDIENTE}
     * o {@code CONFIRMADA} en los cálculos de disponibilidad.
     *
     * <p><strong>Restricción de fecha para MESERO:</strong> Un mesero solo puede marcar
     * inasistencia de reservas del día actual. Los administradores pueden marcar
     * inasistencia de cualquier fecha (pasada, presente o futura).
     *
     * @param reservaId      identificador de la reserva a marcar como inasistencia
     * @param authentication contexto de autenticación con roles del usuario
     * @return {@link MarcarInasistenciaResponse} con confirmación y recursos liberados
     * @throws ResourceNotFoundException si la reserva no existe
     * @throws BusinessException         si la reserva no es {@code CONFIRMADA}, no han
     *                                   transcurrido 30 minutos, o (para MESERO) la reserva
     *                                   no es del día actual (código {@code INVALID_STATE}, status 422)
     */
    @Transactional
    public MarcarInasistenciaResponse marcarInasistencia(Long reservaId, Authentication authentication) {

        // 1. Buscar reserva
        Reserva reserva = reservaRepository.findById(reservaId)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva", reservaId));

        // 2. Determinar si el usuario es MESERO (vs ADMIN)
        boolean esMesero = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals("ROLE_MESERO"));

        // 3. Validar elegibilidad (estado CONFIRMADA + 30 minutos transcurridos + fecha si es MESERO)
        reservaValidador.validarElegibilidadInasistencia(reserva, esMesero);

        // 3. Capturar recursos antes de cambiar estado (para incluir en respuesta)
        String zonaLiberada = reserva.getZona() != null
                ? reserva.getZona().getZonaNombre()
                : null;
        String decoracionLiberada = reserva.getDecoracion() != null
                ? reserva.getDecoracion().getDecoracionNombre()
                : null;

        // 4. Cambiar estado a INASISTENCIA
        reserva.setReservaEstado(EstadoReserva.INASISTENCIA);
        Reserva guardada = reservaRepository.save(reserva);

        // 5. Eliminar pre-orden asociada (libera productos conceptuales del inventario)
        preOrdenGestor.eliminarPreOrdenExistente(reservaId);

        // 6. Publicar evento WebSocket para actualizar listado de meseros
        publicarCambioReserva(guardada, "INASISTENCIA");

        // 7. Construir respuesta de confirmación
        return MarcarInasistenciaResponse.builder()
                .reservaId(reservaId)
                .estado(EstadoReserva.INASISTENCIA.name())
                .zonaLiberada(zonaLiberada)
                .decoracionLiberada(decoracionLiberada)
                .build();
    }

    // -----------------------------------------------------------------------
    // Abonos: resumen de pago y registro de anticipos/devoluciones
    // -----------------------------------------------------------------------

    /**
     * Devuelve el resumen financiero de una reserva: valor total, anticipos, devoluciones
     * y saldos pendientes.
     *
     * @param reservaId identificador de la reserva
     * @return {@link ResumenPagoResponse} con los importes consolidados de la reserva
     * @throws ResourceNotFoundException si la reserva no existe
     */
    @Transactional(readOnly = true)
    public ResumenPagoResponse obtenerResumenPago(Long reservaId) {

        // Lectura sin bloqueo: el resumen no modifica datos
        Reserva reserva = reservaRepository.findById(reservaId)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva", reservaId));

        DatosResumenPago d = calcularDatosResumenPago(reserva);
        return abonoMapper.toResumenPago(reserva, d.totalReserva(), d.totalAnticipado(), d.totalDevuelto());
    }

    /**
     * Registra un anticipo o devolución sobre una reserva y devuelve el resumen financiero
     * actualizado.
     *
     * <p>Una devolución que salda el neto abonado transiciona la reserva a {@code DEVUELTA}.
     * Tras persistir el movimiento se publica un evento WebSocket cuyo tipo coincide con el
     * tipo de abono registrado.
     *
     * @param reservaId   identificador de la reserva
     * @param request     datos del movimiento: tipo, monto, método y fecha
     * @param emailCajero correo del cajero que registra el movimiento
     * @return {@link RegistrarAbonoResponse} con el abono creado y el resumen actualizado
     * @throws ResourceNotFoundException si la reserva o el cajero no existen
     * @throws BusinessException         si el estado, la fecha o el monto incumplen las reglas de negocio
     */
    @Transactional
    public RegistrarAbonoResponse registrarAbono(Long reservaId, RegistrarAbonoRequest request, String emailCajero) {

        // Bloqueo pesimista para serializar registros concurrentes de abonos sobre la misma reserva
        Reserva reserva = reservaRepository.findByIdForUpdate(reservaId)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva", reservaId));

        Empleado cajero = empleadoRepository.findByUsuario_UsuarioEmail(emailCajero)
                .orElseThrow(() -> new ResourceNotFoundException("Empleado", "email", emailCajero));

        reservaValidador.validarElegibilidadAbono(reserva, request.getTipo());
        reservaValidador.validarFechaAbono(request.getFechaHora(), reserva.getReservaFechaCreacion());

        DatosResumenPago d = calcularDatosResumenPago(reserva);
        if (request.getTipo() == TipoAbono.ANTICIPO) {
            reservaValidador.validarMontoAnticipo(request.getMonto(), d.totalReserva(), d.netoAbonado());
        } else {
            reservaValidador.validarMontoDevolucion(request.getMonto(), d.netoAbonado());
        }

        Abono abono = abonoMapper.toEntity(request, reserva, cajero);
        Abono guardado = abonoRepository.save(abono);

        // Una devolución que deja el neto en cero salda por completo la reserva: pasa a DEVUELTA
        if (request.getTipo() == TipoAbono.DEVOLUCION
                && d.netoAbonado().subtract(request.getMonto()).compareTo(BigDecimal.ZERO) == 0) {
            reserva.setReservaEstado(EstadoReserva.DEVUELTA);
            reservaRepository.save(reserva);
        }

        publicarCambioReserva(reserva, request.getTipo().name());

        DatosResumenPago dActualizado = calcularDatosResumenPago(reserva);
        ResumenPagoResponse resumen = abonoMapper.toResumenPago(reserva,
                dActualizado.totalReserva(), dActualizado.totalAnticipado(), dActualizado.totalDevuelto());
        return abonoMapper.toRegistrarResponse(guardado, resumen);
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
        // Aplana los ítems de todas las comandas PRE_RESERVA (split por estación: COCINA + BARRA)
        return comandaRepository
                .findByReserva_ReservaIdAndComandaEstado(reservaId, EstadoComanda.PRE_RESERVA)
                .stream()
                .flatMap(c -> comandaItemRepository.findByComanda_ComandaId(c.getComandaId()).stream())
                .toList();
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

    /**
     * Calcula el valor estimado total de la reserva sin doble conteo: para reservas
     * ESPECIAL incluye la decoración y el menú especial escalado por su cantidad más los
     * platos a la carta; para BASICA equivale al total de la pre-orden de platos.
     *
     * @param reserva reserva cuyo total se calcula
     * @param items   ítems de la pre-orden de la reserva
     * @return valor total estimado de la reserva
     */
    private BigDecimal calcularTotalReserva(Reserva reserva, List<ComandaItem> items) {
        BigDecimal especial = calcularValorEspecial(reserva.getDecoracion(), items);
        // Los ítems de menú especial ya están contemplados en calcularValorEspecial; aquí se excluyen para no duplicarlos
        BigDecimal platosNoEspeciales = items.stream()
                .filter(i -> !Boolean.TRUE.equals(i.getProducto().getMenuEspecial()))
                .map(i -> i.getComandaItemPrecio().multiply(BigDecimal.valueOf(i.getComandaItemCantidad())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return especial.add(platosNoEspeciales);
    }

    /** Agrupa los importes financieros de una reserva para validación y resumen. */
    private record DatosResumenPago(BigDecimal totalReserva, BigDecimal totalAnticipado,
                                    BigDecimal totalDevuelto, BigDecimal netoAbonado) {}

    /**
     * Consolida los importes financieros de una reserva: total estimado, anticipos acumulados,
     * devoluciones acumuladas y neto abonado.
     *
     * @param reserva reserva cuyos importes se consolidan
     * @return {@link DatosResumenPago} con los totales calculados
     */
    private DatosResumenPago calcularDatosResumenPago(Reserva reserva) {
        List<ComandaItem> items = obtenerItemsPreOrden(reserva.getReservaId());
        BigDecimal totalReserva = calcularTotalReserva(reserva, items);
        List<Abono> abonos = abonoRepository.findByReserva_ReservaIdOrderByAbonoFechaHoraAsc(reserva.getReservaId());
        BigDecimal totalAnticipado = abonos.stream().filter(a -> a.getAbonoTipo() == TipoAbono.ANTICIPO)
                .map(Abono::getAbonoMonto).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalDevuelto = abonos.stream().filter(a -> a.getAbonoTipo() == TipoAbono.DEVOLUCION)
                .map(Abono::getAbonoMonto).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new DatosResumenPago(totalReserva, totalAnticipado, totalDevuelto,
                totalAnticipado.subtract(totalDevuelto));
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
     * costo de la decoración más el precio del menú especial multiplicado por la cantidad
     * elegida en el propio ítem.
     *
     * @param decoracion decoración asignada a la reserva; puede ser {@code null}
     * @param items      ítems de la pre-orden a evaluar
     * @return suma del costo de decoración más precio × cantidad de los ítems especiales
     */
    private BigDecimal calcularValorEspecial(Decoracion decoracion, List<ComandaItem> items) {
        BigDecimal valor = BigDecimal.ZERO;
        // Agregar el costo adicional de la decoración si tiene uno definido
        if (decoracion != null && decoracion.getDecoracionCostoAdicional() != null) {
            valor = valor.add(decoracion.getDecoracionCostoAdicional());
        }
        // Agregar precio × cantidad elegida solo para ítems de menú especial
        for (ComandaItem item : items) {
            if (Boolean.TRUE.equals(item.getProducto().getMenuEspecial())) {
                valor = valor.add(item.getComandaItemPrecio()
                        .multiply(BigDecimal.valueOf(item.getComandaItemCantidad())));
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
     *   <li>ESPECIAL → ESPECIAL: notifica solo si el valor total (decoración + menú × cantidad) cambió.</li>
     *   <li>ESPECIAL → BASICA: notifica si el abono neto supera el total de platos (posible devolución).</li>
     *   <li>BASICA → BASICA: notifica si el abono neto supera el total de platos (posible ajuste).</li>
     * </ul>
     *
     * @param anteriorEraEspecial  {@code true} si la reserva era ESPECIAL antes de la modificación
     * @param nuevoEsEspecial      {@code true} si la reserva resulta ESPECIAL tras la modificación
     * @param valorAnterior        valor especial calculado antes de eliminar la pre-orden original
     * @param nuevaDecoracion      decoración resultante de la reserva ya modificada
     * @param reservaResultado     entidad de reserva guardada con los datos actualizados
     * @param reservaIdOriginal    ID de la reserva (para calcular el abono neto histórico)
     * @return mensaje precompuesto para WhatsApp, o {@code null} si no aplica notificación
     */
    private String construirNotificacionWhatsApp(
            boolean anteriorEraEspecial, boolean nuevoEsEspecial,
            BigDecimal valorAnterior, Decoracion nuevaDecoracion,
            Reserva reservaResultado, Long reservaIdOriginal) {

        // BASICA → ESPECIAL: el cliente debe abonar un anticipo para confirmar el servicio especial
        if (!anteriorEraEspecial && nuevoEsEspecial) {
            return mensajeWhatsAppBuilder.construirMensaje(reservaResultado, MensajeWhatsAppBuilder.MSG_WA_ANTICIPO);
        }

        // ESPECIAL → ESPECIAL: notificar solo si el costo total cambió
        if (anteriorEraEspecial && nuevoEsEspecial) {
            BigDecimal valorNuevo = calcularValorEspecial(
                    nuevaDecoracion,
                    obtenerItemsPreOrden(reservaResultado.getReservaId()));
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

    /**
     * Publica un evento WebSocket de cambio en reserva activa.
     *
     * @param reserva     reserva que cambió
     * @param tipoEvento  tipo de evento ({@code CREADA}, {@code MODIFICADA}, {@code CONFIRMADA},
     *                    {@code CANCELADA} o {@code INASISTENCIA})
     */
    private void publicarCambioReserva(Reserva reserva, String tipoEvento) {
        String horaLlegada = reserva.getReservaFechaHoraLlegada()
                .format(DateTimeFormatter.ofPattern("HH:mm"));
        String zonaNombre = reserva.getZona() != null
                ? reserva.getZona().getZonaNombre()
                : null;

        ReservaActualizadaWsMessage mensaje = ReservaActualizadaWsMessage.builder()
                .reservaId(reserva.getReservaId())
                .tipoEvento(tipoEvento)
                .clienteNombre(reserva.getCliente().getClienteNombre())
                .horaLlegada(horaLlegada)
                .zonaNombre(zonaNombre)
                .build();

        wsPublisher.publicarReservaActualizada(mensaje);
    }
}
