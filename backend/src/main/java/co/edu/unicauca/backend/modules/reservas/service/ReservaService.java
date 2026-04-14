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
import co.edu.unicauca.backend.modules.reservas.entity.Reserva;
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
import org.springframework.http.HttpStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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

    private final ReservaRepository reservaRepository;
    private final DecoracionRepository decoracionRepository;
    private final DecoracionZonaRepository decoracionZonaRepository;
    private final ZonaRepository zonaRepository;
    private final ClienteRepository clienteRepository;

    // -----------------------------------------------------------------------
    // Disponibilidad
    // -----------------------------------------------------------------------

    /**
     * Retorna la disponibilidad para el día de la fecha dada.
     * Una zona está libre si la suma de personas de sus reservas activas en ese día es menor que su capacidad.
     * Si ninguna zona tiene capacidad restante, {@code disponible} es {@code false}.
     */
    @Transactional(readOnly = true)
    public DisponibilidadResponse consultarDisponibilidad(LocalDateTime fechaHora) {
        List<Zona> todasLasZonas = zonaRepository.findAll();

        if (todasLasZonas.isEmpty()) {
            return DisponibilidadResponse.builder()
                    .disponible(false)
                    .decoraciones(List.of())
                    .zonas(List.of())
                    .build();
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
            return DisponibilidadResponse.builder()
                    .disponible(false)
                    .decoraciones(List.of())
                    .zonas(List.of())
                    .build();
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
     * Verifica disponibilidad en el momento de guardar.
     */
    @Transactional
    public ReservaResponse crearReserva(String emailCliente, CrearReservaRequest request) {
        Cliente cliente = clienteRepository.findByUsuario_UsuarioEmail(emailCliente)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cliente", "email", emailCliente));

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
        // es una regla dura de negocio (zona asignada fija) que no depende del estado actual de reservas.
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
            // Verificar disponibilidad en el momento actual (→ 422 si no hay capacidad)
            boolean zonaLibre = disponibilidad.getZonas().stream()
                    .anyMatch(z -> z.getZonaId().equals(zonaId));
            if (!zonaLibre) {
                throw new BusinessException(ErrorCode.INVALID_STATE, MSG_DISPONIBILIDAD_CAMBIO,
                        HttpStatus.UNPROCESSABLE_ENTITY);
            }

            // Verificar que la suma total del día + nuevas personas no supere la capacidad
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

        // Una reserva es especial si la decoración elegida tiene costo adicional > 0 o si se elige un menú especial
        boolean tieneDecoracionConCosto = decoracion != null &&
                decoracion.getDecoracionCostoAdicional() != null &&
                decoracion.getDecoracionCostoAdicional().compareTo(java.math.BigDecimal.ZERO) > 0;

        //OJO cambiar para verificar el menú especial cuando se implemente       
        boolean tieneMenuEspecial = request.getNumeroPersonas() != null && request.getNumeroPersonas() >= 10;

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
        return toResponse(guardada);
    }

    // -----------------------------------------------------------------------
    // Reservas cliente
    // -----------------------------------------------------------------------

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

    // -----------------------------------------------------------------------
    // Validaciones de negocio
    // -----------------------------------------------------------------------

    /**
     * Verifica que la decoración sea compatible con la zona elegida.
     * Reglas:
     *  - 1 fila en decoracion_zona → zona fija, debe coincidir exactamente.
     *  - >1 filas → la zona elegida debe estar en la lista compatible.
     *  - 0 filas → sin restricción de zona.
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

    private DecoracionDisponibleResponse buildDecoracionDto(Decoracion d, Set<Long> idsZonasLibres) {
        List<DecoracionZona> links = decoracionZonaRepository.findByDecoracionId(d.getDecoracionId());

        // 1 sola zona = decoración fija 
        boolean puedeSeleccionar = links.size() != 1;
        
        // Zonas compatibles libres: si no hay links, lista vacía = sin restricción de zona
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
                .build();
    }
}
