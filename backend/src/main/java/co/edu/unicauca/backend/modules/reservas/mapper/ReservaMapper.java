package co.edu.unicauca.backend.modules.reservas.mapper;

import co.edu.unicauca.backend.modules.mesas_comandas.entity.Zona;
import co.edu.unicauca.backend.modules.reservas.dto.response.DecoracionDisponibleResponse;
import co.edu.unicauca.backend.modules.reservas.dto.response.DisponibilidadResponse;
import co.edu.unicauca.backend.modules.reservas.dto.response.PreOrdenItemResumenResponse;
import co.edu.unicauca.backend.modules.reservas.dto.response.ReservaResponse;
import co.edu.unicauca.backend.modules.reservas.dto.response.ZonaDisponibleResponse;
import co.edu.unicauca.backend.modules.reservas.entity.Decoracion;
import co.edu.unicauca.backend.modules.reservas.entity.DecoracionZona;
import co.edu.unicauca.backend.modules.reservas.entity.PreOrdenDetalle;
import co.edu.unicauca.backend.modules.reservas.entity.Reserva;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Mapper para convertir entidades del módulo de reservas en sus DTOs de respuesta.
 * 
 * Delega la conversión de ítems de pre-orden en {@link PreOrdenMapper}.
 */
@Component
@RequiredArgsConstructor
public class ReservaMapper {

    /** Formateador sin zona horaria para las fechas de respuesta. */
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final PreOrdenMapper preOrdenMapper;

    /**
     * Convierte una entidad {@link Reserva} y sus detalles de pre-orden en el DTO de respuesta completo.
     *
     * @param reserva  entidad de reserva a convertir
     * @param detalles lista de detalles de pre-orden asociados (puede estar vacía)
     * @return {@link ReservaResponse} con todos los campos de la reserva y el resumen de pre-orden
     */
    public ReservaResponse toResponse(Reserva reserva, List<PreOrdenDetalle> detalles) {
        // Convierte los detalles a resúmenes de ítem; null si no hay pre-orden
        List<PreOrdenItemResumenResponse> preOrdenItems = detalles.isEmpty() ? null :
                detalles.stream()
                        .map(preOrdenMapper::toItemResumen)
                        .collect(Collectors.toList());

        // Calcula el total sumando precio × cantidad de todos los ítems de pre-orden.
        // Retorna null si no hay pre-orden.
        BigDecimal preOrdenTotal = detalles.isEmpty() ? null :
                detalles.stream()
                        .map(d -> d.getProducto().getProductoPrecio()
                                .multiply(BigDecimal.valueOf(d.getPreordenDetalleCantidad())))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        return ReservaResponse.builder()
                .reservaId(reserva.getReservaId())
                .fechaHoraLlegada(reserva.getReservaFechaHoraLlegada().format(FORMATTER))
                .numeroPersonas(reserva.getReservaNumeroPersonas())
                .estado(reserva.getReservaEstado().name())
                .tipo(reserva.getReservaTipo().name())
                .decoracionId(reserva.getDecoracion() != null ? reserva.getDecoracion().getDecoracionId() : null)
                .decoracionNombre(reserva.getDecoracion() != null ? reserva.getDecoracion().getDecoracionNombre() : null)
                .zonaId(reserva.getZona() != null ? reserva.getZona().getZonaId() : null)
                .zonaNombre(reserva.getZona() != null ? reserva.getZona().getZonaNombre() : null)
                .notas(reserva.getReservaNotas())
                .fechaCreacion(reserva.getReservaFechaCreacion().format(FORMATTER))
                .clienteId(reserva.getCliente().getUsuarioId())
                .clienteNombre(reserva.getCliente().getClienteNombre())
                .preOrdenItems(preOrdenItems)
                .preOrdenTotal(preOrdenTotal)
                .build();
    }

    /**
     * Convierte una entidad {@link Decoracion} en su DTO de disponibilidad, indicando con qué
     * zonas libres es compatible y si el cliente puede seleccionar zona manualmente.
     *
     * @param decoracion     decoración a convertir
     * @param links          relaciones decoración-zona precargadas por el servicio
     * @param idsZonasLibres IDs de zonas con capacidad disponible en el día consultado
     * @return {@link DecoracionDisponibleResponse} con datos de presentación y compatibilidad
     */
    public DecoracionDisponibleResponse toDecoracionDto(Decoracion decoracion,
                                                        List<DecoracionZona> links,
                                                        Set<Long> idsZonasLibres) {
        // Si la decoración solo tiene una zona asignada, el cliente no puede elegir zona
        boolean puedeSeleccionar = links.size() != 1;

        // Solo incluye zonas compatibles que además estén libres en el día consultado
        List<Long> zonaIdsCompatibles = links.stream()
                .map(DecoracionZona::getZonaId)
                .filter(idsZonasLibres::contains)
                .collect(Collectors.toList());

        return DecoracionDisponibleResponse.builder()
                .decoracionId(decoracion.getDecoracionId())
                .nombre(decoracion.getDecoracionNombre())
                .imagenUrl(decoracion.getDecoracionImagenUrl())
                .puedeSeleccionarZona(puedeSeleccionar)
                .zonaIdsCompatibles(zonaIdsCompatibles)
                .build();
    }

    /**
     * Convierte una entidad {@link Zona} en su DTO de disponibilidad con los campos básicos
     * de presentación (id, nombre, imagen y capacidad).
     *
     * @param zona zona a convertir
     * @return {@link ZonaDisponibleResponse} con los datos de presentación de la zona
     */
    public ZonaDisponibleResponse toZonaDto(Zona zona) {
        return ZonaDisponibleResponse.builder()
                .zonaId(zona.getZonaId())
                .nombre(zona.getZonaNombre())
                .imagenUrl(zona.getZonaImagenUrl())
                .capacidad(zona.getZonaCapacidadPersonas())
                .build();
    }

    /**
     * Construye una respuesta de disponibilidad negativa con listas vacías.
     *
     * @return {@link DisponibilidadResponse} con {@code disponible = false} y colecciones vacías
     */
    public DisponibilidadResponse sinDisponibilidad() {
        return DisponibilidadResponse.builder()
                .disponible(false)
                .decoraciones(List.of())
                .zonas(List.of())
                .build();
    }
}
