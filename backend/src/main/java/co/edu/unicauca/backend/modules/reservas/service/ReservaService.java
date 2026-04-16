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

/**
 * Servicio de negocio para la gestión de reservas del restaurante.
 *
 * <p>Centraliza toda la lógica de creación, consulta y validación de reservas,
 * incluyendo el cálculo de disponibilidad de zonas y decoraciones, la gestión
 * de bloqueos administrativos y la persistencia de pre-órdenes.
 *
 * <p>Responsabilidades principales:
 * <ul>
 *   <li>Consultar disponibilidad para una fecha y hora dada.</li>
 *   <li>Crear una reserva validando horario, bloqueos, capacidad y compatibilidad
 *       decoración-zona.</li>
 *   <li>Persistir la pre-orden asociada, incluyendo modificaciones de menú especial.</li>
 *   <li>Exponer el historial de reservas.</li>
 *   <li>Proveer el detalle de pre-orden.</li>
 * </ul>
 *
 * @see co.edu.unicauca.backend.modules.reservas.entity.Reserva
 * @see co.edu.unicauca.backend.modules.reservas.dto.request.CrearReservaRequest
 * @see co.edu.unicauca.backend.modules.reservas.dto.response.DisponibilidadResponse
 */
@Service
@RequiredArgsConstructor
public class ReservaService {

    /** Estados de reserva considerados como ocupación activa para el cálculo de disponibilidad. */
    private static final List<EstadoReserva> ESTADOS_ACTIVOS = List.of(EstadoReserva.PENDIENTE, EstadoReserva.CONFIRMADA);

    /** Formateador de fechas/horas para las respuestas JSON ({@code ISO-8601} sin zona horaria). */
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

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
    private final ProductoRepository productoRepository;
    private final OpcionModificacionRepository opcionModificacionRepository;
    private final PreOrdenDetalleRepository preOrdenDetalleRepository;
    private final PreOrdenMenuModificacionRepository preOrdenMenuModificacionRepository;
    private final ProductoOpcionModificacionRepository productoOpcionModificacionRepository;
    
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

        // Rechazar si la hora está fuera del horario de atención del restaurante.
        if (!esHorarioValido(fechaHora)) {
            return sinDisponibilidad();
        }

        // Rechazar si existe un bloqueo administrativo para esa fecha/hora.
        if (estaBloqueda(fechaHora)) {
            return sinDisponibilidad();
        }

        // Si no hay zonas configuradas, no puede haber disponibilidad.
        List<Zona> todasLasZonas = zonaRepository.findAll();
        if (todasLasZonas.isEmpty()) {
            return sinDisponibilidad();
        }

        // Rango del día completo para agregar las reservas existentes.
        LocalDateTime inicio = fechaHora.toLocalDate().atStartOfDay();
        LocalDateTime fin = fechaHora.toLocalDate().atTime(23, 59, 59);

