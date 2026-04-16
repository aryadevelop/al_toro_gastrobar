package co.edu.unicauca.backend.modules.reservas.service;

import co.edu.unicauca.backend.modules.inventario.entity.OpcionModificacion;
import co.edu.unicauca.backend.modules.inventario.repository.OpcionModificacionRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Zona;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ZonaRepository;
import co.edu.unicauca.backend.modules.produccion.entity.Producto;
import co.edu.unicauca.backend.modules.produccion.repository.ProductoRepository;
import co.edu.unicauca.backend.modules.reservas.dto.request.CrearReservaRequest;
import co.edu.unicauca.backend.modules.reservas.dto.request.PreOrdenItemRequest;
import co.edu.unicauca.backend.modules.reservas.dto.response.DecoracionDisponibleResponse;
import co.edu.unicauca.backend.modules.reservas.dto.response.DisponibilidadResponse;
import co.edu.unicauca.backend.modules.reservas.dto.response.PreOrdenDetalleResponse;
import co.edu.unicauca.backend.modules.reservas.dto.response.PreOrdenItemResumen;
import co.edu.unicauca.backend.modules.reservas.dto.response.ReservaResponse;
import co.edu.unicauca.backend.modules.reservas.dto.response.ZonaDisponibleResponse;
import co.edu.unicauca.backend.modules.reservas.entity.Decoracion;
import co.edu.unicauca.backend.modules.reservas.entity.DecoracionZona;
import co.edu.unicauca.backend.modules.reservas.entity.PreOrdenDetalle;
import co.edu.unicauca.backend.modules.reservas.entity.PreOrdenMenuModificacion;
import co.edu.unicauca.backend.modules.reservas.entity.Reserva;
import co.edu.unicauca.backend.modules.reservas.repository.BloqueDisponibilidadRepository;
import co.edu.unicauca.backend.modules.reservas.repository.DecoracionRepository;
import co.edu.unicauca.backend.modules.reservas.repository.DecoracionZonaRepository;
import co.edu.unicauca.backend.modules.reservas.repository.PreOrdenDetalleRepository;
import co.edu.unicauca.backend.modules.reservas.repository.PreOrdenMenuModificacionRepository;
import co.edu.unicauca.backend.modules.inventario.repository.ProductoOpcionModificacionRepository;
import co.edu.unicauca.backend.modules.reservas.repository.ReservaRepository;
import co.edu.unicauca.backend.modules.usuarios.entity.Cliente;
import co.edu.unicauca.backend.modules.usuarios.repository.ClienteRepository;
import co.edu.unicauca.backend.shared.enums.EstadoGenerico;
import co.edu.unicauca.backend.shared.enums.EstadoReserva;
import co.edu.unicauca.backend.shared.enums.TipoReserva;
import co.edu.unicauca.backend.shared.exception.BusinessException;
import co.edu.unicauca.backend.shared.exception.ErrorCode;
import co.edu.unicauca.backend.shared.exception.ResourceNotFoundException;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReservaService {

    private static final List<EstadoReserva> ESTADOS_ACTIVOS = List.of(EstadoReserva.PENDIENTE, EstadoReserva.CONFIRMADA);

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private static final String MSG_DISPONIBILIDAD_CAMBIO = "Lo sentimos, la disponibilidad cambió. Por favor revise nuevamente.";

    /** Hora de apertura del restaurante (5:00 PM, inclusive). */
    private static final int HORA_APERTURA = 17;
    /** Hora de cierre del restaurante (10:00 PM, exclusiva). */
    private static final int HORA_CIERRE = 22;

    private static final String MSG_FUERA_HORARIO =
            "Lo sentimos, no hay disponibilidad para la fecha y hora seleccionada. " +
            "El horario de reservas es de lunes a domingo de 5:00 p.m. a 10:00 p.m. " +
            "Por favor elija otra fecha u hora.";

    private final ReservaRepository reservaRepository;
    private final DecoracionRepository decoracionRepository;
    private final DecoracionZonaRepository decoracionZonaRepository;
    private final ZonaRepository zonaRepository;
    private final ClienteRepository clienteRepository;
    private final BloqueDisponibilidadRepository bloqueRepository;
    private final ProductoRepository productoRepository;
    private final OpcionModificacionRepository opcionModificacionRepository;
    private final PreOrdenDetalleRepository preOrdenDetalleRepository;
    private final PreOrdenMenuModificacionRepository preOrdenMenuModificacionRepository;
    private final ProductoOpcionModificacionRepository productoOpcionModificacionRepository;

    // -----------------------------------------------------------------------
    // Disponibilidad
    // -----------------------------------------------------------------------

    /**
     * Retorna la disponibilidad para el día de la fecha dada.
     * Devuelve {@code disponible=false} si:
     *  - La hora está fuera del horario de atención (5 PM – 10 PM), o
     *  - Existe un bloqueo activo creado por el administrador para esa fecha/hora, o
     *  - Ninguna zona tiene capacidad restante.
     */
    @Transactional(readOnly = true)
    public DisponibilidadResponse consultarDisponibilidad(LocalDateTime fechaHora) {

        // Regla 1: verificar horario de atención
        if (!esHorarioValido(fechaHora)) {
            return sinDisponibilidad();
        }

        // Regla 2: verificar bloqueos de administrador
        if (estaBloqueda(fechaHora)) {
            return sinDisponibilidad();
        }

        List<Zona> todasLasZonas = zonaRepository.findAll();

        if (todasLasZonas.isEmpty()) {
            return sinDisponibilidad();
        }

        LocalDateTime inicio = fechaHora.toLocalDate().atStartOfDay();
        LocalDateTime fin = fechaHora.toLocalDate().atTime(23, 59, 59);

        // Personas ya reservadas por zona ese día: {zonaId -> sumaPersonas}
        Map<Long, Integer> personasPorZona = reservaRepository
                .findPersonasPorZonaEnDia(inicio, fin, ESTADOS_ACTIVOS)
                .stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> ((Number) row[1]).intValue()
                ));

        Set<Long> decoracionesOcupadas = Set.copyOf(
                reservaRepository.findDecoracionesOcupadasEnDia(inicio, fin, ESTADOS_ACTIVOS));

        // Zona libre = tiene al menos 1 persona de capacidad restante
        List<Zona> zonasLibres = todasLasZonas.stream()
                .filter(z -> personasPorZona.getOrDefault(z.getZonaId(), 0)
                             < z.getZonaCapacidadPersonas())
                .collect(Collectors.toList());

        if (zonasLibres.isEmpty()) {
            return sinDisponibilidad();
        }

        List<Decoracion> decoracionesActivas = decoracionRepository
                .findByDecoracionEstado(EstadoGenerico.ACTIVO)
                .stream()
                .filter(d -> !decoracionesOcupadas.contains(d.getDecoracionId()))
                .collect(Collectors.toList());

        Set<Long> idsZonasLibres = zonasLibres.stream()
                .map(Zona::getZonaId)
                .collect(Collectors.toSet());

        List<DecoracionDisponibleResponse> decoracionesDto = decoracionesActivas.stream()
                .map(d -> buildDecoracionDto(d, idsZonasLibres))
                .collect(Collectors.toList());

        List<ZonaDisponibleResponse> zonasDto = zonasLibres.stream()
                .map(this::buildZonaDto)
                .collect(Collectors.toList());

        return DisponibilidadResponse.builder()
                .disponible(true)
                .decoraciones(decoracionesDto)
                .zonas(zonasDto)
                .build();
    }

    // -----------------------------------------------------------------------
    // Crear reserva
    // -----------------------------------------------------------------------

    /**
     * Crea una nueva reserva para el cliente identificado por su email.
     * Verifica horario de atención, bloqueos de administrador y disponibilidad de zona.
     */
    @Transactional
    public ReservaResponse crearReserva(String emailCliente, CrearReservaRequest request) {
        Cliente cliente = clienteRepository.findByUsuario_UsuarioEmail(emailCliente)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cliente", "email", emailCliente));

        // Validar horario de atención antes de cualquier otra verificación
        if (!esHorarioValido(request.getFechaHoraLlegada())) {
            throw new BusinessException(ErrorCode.INVALID_STATE, MSG_FUERA_HORARIO,
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }

        // Validar bloqueos de administrador
        if (estaBloqueda(request.getFechaHoraLlegada())) {
            throw new BusinessException(ErrorCode.INVALID_STATE,
                    "Lo sentimos, no hay disponibilidad para la fecha y hora seleccionada. Por favor elija otra fecha u hora.",
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }

        // Verificar existencia de decoración y zona en BD (→ 404 si no existe)
        Decoracion decoracion = null;
        if (request.getDecoracionId() != null) {
            final Long decId = request.getDecoracionId();
            decoracion = decoracionRepository.findById(decId)
                    .orElseThrow(() -> new ResourceNotFoundException("Decoracion", decId));
        }

        Zona zona = null;
        if (request.getZonaId() != null) {
            final Long zonaId = request.getZonaId();
            zona = zonaRepository.findById(zonaId)
                    .orElseThrow(() -> new ResourceNotFoundException("Zona", zonaId));
        }

        // Validar compatibilidad decoración ↔ zona ANTES de verificar disponibilidad:
        if (decoracion != null && zona != null) {
            validarCompatibilidadDecoracionZona(decoracion, zona);
        }

        // Verificar disponibilidad
        DisponibilidadResponse disponibilidad =
                consultarDisponibilidad(request.getFechaHoraLlegada());

        if (!disponibilidad.getDisponible()) {
            throw new BusinessException(ErrorCode.INVALID_STATE, MSG_DISPONIBILIDAD_CAMBIO,
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }

        // Verificar disponibilidad de la decoración en el momento actual (→ 422 si ya está ocupada)
        if (decoracion != null) {
            final Long decId = decoracion.getDecoracionId();
            boolean decoracionLibre = disponibilidad.getDecoraciones().stream()
                    .anyMatch(d -> d.getDecoracionId().equals(decId));
            if (!decoracionLibre) {
                throw new BusinessException(ErrorCode.INVALID_STATE, MSG_DISPONIBILIDAD_CAMBIO,
                        HttpStatus.UNPROCESSABLE_ENTITY);
            }
        }

        // Validar que la zona pedida sigue con capacidad
        if (zona != null) {
            final Long zonaId = zona.getZonaId();
            boolean zonaLibre = disponibilidad.getZonas().stream()
                    .anyMatch(z -> z.getZonaId().equals(zonaId));
            if (!zonaLibre) {
                throw new BusinessException(ErrorCode.INVALID_STATE, MSG_DISPONIBILIDAD_CAMBIO,
                        HttpStatus.UNPROCESSABLE_ENTITY);
            }

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

        // Una reserva es especial si la decoración tiene costo adicional > 0 o si incluye un menú especial
        boolean tieneDecoracionConCosto = decoracion != null &&
                decoracion.getDecoracionCostoAdicional() != null &&
                decoracion.getDecoracionCostoAdicional().compareTo(java.math.BigDecimal.ZERO) > 0;

        boolean tieneMenuEspecial = request.getPreOrden() != null &&
                request.getPreOrden().stream()
                        .anyMatch(item -> Boolean.TRUE.equals(item.getEsMenuEspecial()));

        boolean esEspecial = tieneDecoracionConCosto || tieneMenuEspecial;

        TipoReserva tipo = esEspecial ? TipoReserva.ESPECIAL : TipoReserva.BASICA;
        EstadoReserva estado = esEspecial ? EstadoReserva.PENDIENTE : EstadoReserva.CONFIRMADA;

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

        // Validar reglas de negocio de pre-orden antes de persistir
        if (request.getPreOrden() != null && !request.getPreOrden().isEmpty()) {
            validarPreOrden(request.getPreOrden(), request.getNumeroPersonas());
            persistirPreOrden(guardada, request.getPreOrden());
        }

        return toResponse(guardada);
    }

    // -----------------------------------------------------------------------
    // Historial de reservas
    // -----------------------------------------------------------------------

    /** Retorna el historial del cliente autenticado. */
    @Transactional(readOnly = true)
    public List<ReservaResponse> obtenerReservasCliente(String emailCliente) {
        Cliente cliente = clienteRepository.findByUsuario_UsuarioEmail(emailCliente)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cliente", "email", emailCliente));

        return reservaRepository
                .findByCliente_UsuarioIdOrderByReservaFechaHoraLlegadaDesc(cliente.getUsuarioId())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /** Retorna el historial completo de todas las reservas del sistema (acceso de personal). */
    @Transactional(readOnly = true)
    public List<ReservaResponse> obtenerTodasLasReservas() {
        return reservaRepository
                .findAll(Sort.by(Sort.Direction.DESC, "reservaFechaHoraLlegada"))
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // -----------------------------------------------------------------------
    // Pre-orden
    // -----------------------------------------------------------------------

    /**
     * Valida las reglas de negocio de la pre-orden antes de persistirla:
     *  - CA-01/CA-05: si hay algún ítem de menú especial, el número de personas debe ser > 10.
     *  - CA-05: solo se permite un ítem de menú especial por reserva.
     */
    private void validarPreOrden(List<PreOrdenItemRequest> items, int numeroPersonas) {
        long menuEspecialCount = items.stream()
                .filter(i -> Boolean.TRUE.equals(i.getEsMenuEspecial()))
                .count();

        if (menuEspecialCount > 0 && numeroPersonas <= 10) {
            throw new BusinessException(ErrorCode.INVALID_STATE,
                    "El menú especial solo está disponible para reservas de más de 10 personas.",
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }

        if (menuEspecialCount > 1) {
            throw new BusinessException(ErrorCode.INVALID_STATE,
                    "Solo puede seleccionar un menú especial por reserva.",
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    /**
     * Persiste los ítems de pre-orden asociados a una reserva.
     * Por cada ítem:
     *  - Valida que el producto exista y esté activo.
     *  - Crea PreOrdenDetalle con la descripción (null = sin modificación libre).
     *  - Si esMenuEspecial=true, persiste las selecciones de checkbox en PreOrdenMenuModificacion.
     */
    private void persistirPreOrden(Reserva reserva, List<PreOrdenItemRequest> items) {
        for (PreOrdenItemRequest item : items) {
            Producto producto = productoRepository.findById(item.getProductoId())
                    .orElseThrow(() -> new ResourceNotFoundException("Producto", item.getProductoId()));

            if (producto.getProductoEstado() != EstadoGenerico.ACTIVO) {
                throw new BusinessException(ErrorCode.INVALID_STATE,
                        "El producto '" + producto.getProductoNombre() + "' no está disponible.",
                        HttpStatus.UNPROCESSABLE_ENTITY);
            }

            PreOrdenDetalle detalle = PreOrdenDetalle.builder()
                    .reserva(reserva)
                    .producto(producto)
                    .preordenDetalleCantidad(item.getCantidad())
                    .preordenDetalleDescripcion(item.getDescripcion())
                    .build();

            PreOrdenDetalle detalleGuardado = preOrdenDetalleRepository.save(detalle);

            // Persistir modificaciones de menú especial (checkboxes CA-07)
            if (Boolean.TRUE.equals(item.getEsMenuEspecial())
                    && item.getOpcionesModificacion() != null
                    && !item.getOpcionesModificacion().isEmpty()) {

                for (Long opcionId : item.getOpcionesModificacion()) {
                    OpcionModificacion opcion = opcionModificacionRepository.findById(opcionId)
                            .orElseThrow(() -> new ResourceNotFoundException("OpcionModificacion", opcionId));

                    // CA-07: verificar que la opción pertenece al menú seleccionado
                    if (!productoOpcionModificacionRepository.existsByProductoIdAndOpcionId(
                            producto.getProductoId(), opcionId)) {
                        throw new BusinessException(ErrorCode.INVALID_STATE,
                                "La opción de modificación '" + opcion.getOpcionNombre() +
                                "' no pertenece al menú seleccionado.",
                                HttpStatus.UNPROCESSABLE_ENTITY);
                    }

                    PreOrdenMenuModificacion mod = PreOrdenMenuModificacion.builder()
                            .preordenDetalle(detalleGuardado)
                            .opcion(opcion)
                            .build();

                    preOrdenMenuModificacionRepository.save(mod);
                }
            }
        }
    }

    // -----------------------------------------------------------------------
    // Pre-orden por reserva (endpoint para módulo de comandas)
    // -----------------------------------------------------------------------

    /**
     * Retorna el detalle completo de la pre-orden de una reserva.
     * Accesible por CAJERO y MESERO para pre-cargar la comanda.
     */
    @Transactional(readOnly = true)
    public List<PreOrdenDetalleResponse> obtenerPreOrden(Long reservaId) {
        if (!reservaRepository.existsById(reservaId)) {
            throw new ResourceNotFoundException("Reserva", reservaId);
        }

        List<PreOrdenDetalle> detalles =
                preOrdenDetalleRepository.findByReserva_ReservaIdOrderByCreatedAtAsc(reservaId);

        return detalles.stream()
                .map(d -> {
                    List<PreOrdenMenuModificacion> mods =
                            preOrdenMenuModificacionRepository
                                    .findByPreordenDetalle_PreordenDetalleId(d.getPreordenDetalleId());

                    List<PreOrdenDetalleResponse.OpcionModificacionSeleccionada> modificaciones =
                            mods.stream()
                                    .map(m -> PreOrdenDetalleResponse.OpcionModificacionSeleccionada.builder()
                                            .opcionId(m.getOpcion().getOpcionId())
                                            .opcionNombre(m.getOpcion().getOpcionNombre())
                                            .tipoComponente(m.getOpcion().getTipoComponente().name())
                                            .build())
                                    .collect(Collectors.toList());

                    return PreOrdenDetalleResponse.builder()
                            .preordenDetalleId(d.getPreordenDetalleId())
                            .productoId(d.getProducto().getProductoId())
                            .productoNombre(d.getProducto().getProductoNombre())
                            .cantidad(d.getPreordenDetalleCantidad())
                            .precioUnitario(d.getProducto().getProductoPrecio())
                            .descripcion(d.getPreordenDetalleDescripcion())
                            .modificaciones(modificaciones.isEmpty() ? null : modificaciones)
                            .build();
                })
                .collect(Collectors.toList());
    }

    // -----------------------------------------------------------------------
    // Validaciones de negocio
    // -----------------------------------------------------------------------

    /**
     * Verifica que la fecha y hora estén dentro del horario de atención del restaurante.
     * Horario: lunes a domingo, 5:00 PM (17:00) a 10:00 PM (22:00, exclusivo).
     */
    private boolean esHorarioValido(LocalDateTime fechaHora) {
        int hora = fechaHora.getHour();
        return hora >= HORA_APERTURA && hora < HORA_CIERRE;
    }

    /**
     * Verifica si existe un bloqueo de administrador activo para la fecha y hora dada.
     */
    private boolean estaBloqueda(LocalDateTime fechaHora) {
        LocalDate fecha = fechaHora.toLocalDate();
        LocalTime hora  = fechaHora.toLocalTime();
        return bloqueRepository.countBloquesParaFechaHora(fecha, hora) > 0;
    }

    /**
     * Verifica que la decoración sea compatible con la zona elegida.
     */
    private void validarCompatibilidadDecoracionZona(Decoracion decoracion, Zona zona) {
        List<DecoracionZona> links = decoracionZonaRepository
                .findByDecoracionId(decoracion.getDecoracionId());

        if (links.size() == 1) {
            Long zonaPermitida = links.get(0).getZonaId();
            if (!zonaPermitida.equals(zona.getZonaId())) {
                throw new BusinessException(ErrorCode.INVALID_STATE,
                        "La decoración seleccionada solo puede usarse en su zona asignada.",
                        HttpStatus.UNPROCESSABLE_ENTITY);
            }
        } else if (links.size() > 1) {
            boolean esCompatible = links.stream()
                    .anyMatch(l -> l.getZonaId().equals(zona.getZonaId()));
            if (!esCompatible) {
                throw new BusinessException(ErrorCode.INVALID_STATE,
                        "La decoración seleccionada no es compatible con la zona elegida.",
                        HttpStatus.UNPROCESSABLE_ENTITY);
            }
        }
    }

    // -----------------------------------------------------------------------
    // Helpers de mapeo
    // -----------------------------------------------------------------------

    private DisponibilidadResponse sinDisponibilidad() {
        return DisponibilidadResponse.builder()
                .disponible(false)
                .decoraciones(List.of())
                .zonas(List.of())
                .build();
    }

    private DecoracionDisponibleResponse buildDecoracionDto(Decoracion d, Set<Long> idsZonasLibres) {
        List<DecoracionZona> links = decoracionZonaRepository.findByDecoracionId(d.getDecoracionId());

        boolean puedeSeleccionar = links.size() != 1;

        List<Long> zonaIdsCompatibles = links.stream()
                .map(DecoracionZona::getZonaId)
                .filter(idsZonasLibres::contains)
                .collect(Collectors.toList());

        return DecoracionDisponibleResponse.builder()
                .decoracionId(d.getDecoracionId())
                .nombre(d.getDecoracionNombre())
                .imagenUrl(d.getDecoracionImagenUrl())
                .puedeSeleccionarZona(puedeSeleccionar)
                .zonaIdsCompatibles(zonaIdsCompatibles)
                .build();
    }

    private ZonaDisponibleResponse buildZonaDto(Zona z) {
        return ZonaDisponibleResponse.builder()
                .zonaId(z.getZonaId())
                .nombre(z.getZonaNombre())
                .imagenUrl(z.getZonaImagenUrl())
                .capacidad(z.getZonaCapacidadPersonas())
                .build();
    }

    private ReservaResponse toResponse(Reserva r) {
        List<PreOrdenDetalle> detalles =
                preOrdenDetalleRepository.findByReserva_ReservaIdOrderByCreatedAtAsc(r.getReservaId());

        List<PreOrdenItemResumen> preOrdenItems = detalles.isEmpty() ? null :
                detalles.stream()
                        .map(d -> PreOrdenItemResumen.builder()
                                .productoId(d.getProducto().getProductoId())
                                .productoNombre(d.getProducto().getProductoNombre())
                                .cantidad(d.getPreordenDetalleCantidad())
                                .precioUnitario(d.getProducto().getProductoPrecio())
                                .descripcion(d.getPreordenDetalleDescripcion())
                                .build())
                        .collect(Collectors.toList());

        // Total aproximado: solo ítems normales (descripcion == null); modificaciones libres son TBD
        BigDecimal preOrdenTotal = detalles.isEmpty() ? null :
                detalles.stream()
                        .filter(d -> d.getPreordenDetalleDescripcion() == null)
                        .map(d -> d.getProducto().getProductoPrecio()
                                .multiply(BigDecimal.valueOf(d.getPreordenDetalleCantidad())))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        return ReservaResponse.builder()
                .reservaId(r.getReservaId())
                .fechaHoraLlegada(r.getReservaFechaHoraLlegada().format(FORMATTER))
                .numeroPersonas(r.getReservaNumeroPersonas())
                .estado(r.getReservaEstado().name())
                .tipo(r.getReservaTipo().name())
                .decoracionId(r.getDecoracion() != null ? r.getDecoracion().getDecoracionId() : null)
                .decoracionNombre(r.getDecoracion() != null ? r.getDecoracion().getDecoracionNombre() : null)
                .zonaId(r.getZona() != null ? r.getZona().getZonaId() : null)
                .zonaNombre(r.getZona() != null ? r.getZona().getZonaNombre() : null)
                .notas(r.getReservaNotas())
                .fechaCreacion(r.getReservaFechaCreacion().format(FORMATTER))
                .clienteId(r.getCliente().getUsuarioId())
                .clienteNombre(r.getCliente().getClienteNombre())
                .preOrdenItems(preOrdenItems)
                .preOrdenTotal(preOrdenTotal)
                .build();
    }
}
