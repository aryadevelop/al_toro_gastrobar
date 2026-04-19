package co.edu.unicauca.backend.modules.reservas.service;

import co.edu.unicauca.backend.modules.mesas_comandas.entity.Zona;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ZonaRepository;
import co.edu.unicauca.backend.modules.reservas.dto.response.DecoracionDisponibleResponse;
import co.edu.unicauca.backend.modules.reservas.dto.response.DisponibilidadResponse;
import co.edu.unicauca.backend.modules.reservas.dto.response.ZonaDisponibleResponse;
import co.edu.unicauca.backend.modules.reservas.entity.Decoracion;
import co.edu.unicauca.backend.modules.reservas.entity.DecoracionZona;
import co.edu.unicauca.backend.modules.reservas.mapper.ReservaMapper;
import co.edu.unicauca.backend.modules.reservas.repository.DecoracionRepository;
import co.edu.unicauca.backend.modules.reservas.repository.DecoracionZonaRepository;
import co.edu.unicauca.backend.modules.reservas.repository.ReservaRepository;
import co.edu.unicauca.backend.shared.enums.EstadoGenerico;
import co.edu.unicauca.backend.shared.enums.EstadoReserva;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Componente encargado de consultar la disponibilidad del restaurante para una fecha y hora dadas.
 *
 * <p>Encapsula la lógica unificada de disponibilidad extraída de {@link ReservaService},
 * ofreciendo dos puntos de entrada públicos:
 * <ul>
 *   <li>{@link #consultarParaNuevaReserva} — para cuando se crea una nueva reserva
 *       (no excluye ninguna reserva existente de los conteos).</li>
 *   <li>{@link #consultarParaModificacion} — para cuando se modifica una reserva existente
 *       (excluye la reserva que se está editando de los conteos de ocupación).</li>
 * </ul>
 *
 * <p>Ambos métodos delegan en {@link #consultarDisponibilidadInterna}, que centraliza
 * las validaciones de horario, bloqueos y cálculo de zonas y decoraciones disponibles.
 *
 * @see ReservaService
 * @see ReservaValidador
 * @see ReservaMapper
 */
@Component
@RequiredArgsConstructor
public class DisponibilidadConsultador {

    // -----------------------------------------------------------------------
    // Estados de reserva considerados "activos" para los cálculos de ocupación
    // -----------------------------------------------------------------------

    /**
     * Lista de estados de reserva que se consideran activos (ocupan cupo).
     *
     * <p>Solo las reservas en alguno de estos estados se computan al calcular
     * la disponibilidad de zonas y decoraciones.
     */
    private final List<EstadoReserva> estadosActivos =
            List.of(EstadoReserva.PENDIENTE, EstadoReserva.CONFIRMADA);

    // -----------------------------------------------------------------------
    // Dependencias inyectadas
    // -----------------------------------------------------------------------

    /** Validador de horario y bloqueos administrativos. */
    private final ReservaValidador validador;

    /** Mapper para construir los DTOs de disponibilidad. */
    private final ReservaMapper reservaMapper;

    /** Repositorio de reservas para las consultas de ocupación por zona y decoración. */
    private final ReservaRepository reservaRepository;

    /** Repositorio de zonas del restaurante. */
    private final ZonaRepository zonaRepository;

    /** Repositorio de decoraciones disponibles. */
    private final DecoracionRepository decoracionRepository;

    /** Repositorio de asociaciones entre decoraciones y zonas. */
    private final DecoracionZonaRepository decoracionZonaRepository;

    // -----------------------------------------------------------------------
    // Record interno: rango de tiempo que abarca un día completo
    // -----------------------------------------------------------------------

    /**
     * Representa el inicio (00:00:00) y el fin (23:59:59) del día de una fecha dada.
     *
     * <p>Se usa para acotar las consultas de ocupación al día calendario correspondiente
     * a la fecha y hora de llegada solicitada.
     */
    private record RangoDia(LocalDateTime inicio, LocalDateTime fin) {

        /**
         * Crea un {@link RangoDia} a partir de una fecha y hora arbitraria.
         *
         * @param fechaHora fecha y hora de llegada solicitada
         * @return rango que va desde las 00:00:00 hasta las 23:59:59 del mismo día
         */
        static RangoDia desde(LocalDateTime fechaHora) {
            // Inicio del día: primer instante del día calendario de la fecha dada
            LocalDateTime inicio = fechaHora.toLocalDate().atStartOfDay();
            // Fin del día: último segundo del día calendario de la fecha dada
            LocalDateTime fin = fechaHora.toLocalDate().atTime(23, 59, 59);
            return new RangoDia(inicio, fin);
        }
    }

    // -----------------------------------------------------------------------
    // Métodos públicos
    // -----------------------------------------------------------------------

    /**
     * Consulta la disponibilidad del restaurante para crear una nueva reserva.
     *
     * <p>No excluye ninguna reserva de los conteos; equivale a llamar
     * {@link #consultarDisponibilidadInterna} con {@code excludeReservaId = null}.
     *
     * @param fechaHora    fecha y hora de llegada solicitada
     * @param numeroPersonas número de comensales (no usado en el filtrado global, pero
     *                       disponible para extensiones futuras)
     * @param horaApertura hora de apertura del restaurante (pasada desde el servicio invocante)
     * @param horaCierre   hora de cierre del restaurante (pasada desde el servicio invocante)
     * @return {@link DisponibilidadResponse} con las zonas y decoraciones libres,
     *         o con {@code disponible = false} si no hay disponibilidad
     */
    @Transactional(readOnly = true)
    public DisponibilidadResponse consultarParaNuevaReserva(
            LocalDateTime fechaHora,
            int numeroPersonas,
            LocalTime horaApertura,
            LocalTime horaCierre) {

        // Delegar a la lógica interna sin excluir ninguna reserva existente
        return consultarDisponibilidadInterna(fechaHora, numeroPersonas, null, horaApertura, horaCierre);
    }

    /**
     * Consulta la disponibilidad del restaurante para modificar una reserva existente.
     *
     * <p>Excluye la reserva indicada por {@code excludeReservaId} de todos los conteos
     * de ocupación, de modo que la reserva actual no bloquee su propia zona ni decoración.
     *
     * @param fechaHora        nueva fecha y hora de llegada solicitada
     * @param numeroPersonas   número de comensales de la reserva modificada
     * @param excludeReservaId ID de la reserva que se está modificando (excluida de conteos)
     * @param horaApertura     hora de apertura del restaurante
     * @param horaCierre       hora de cierre del restaurante
     * @return {@link DisponibilidadResponse} con las zonas y decoraciones libres excluyendo
     *         la reserva actual, o con {@code disponible = false} si no hay disponibilidad
     */
    @Transactional(readOnly = true)
    public DisponibilidadResponse consultarParaModificacion(
            LocalDateTime fechaHora,
            int numeroPersonas,
            Long excludeReservaId,
            LocalTime horaApertura,
            LocalTime horaCierre) {

        // Delegar a la lógica interna excluyendo la reserva que se está editando
        return consultarDisponibilidadInterna(fechaHora, numeroPersonas, excludeReservaId, horaApertura, horaCierre);
    }

    // -----------------------------------------------------------------------
    // Lógica interna unificada
    // -----------------------------------------------------------------------

    /**
     * Lógica central de consulta de disponibilidad, compartida por creación y modificación.
     *
     * <p>Pasos que ejecuta:
     * <ol>
     *   <li>Valida que {@code fechaHora} esté dentro del horario de atención.</li>
     *   <li>Verifica que no exista un bloqueo administrativo para esa fecha/hora.</li>
     *   <li>Obtiene todas las zonas y calcula cuántas personas ya hay reservadas por zona
     *       en el día solicitado (opcionalmente excluyendo una reserva específica).</li>
     *   <li>Filtra las zonas con cupo restante.</li>
     *   <li>Determina qué decoraciones activas están libres en ese día.</li>
     *   <li>Construye y retorna el {@link DisponibilidadResponse}.</li>
     * </ol>
     *
     * @param fechaHora        fecha y hora de llegada solicitada
     * @param numeroPersonas   número de comensales (disponible para extensiones futuras)
     * @param excludeReservaId ID de reserva a excluir de los conteos; {@code null} al crear
     * @param horaApertura     hora de apertura del restaurante (inclusive)
     * @param horaCierre       hora de cierre del restaurante (exclusivo)
     * @return {@link DisponibilidadResponse} con zonas y decoraciones libres,
     *         o con {@code disponible = false} si no se cumple alguna condición
     */
    private DisponibilidadResponse consultarDisponibilidadInterna(
            LocalDateTime fechaHora,
            int numeroPersonas,
            Long excludeReservaId,
            LocalTime horaApertura,
            LocalTime horaCierre) {

        // Paso 1: verificar que la hora esté dentro del horario de atención
        if (!validador.esHorarioValido(fechaHora, horaApertura, horaCierre)) {
            // Hora fuera del rango de atención: retornar sin disponibilidad
            return reservaMapper.sinDisponibilidad();
        }

        // Paso 2: verificar que no exista un bloqueo administrativo para esa fecha/hora
        if (validador.estaBloqueda(fechaHora)) {
            // Existe un bloqueo activo para ese instante: retornar sin disponibilidad
            return reservaMapper.sinDisponibilidad();
        }

        // Paso 3: obtener todas las zonas del restaurante
        List<Zona> todasLasZonas = zonaRepository.findAll();

        // Si no hay zonas registradas, no puede haber disponibilidad
        if (todasLasZonas.isEmpty()) {
            return reservaMapper.sinDisponibilidad();
        }

        // Calcular el rango completo del día de la fecha solicitada
        RangoDia rango = RangoDia.desde(fechaHora);

        // Paso 4: consultar personas reservadas por zona en el día; excluir la reserva actual si aplica
        List<Object[]> filas = excludeReservaId == null
                // Consulta normal: incluye todas las reservas activas del día
                ? reservaRepository.findPersonasPorZonaEnDia(rango.inicio(), rango.fin(), estadosActivos)
                // Consulta de modificación: excluye la reserva que se está editando
                : reservaRepository.findPersonasPorZonaEnDiaExcluyendo(
                        rango.inicio(), rango.fin(), estadosActivos, excludeReservaId);

        // Convertir el resultado de la query a un mapa zonaId → personas ocupadas
        Map<Long, Integer> personasPorZona = mapearPersonasPorZona(filas);

        // Paso 5: filtrar zonas con cupo restante (personas ocupadas < capacidad total)
        List<Zona> zonasLibres = todasLasZonas.stream()
                .filter(z -> personasPorZona.getOrDefault(z.getZonaId(), 0)
                             < z.getZonaCapacidadPersonas())
                .collect(Collectors.toList());

        // Si ninguna zona tiene cupo, el restaurante está completo para ese día
        if (zonasLibres.isEmpty()) {
            return reservaMapper.sinDisponibilidad();
        }

        // Obtener el conjunto de IDs de zonas libres para filtrar compatibilidad de decoraciones
        Set<Long> idsZonasLibres = zonasLibres.stream()
                .map(Zona::getZonaId)
                .collect(Collectors.toSet());

        // Paso 6: determinar qué decoraciones están ocupadas en el día; excluir la reserva actual si aplica
        Set<Long> decoracionesOcupadas = excludeReservaId == null
                // Consulta normal: incluye todas las reservas activas del día
                ? Set.copyOf(reservaRepository.findDecoracionesOcupadasEnDia(
                        rango.inicio(), rango.fin(), estadosActivos))
                // Consulta de modificación: excluye la reserva que se está editando
                : Set.copyOf(reservaRepository.findDecoracionesOcupadasEnDiaExcluyendo(
                        rango.inicio(), rango.fin(), estadosActivos, excludeReservaId));

        // Obtener las decoraciones activas que no están ocupadas ese día
        List<Decoracion> decoracionesActivas = decoracionRepository
                .findByDecoracionEstado(EstadoGenerico.ACTIVO)
                .stream()
                .filter(d -> !decoracionesOcupadas.contains(d.getDecoracionId()))
                .collect(Collectors.toList());

        // Mapear cada decoración activa a su DTO, incluyendo las zonas compatibles libres
        List<DecoracionDisponibleResponse> decoracionesDto = decoracionesActivas.stream()
                .map(d -> {
                    // Cargar los enlaces decoración-zona para calcular compatibilidad
                    List<DecoracionZona> links = decoracionZonaRepository.findByDecoracionId(d.getDecoracionId());
                    // Convertir al DTO usando el mapper, filtrando solo las zonas libres compatibles
                    return reservaMapper.toDecoracionDto(d, links, idsZonasLibres);
                })
                .collect(Collectors.toList());

        // Mapear las zonas libres a sus DTOs de disponibilidad
        List<ZonaDisponibleResponse> zonasDto = zonasLibres.stream()
                .map(reservaMapper::toZonaDto)
                .collect(Collectors.toList());

        // Construir y retornar la respuesta con disponibilidad positiva
        return DisponibilidadResponse.builder()
                .disponible(true)
                .decoraciones(decoracionesDto)
                .zonas(zonasDto)
                .build();
    }

    // -----------------------------------------------------------------------
    // Helpers privados estáticos
    // -----------------------------------------------------------------------

    /**
     * Convierte el resultado crudo de una query nativa {@code [zonaId, personas]} en un mapa
     * {@code Map<Long, Integer>} con el total de personas reservadas por zona.
     *
     * <p>El array {@code Object[]} de cada fila contiene:
     * <ul>
     *   <li>{@code [0]} — {@code Long} con el identificador de la zona.</li>
     *   <li>{@code [1]} — {@code Number} con la suma de personas en esa zona.</li>
     * </ul>
     *
     * @param filas resultado devuelto por las queries del repositorio
     * @return mapa de zonaId → número de personas reservadas; vacío si no hay filas
     */
    private static Map<Long, Integer> mapearPersonasPorZona(List<Object[]> filas) {
        // Iterar sobre las filas y convertir cada par [zonaId, personas] al mapa resultante
        return filas.stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],          // clave: ID de la zona (Long)
                        row -> ((Number) row[1]).intValue() // valor: suma de personas (int)
                ));
    }
}