        // Calcular personas ya reservadas por zona ese día → {zonaId: sumaPersonas}.
        Map<Long, Integer> personasPorZona = reservaRepository
                .findPersonasPorZonaEnDia(inicio, fin, ESTADOS_ACTIVOS)
                .stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> ((Number) row[1]).intValue()
                ));

        // Obtener IDs de decoraciones ya asignadas a otra reserva activa ese día.
        Set<Long> decoracionesOcupadas = Set.copyOf(
                reservaRepository.findDecoracionesOcupadasEnDia(inicio, fin, ESTADOS_ACTIVOS));

        // Filtrar zonas con al menos 1 persona de capacidad disponible.
        List<Zona> zonasLibres = todasLasZonas.stream()
                .filter(z -> personasPorZona.getOrDefault(z.getZonaId(), 0)
                             < z.getZonaCapacidadPersonas())
                .collect(Collectors.toList());

        if (zonasLibres.isEmpty()) {
            return sinDisponibilidad();
        }

        // Filtrar decoraciones activas que no estén ocupadas ese día.
        List<Decoracion> decoracionesActivas = decoracionRepository
                .findByDecoracionEstado(EstadoGenerico.ACTIVO)
                .stream()
                .filter(d -> !decoracionesOcupadas.contains(d.getDecoracionId()))
                .collect(Collectors.toList());

        // Construir los DTOs de respuesta con zonas y decoraciones libres.
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
     * <p>El {@code TipoReserva} y el {@code EstadoReserva} iniciales se determinan
     * automáticamente: una reserva con decoración de costo o menú especial es
     * {@code ESPECIAL/PENDIENTE}; en caso contrario es {@code BASICA/CONFIRMADA}.
     *
     * @param emailCliente correo del cliente autenticado
     * @param request      datos de la reserva a crear
     * @return {@link ReservaResponse} con los datos completos de la reserva creada
     * @throws co.edu.unicauca.backend.shared.exception.ResourceNotFoundException si el cliente,
     *         la decoración o la zona no existen
     * @throws co.edu.unicauca.backend.shared.exception.BusinessException si se incumple
     *         cualquier regla de negocio (horario, bloqueo, capacidad, compatibilidad)
     */
    @Transactional
    public ReservaResponse crearReserva(String emailCliente, CrearReservaRequest request) {

        // Obtener el cliente autenticado; lanza 404 si no existe.
        Cliente cliente = clienteRepository.findByUsuario_UsuarioEmail(emailCliente)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente", "email", emailCliente));

        // Validar horario de atención antes de cualquier otra verificación.
        if (!esHorarioValido(request.getFechaHoraLlegada())) {
            throw new BusinessException(ErrorCode.INVALID_STATE, MSG_FUERA_HORARIO,
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }

        // Verificar que no exista un bloqueo administrativo para la fecha/hora solicitada.
        if (estaBloqueda(request.getFechaHoraLlegada())) {
            throw new BusinessException(ErrorCode.INVALID_STATE, MSG_FUERA_HORARIO, HttpStatus.UNPROCESSABLE_ENTITY);
        }

        // Resolver la decoración solicitada; null si el cliente no eligió ninguna.
        Decoracion decoracion = null;
        if (request.getDecoracionId() != null) {
            final Long decId = request.getDecoracionId();
            decoracion = decoracionRepository.findById(decId)
                    .orElseThrow(() -> new ResourceNotFoundException("Decoracion", decId));
        }

        // Resolver la zona solicitada; null si el cliente no eligió ninguna.
        Zona zona = null;
        if (request.getZonaId() != null) {
            final Long zonaId = request.getZonaId();
            zona = zonaRepository.findById(zonaId)
                    .orElseThrow(() -> new ResourceNotFoundException("Zona", zonaId));
        }

        // Validar que la decoración sea compatible con la zona antes de consultar disponibilidad.
        if (decoracion != null && zona != null) {
            validarCompatibilidadDecoracionZona(decoracion, zona);
        }

        // Confirmar disponibilidad en tiempo real.
        DisponibilidadResponse disponibilidad =
                consultarDisponibilidad(request.getFechaHoraLlegada());

        if (!disponibilidad.getDisponible()) {
            throw new BusinessException(ErrorCode.INVALID_STATE, MSG_DISPONIBILIDAD_CAMBIO,
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }

        // Verificar que la decoración elegida siga disponible en este momento exacto.
        if (decoracion != null) {
            final Long decId = decoracion.getDecoracionId();
            boolean decoracionLibre = disponibilidad.getDecoraciones().stream()
                    .anyMatch(d -> d.getDecoracionId().equals(decId));
            if (!decoracionLibre) {
                throw new BusinessException(ErrorCode.INVALID_STATE, MSG_DISPONIBILIDAD_CAMBIO,
                        HttpStatus.UNPROCESSABLE_ENTITY);
            }
        }

        // Verificar que la zona siga con cupo y que no se supere su capacidad total en el día.
        if (zona != null) {
            final Long zonaId = zona.getZonaId();
            boolean zonaLibre = disponibilidad.getZonas().stream()
                    .anyMatch(z -> z.getZonaId().equals(zonaId));
            if (!zonaLibre) {
                throw new BusinessException(ErrorCode.INVALID_STATE, MSG_DISPONIBILIDAD_CAMBIO,
                        HttpStatus.UNPROCESSABLE_ENTITY);
            }

            // Sumar personas ya reservadas en esa zona ese día para validar el cupo exacto.
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

        // Determinar tipo y estado iniciales:
        // ESPECIAL/PENDIENTE si hay decoración con costo o menú especial
        // BASICA/CONFIRMADA en caso contrario.
        boolean tieneDecoracionConCosto = decoracion != null &&
                decoracion.getDecoracionCostoAdicional() != null &&
                decoracion.getDecoracionCostoAdicional().compareTo(java.math.BigDecimal.ZERO) > 0;

        boolean tieneMenuEspecial = request.getPreOrden() != null &&
                request.getPreOrden().stream()
                        .anyMatch(item -> Boolean.TRUE.equals(item.getEsMenuEspecial()));

        boolean esEspecial = tieneDecoracionConCosto || tieneMenuEspecial;

        TipoReserva tipo = esEspecial ? TipoReserva.ESPECIAL : TipoReserva.BASICA;
        EstadoReserva estado = esEspecial ? EstadoReserva.PENDIENTE : EstadoReserva.CONFIRMADA;

        // Validar reglas de la pre-orden
        if (request.getPreOrden() != null && !request.getPreOrden().isEmpty()) {
            validarPreOrden(request.getPreOrden(), request.getNumeroPersonas());
        }

        // Construir y persistir la reserva solo cuando todas las validaciones han pasado.
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

        // Persistir la pre-orden cuando la reserva ya tiene ID asignado.
        if (request.getPreOrden() != null && !request.getPreOrden().isEmpty()) {
            persistirPreOrden(guardada, request.getPreOrden());
        }

        return toResponse(guardada);
    }


    /**
     * Devuelve el historial de reservas del cliente autenticado, ordenadas de más reciente a más antigua.
     *
     * @param emailCliente correo del cliente autenticado
     * @return lista de {@link ReservaResponse} con las reservas del cliente; vacía si no tiene ninguna
     * @throws co.edu.unicauca.backend.shared.exception.ResourceNotFoundException si el cliente no existe
     */
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

    /**
     * Devuelve el historial completo de todas las reservas del sistema, ordenadas de más reciente a más antigua.
     *
     * <p>Solo accesible por personal del restaurante (cajero, mesero, administrador).
     *
     * @return lista de {@link ReservaResponse} con todas las reservas registradas; vacía si no hay ninguna
     */
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
     * Valida las reglas de negocio de la pre-orden antes de persistirla.
     *
     * <p>Reglas aplicadas:
     * <ul>
     *   <li><b>CA-01/CA-05:</b> si hay algún ítem de menú especial, la reserva debe ser
     *       para más de {@code 10} personas.</li>
     *   <li><b>CA-05:</b> solo se permite un ítem de menú especial por reserva.</li>
     * </ul>
     *
     * @param items          lista de ítems de la pre-orden a validar
     * @param numeroPersonas número de comensales de la reserva
     * @throws co.edu.unicauca.backend.shared.exception.BusinessException si se incumple
     *         alguna de las reglas anteriores
     */
    private void validarPreOrden(List<PreOrdenItemRequest> items, int numeroPersonas) {

        // Contar cuántos ítems corresponden a un menú especial.
        long menuEspecialCount = items.stream()
                .filter(i -> Boolean.TRUE.equals(i.getEsMenuEspecial()))
                .count();

        // CA-01/CA-05: el menú especial requiere más de 10 comensales.
        if (menuEspecialCount > 0 && numeroPersonas <= 10) {
            throw new BusinessException(ErrorCode.INVALID_STATE,
                    "El menú especial solo está disponible para reservas de más de 10 personas.",
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }

        // CA-05: no se permite más de un menú especial por reserva.
        if (menuEspecialCount > 1) {
            throw new BusinessException(ErrorCode.INVALID_STATE,
                    "Solo puede seleccionar un menú especial por reserva.",
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    /**
     * Persiste los ítems de la pre-orden asociados a una reserva ya guardada.
     *
     * <p>Por cada ítem de la lista:
     * <ul>
     *   <li>Valida que el producto exista y esté en estado {@code ACTIVO}.</li>
     *   <li>Crea y guarda un {@link PreOrdenDetalle}; {@code descripcion null} indica sin nota libre.</li>
     *   <li>Si {@code esMenuEspecial = true}, persiste cada opción de modificación seleccionada
     *       en {@link PreOrdenMenuModificacion}, verificando que la opción pertenezca al menú
     *       indicado (regla <b>CA-07</b>).</li>
     * </ul>
     *
     * @param reserva reserva a la que se asocia la pre-orden (ya persistida)
     * @param items   lista de ítems a guardar
     * @throws co.edu.unicauca.backend.shared.exception.ResourceNotFoundException si el producto
     *         o la opción de modificación no existen
     * @throws co.edu.unicauca.backend.shared.exception.BusinessException si el producto no está
     *         activo o si una opción no pertenece al menú seleccionado
     */
    private void persistirPreOrden(Reserva reserva, List<PreOrdenItemRequest> items) {
        for (PreOrdenItemRequest item : items) {

            // Verificar que el producto exista y esté activo para incluirlo en la pre-orden.
            Producto producto = productoRepository.findById(item.getProductoId())
                    .orElseThrow(() -> new ResourceNotFoundException("Producto", item.getProductoId()));

            if (producto.getProductoEstado() != EstadoGenerico.ACTIVO) {
                throw new BusinessException(ErrorCode.INVALID_STATE,
                        "El producto '" + producto.getProductoNombre() + "' no está disponible.",
                        HttpStatus.UNPROCESSABLE_ENTITY);
            }

            // Crear y guardar el detalle de pre-orden para este ítem.
            PreOrdenDetalle detalle = PreOrdenDetalle.builder()
                    .reserva(reserva)
                    .producto(producto)
                    .preordenDetalleCantidad(item.getCantidad())
                    .preordenDetalleDescripcion(item.getDescripcion())
                    .build();

            PreOrdenDetalle detalleGuardado = preOrdenDetalleRepository.save(detalle);

            // Si el ítem es un menú especial, persistir las opciones de modificación seleccionadas.
            if (Boolean.TRUE.equals(item.getEsMenuEspecial())
                    && item.getOpcionesModificacion() != null
                    && !item.getOpcionesModificacion().isEmpty()) {

                for (Long opcionId : item.getOpcionesModificacion()) {

                    // Verificar que la opción de modificación exista en el sistema.
                    OpcionModificacion opcion = opcionModificacionRepository.findById(opcionId)
                            .orElseThrow(() -> new ResourceNotFoundException("OpcionModificacion", opcionId));

                    // CA-07: la opción debe pertenecer al menú del producto seleccionado.
                    if (!productoOpcionModificacionRepository.existsByProductoIdAndOpcionId(
                            producto.getProductoId(), opcionId)) {
                        throw new BusinessException(ErrorCode.INVALID_STATE,
                                "La opción de modificación '" + opcion.getOpcionNombre() +
                                "' no pertenece al menú seleccionado.",
                                HttpStatus.UNPROCESSABLE_ENTITY);
                    }

                    // Registrar la opción seleccionada vinculada al detalle de pre-orden.
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
     * Devuelve el detalle completo de la pre-orden asociada a una reserva.
     *
     * <p>Por cada ítem de pre-orden incluye el producto, la cantidad, el precio unitario
     * y las opciones de modificación de menú especial seleccionadas por el cliente.
     * Accesible por {@code CAJERO} y {@code MESERO} para pre-cargar la comanda.
     *
     * @param reservaId identificador de la reserva
     * @return lista de {@link PreOrdenDetalleResponse} con los ítems de la pre-orden;
     *         vacía si la reserva no tiene pre-orden
     * @throws co.edu.unicauca.backend.shared.exception.ResourceNotFoundException si la reserva
     *         no existe
     */
    @Transactional(readOnly = true)
    public List<PreOrdenDetalleResponse> obtenerPreOrden(Long reservaId) {

        // Verificar que la reserva exista antes de consultar su pre-orden.
        if (!reservaRepository.existsById(reservaId)) {
            throw new ResourceNotFoundException("Reserva", reservaId);
        }

        // Cargar los ítems de pre-orden ordenados cronológicamente.
        List<PreOrdenDetalle> detalles =
                preOrdenDetalleRepository.findByReserva_ReservaIdOrderByCreatedAtAsc(reservaId);

        // Mapear cada ítem junto con sus opciones de modificación de menú especial.
        return detalles.stream()
                .map(d -> {
                    // Cargar las modificaciones seleccionadas para este ítem concreto.
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
     *
     * <p>Horario de atención: lunes a domingo, {@code 17:00} (inclusive) a {@code 22:00} (exclusivo).
     *
     * @param fechaHora fecha y hora a verificar
     * @return {@code true} si la hora cae dentro del horario de atención; {@code false} en caso contrario
     */
    private boolean esHorarioValido(LocalDateTime fechaHora) {
        int hora = fechaHora.getHour();
        // Rango válido: [HORA_APERTURA, HORA_CIERRE) → inicio inclusivo, cierre exclusivo.
        return hora >= HORA_APERTURA && hora < HORA_CIERRE;
    }

    /**
     * Verifica si existe un bloqueo administrativo activo que cubra la fecha y hora indicadas.
     *
     * @param fechaHora fecha y hora a verificar
     * @return {@code true} si hay al menos un bloqueo activo para esa fecha/hora; {@code false} en caso contrario
     */
    private boolean estaBloqueda(LocalDateTime fechaHora) {
        // Descomponer en fecha y hora para la consulta al repositorio.
        LocalDate fecha = fechaHora.toLocalDate();
        LocalTime hora  = fechaHora.toLocalTime();
        return bloqueRepository.countBloquesParaFechaHora(fecha, hora) > 0;
    }

    /**
     * Verifica que la decoración seleccionada sea compatible con la zona elegida.
     *
     * <p>Si la decoración está asociada a una única zona, solo puede usarse en ella.
     * Si está asociada a varias zonas, la zona elegida debe estar entre las permitidas.
     * Si no tiene zonas asociadas, se considera compatible con cualquier zona.
     *
     * @param decoracion decoración seleccionada por el cliente
     * @param zona       zona elegida por el cliente
     * @throws co.edu.unicauca.backend.shared.exception.BusinessException si la decoración
     *         no es compatible con la zona indicada
     */
    private void validarCompatibilidadDecoracionZona(Decoracion decoracion, Zona zona) {

        // Obtener las zonas a las que está asignada esta decoración.
        List<DecoracionZona> links = decoracionZonaRepository
                .findByDecoracionId(decoracion.getDecoracionId());

        // Decoración exclusiva de una sola zona: la zona elegida debe coincidir exactamente.
        if (links.size() == 1) {
            Long zonaPermitida = links.get(0).getZonaId();
            if (!zonaPermitida.equals(zona.getZonaId())) {
                throw new BusinessException(ErrorCode.INVALID_STATE,
                        "La decoración seleccionada solo puede usarse en su zona asignada.",
                        HttpStatus.UNPROCESSABLE_ENTITY);
            }
        } else if (links.size() > 1) {
            // Decoración disponible en varias zonas: la zona elegida debe estar entre las permitidas.
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

    /**
     * Construye una respuesta de disponibilidad negativa con listas vacías.
     *
     * @return {@link DisponibilidadResponse} con {@code disponible = false} y colecciones vacías
     */
    private DisponibilidadResponse sinDisponibilidad() {
        return DisponibilidadResponse.builder()
                .disponible(false)
                .decoraciones(List.of())
                .zonas(List.of())
                .build();
    }

    /**
     * Construye el DTO de disponibilidad para una decoración, indicando con qué zonas libres es compatible.
     *
     * <p>Si la decoración está asociada a una única zona, {@code puedeSeleccionarZona} se establece
     * en {@code false} (la zona queda fijada implícitamente).
     *
     * @param d             decoración a mapear
     * @param idsZonasLibres conjunto de identificadores de zonas con capacidad disponible
     * @return {@link DecoracionDisponibleResponse} con las zonas compatibles y libres
     */
    private DecoracionDisponibleResponse buildDecoracionDto(Decoracion d, Set<Long> idsZonasLibres) {

        // Obtener las zonas asociadas a esta decoración para calcular compatibilidad.
        List<DecoracionZona> links = decoracionZonaRepository.findByDecoracionId(d.getDecoracionId());

        // Si tiene exactamente 1 zona asignada, la zona queda fijada: el cliente no puede elegirla.
        boolean puedeSeleccionar = links.size() != 1;

        // Intersección entre las zonas de la decoración y las zonas con cupo disponible.
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

    /**
     * Construye el DTO de disponibilidad para una zona del restaurante.
     *
     * @param z zona a mapear
     * @return {@link ZonaDisponibleResponse} con los datos básicos de la zona
     */
    private ZonaDisponibleResponse buildZonaDto(Zona z) {
        return ZonaDisponibleResponse.builder()
                .zonaId(z.getZonaId())
                .nombre(z.getZonaNombre())
                .imagenUrl(z.getZonaImagenUrl())
                .capacidad(z.getZonaCapacidadPersonas())
                .build();
    }

    /**
     * Convierte una entidad {@link Reserva} en su DTO de respuesta completo.
     *
     * <p>Carga los ítems de pre-orden desde la base de datos y calcula el total aproximado
     * sumando {@code precioUnitario × cantidad} de los ítems sin descripción libre.
     * {@code preOrdenItems} y {@code preOrdenTotal} son {@code null} si la reserva no tiene pre-orden.
     *
     * @param r entidad de reserva a convertir
     * @return {@link ReservaResponse} con todos los campos de la reserva y su pre-orden
     */
    private ReservaResponse toResponse(Reserva r) {

        // Cargar los ítems de pre-orden asociados a esta reserva.
        List<PreOrdenDetalle> detalles =
                preOrdenDetalleRepository.findByReserva_ReservaIdOrderByCreatedAtAsc(r.getReservaId());

        // Construir el resumen de ítems; null si no hay pre-orden.
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

        // Calcular el total sumando precio × cantidad; excluye ítems con descripción libre (precio TBD).
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
