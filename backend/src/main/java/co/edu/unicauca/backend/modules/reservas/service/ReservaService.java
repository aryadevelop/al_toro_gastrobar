package co.edu.unicauca.backend.modules.reservas.service;

import co.edu.unicauca.backend.modules.mesas_comandas.entity.ComandaItem;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Zona;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ComandaItemRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ComandaRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ZonaRepository;
import co.edu.unicauca.backend.modules.pagos_caja.entity.Abono;
import co.edu.unicauca.backend.modules.pagos_caja.repository.AbonoRepository;
import co.edu.unicauca.backend.modules.reservas.dto.request.CrearReservaRequest;
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
    private static final String MSG_ANTICIPACION_MINIMA =
            "La reserva debe realizarse con al menos un día de anticipación.";
    private static final LocalTime HORA_APERTURA           = LocalTime.of(17, 0);
    private static final LocalTime HORA_CIERRE             = LocalTime.of(22, 0);
    private static final LocalTime HORA_LIMITE_MENU_ESPECIAL = LocalTime.of(23, 0);
    private static final LocalTime HORA_LIMITE_ESTANDAR      = LocalTime.of(13, 0);

    private final ReservaRepository reservaRepository;
    private final DecoracionRepository decoracionRepository;
    private final ZonaRepository zonaRepository;
    private final ClienteRepository clienteRepository;
    private final ComandaRepository comandaRepository;
    private final ComandaItemRepository comandaItemRepository;
    private final ReservaMapper reservaMapper;
    private final AbonoRepository abonoRepository;
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

        // Verificar existencia de la reserva
        Reserva reserva = reservaRepository.findById(reservaId)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva", reservaId));

        // Verificar ownership y estado activo
        reservaValidador.validarElegibilidadModificacion(reserva, emailCliente);

        // Verificar hora límite de modificación:
        // - Reserva actual o nueva es ESPECIAL: antes de las 11:00 pm del día anterior
        // - Básica sin ningún componente especial: antes de la 1:00 pm del mismo día
        boolean actualEsEspecial = reserva.getReservaTipo() == TipoReserva.ESPECIAL;

        // La nueva pre-orden incluye menú especial
        boolean nuevoTieneMenuEspecial = request.getPreOrden() != null &&
                request.getPreOrden().stream().anyMatch(i -> Boolean.TRUE.equals(i.getEsMenuEspecial()));

        // La nueva decoración tiene costo adicional > 0 (null se trata como sin costo)
        boolean nuevoDecoracionTieneCosto = reservaValidador.tieneDecoracionConCosto(
                request.getDecoracionId() == null ? null
                        : decoracionRepository.findById(request.getDecoracionId()).orElse(null));

        // Aplica límite de modificación si la reserva actual o la nueva configuración es ESPECIAL
        boolean aplicaLimiteEspecial = actualEsEspecial || nuevoTieneMenuEspecial || nuevoDecoracionTieneCosto;

        // Para evaluar la hora límite, se toma la fecha de llegada original, ya que el cliente podría estar modificando solo la hora o la decoración manteniendo la misma fecha, y el límite se calcula respecto a esa fecha original.
        LocalDateTime limiteModificacion = aplicaLimiteEspecial
                ? reserva.getReservaFechaHoraLlegada().toLocalDate().minusDays(1).atTime(HORA_LIMITE_MENU_ESPECIAL)
                : reserva.getReservaFechaHoraLlegada().toLocalDate().atTime(HORA_LIMITE_ESTANDAR);

        // Verificar que la modificación se realice antes del límite establecido
        if (!LocalDateTime.now().isBefore(limiteModificacion)) {
            throw new BusinessException(ErrorCode.INVALID_STATE, "No es posible modificar esta reserva. Solo cancelarla", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        // Validar que la nueva fecha, decoración y zona sean compatibles y tengan capacidad, excluyendo la reserva actual de las consultas de disponibilidad
        ParDecoracionZona dz = validarYCargarDecoracionZona(request.getFechaHoraLlegada(), request.getDecoracionId(), request.getZonaId());
        Decoracion nuevaDecoracion = dz.decoracion();
        Zona nuevaZona = dz.zona();

        // Validar que la nueva configuración tenga capacidad, excluyendo la reserva actual
        validarDisponibilidadYCapacidad(request.getFechaHoraLlegada(), nuevaDecoracion, nuevaZona, request.getNumeroPersonas(), reservaId);

        // Validar preorden
        if (request.getPreOrden() != null && !request.getPreOrden().isEmpty()) {
            preOrdenGestor.validarPreOrden(request.getPreOrden(), request.getNumeroPersonas());
        }

        // Determinar nuevo tipo de reserva para evaluar transiciones y estado resultante
        TipoReserva nuevoTipo = determinarTipoReserva(nuevaDecoracion, request.getPreOrden());
        boolean nuevoEsEspecial = (nuevoTipo == TipoReserva.ESPECIAL);
        boolean anteriorEraEspecial = reserva.getReservaTipo() == TipoReserva.ESPECIAL;
        boolean requiereWhatsApp = false;
        Reserva reservaResultado;

        // Capturar el numero antiguo de personas
        int oldNumeroPersonas = reserva.getReservaNumeroPersonas();

        // Para ESPECIAL→ESPECIAL: calcular valor anterior antes de eliminar la pre-orden
        BigDecimal valorAnterior = BigDecimal.ZERO;
        if (anteriorEraEspecial && nuevoEsEspecial) {
            if (reserva.getDecoracion() != null
                    && reserva.getDecoracion().getDecoracionCostoAdicional() != null) {
                valorAnterior = valorAnterior.add(reserva.getDecoracion().getDecoracionCostoAdicional());
            }
            for (ComandaItem item : obtenerItemsPreOrden(reservaId)) {
                if (Boolean.TRUE.equals(item.getProducto().getMenuEspecial())) {
                    valorAnterior = valorAnterior.add(
                            item.getComandaItemPrecio()
                                .multiply(BigDecimal.valueOf(oldNumeroPersonas)));
                }
            }
        }

        // Determinar nuevo estado según transición de tipo
        EstadoReserva nuevoEstado;
        if (!anteriorEraEspecial && nuevoEsEspecial) {
            nuevoEstado = EstadoReserva.PENDIENTE;
        } else if (anteriorEraEspecial && !nuevoEsEspecial) {
            nuevoEstado = EstadoReserva.CONFIRMADA;
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

        preOrdenGestor.eliminarPreOrdenExistente(reservaId);
        reservaResultado = reservaRepository.save(reserva);

        // Persistir nueva pre-orden si existe
        boolean tieneNuevaPreOrden = request.getPreOrden() != null && !request.getPreOrden().isEmpty();
        if (tieneNuevaPreOrden) {
            preOrdenGestor.persistirPreOrden(reservaResultado, request.getPreOrden());
        }

        // Determinar mensaje de WhatsApp según transición
        String mensajeWhatsApp = null;
        if (!anteriorEraEspecial && nuevoEsEspecial) {
            // BASICA → ESPECIAL: el cliente debe confirmar anticipo
            requiereWhatsApp = true;
            mensajeWhatsApp  = mensajeWhatsAppBuilder.construirMensaje(reservaResultado, MensajeWhatsAppBuilder.MSG_WA_ANTICIPO);

        } else if (anteriorEraEspecial && nuevoEsEspecial) {
            // ESPECIAL → ESPECIAL: notificar solo si el valor total (decoración + menú × personas) cambió
            BigDecimal valorNuevo = BigDecimal.ZERO;
            if (nuevaDecoracion != null && nuevaDecoracion.getDecoracionCostoAdicional() != null) {
                valorNuevo = valorNuevo.add(nuevaDecoracion.getDecoracionCostoAdicional());
            }
            for (ComandaItem item : obtenerItemsPreOrden(reservaResultado.getReservaId())) {
                if (Boolean.TRUE.equals(item.getProducto().getMenuEspecial())) {
                    valorNuevo = valorNuevo.add(
                            item.getComandaItemPrecio()
                                .multiply(BigDecimal.valueOf(request.getNumeroPersonas())));
                }
            }
            if (valorAnterior.compareTo(valorNuevo) != 0) {
                requiereWhatsApp = true;
                mensajeWhatsApp  = mensajeWhatsAppBuilder.construirMensaje(reservaResultado, MensajeWhatsAppBuilder.MSG_WA_CAMBIO_ESPECIAL);
            }

        } else if (anteriorEraEspecial) {
            // ESPECIAL → BASICA: notificar si el abono neto supera el valor de los platos
            BigDecimal totalAbonos = calcularAbonoNeto(reservaId);
            BigDecimal totalPlatos = calcularTotalPlatos(reservaResultado.getReservaId());
            if (totalAbonos.compareTo(BigDecimal.ZERO) > 0 && totalPlatos.compareTo(totalAbonos) < 0) {
                requiereWhatsApp = true;
                mensajeWhatsApp  = mensajeWhatsAppBuilder.construirMensaje(reservaResultado, MensajeWhatsAppBuilder.MSG_WA_CAMBIO_ESPECIAL);
            }

        } else {
            // BASICA → BASICA: notificar si el abono neto supera el valor de los platos
            BigDecimal totalAbonos = calcularAbonoNeto(reservaId);
            BigDecimal totalPlatos = calcularTotalPlatos(reservaResultado.getReservaId());
            if (totalAbonos.compareTo(BigDecimal.ZERO) > 0 && totalPlatos.compareTo(totalAbonos) < 0) {
                requiereWhatsApp = true;
                mensajeWhatsApp  = mensajeWhatsAppBuilder.construirMensaje(reservaResultado, MensajeWhatsAppBuilder.MSG_WA_ABONO_AJUSTE);
            }
        }

        return reservaMapper.toModificarResponse(reservaResultado, requiereWhatsApp, mensajeWhatsApp);
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

        //
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
