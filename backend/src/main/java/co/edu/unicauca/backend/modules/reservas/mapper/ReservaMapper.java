package co.edu.unicauca.backend.modules.reservas.mapper;

import co.edu.unicauca.backend.modules.mesas_comandas.entity.ComandaItem;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Zona;
import co.edu.unicauca.backend.modules.pagos_caja.entity.Abono;
import co.edu.unicauca.backend.modules.reservas.dto.response.AbonoItemResponse;
import co.edu.unicauca.backend.modules.reservas.dto.response.DecoracionDisponibleResponse;
import co.edu.unicauca.backend.modules.reservas.dto.response.DisponibilidadResponse;
import co.edu.unicauca.backend.modules.reservas.dto.response.PreOrdenItemResponse;
import co.edu.unicauca.backend.modules.reservas.dto.response.CancelarReservaResponse;
import co.edu.unicauca.backend.modules.reservas.dto.response.ModificarReservaResponse;
import co.edu.unicauca.backend.modules.reservas.dto.response.ReservaDetalleResponse;
import co.edu.unicauca.backend.modules.reservas.dto.response.ReservaResponse;
import co.edu.unicauca.backend.modules.reservas.dto.response.ZonaDisponibleResponse;
import co.edu.unicauca.backend.modules.reservas.entity.Decoracion;
import co.edu.unicauca.backend.modules.reservas.entity.DecoracionZona;
import co.edu.unicauca.backend.modules.reservas.entity.Reserva;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import co.edu.unicauca.backend.shared.enums.EstadoReserva;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Mapper para convertir entidades del módulo de reservas en sus DTOs de respuesta.
 *
 * <p>Cubre la conversión de {@link Reserva} junto con sus {@link ComandaItem} de pre-orden,
 * {@link co.edu.unicauca.backend.modules.pagos_caja.entity.Abono abonos}, zonas y
 * decoraciones. El mapeo de los ítems de pre-orden se delega en {@link PreOrdenMapper}.
 *
 * @see co.edu.unicauca.backend.modules.reservas.service.ReservaService
 * @see PreOrdenMapper
 */
@Component
@RequiredArgsConstructor
public class ReservaMapper {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final PreOrdenMapper preOrdenMapper;

    /**
     * Convierte una {@link Reserva} en el DTO de respuesta incluyendo información de WhatsApp.
     *
     * @param reserva          entidad de reserva a convertir
     * @param requiereWhatsApp {@code true} si la reserva es ESPECIAL y requiere anticipo
     * @param mensajeWhatsApp  mensaje precompuesto; {@code null} cuando no aplica
     * @return {@link ReservaResponse} con todos los campos de la reserva
     */
    public ReservaResponse toResponse(Reserva reserva, boolean requiereWhatsApp, String mensajeWhatsApp) {
        return ReservaResponse.builder()
                .reservaId(reserva.getReservaId())
                .fechaHoraLlegada(reserva.getReservaFechaHoraLlegada().format(FORMATTER))
                .numeroPersonas(reserva.getReservaNumeroPersonas())
                .estado(reserva.getReservaEstado().name())
                .tipo(reserva.getReservaTipo().name())
                .decoracionNombre(reserva.getDecoracion() != null ? reserva.getDecoracion().getDecoracionNombre() : null)
                .zonaNombre(reserva.getZona() != null ? reserva.getZona().getZonaNombre() : null)
                .notas(reserva.getReservaNotas())
                .clienteId(reserva.getCliente().getUsuarioId())
                .clienteNombre(reserva.getCliente().getClienteNombre())
                .requiereWhatsApp(requiereWhatsApp ? true : null)
                .mensajeWhatsApp(mensajeWhatsApp)
                .build();
    }

    /**
     * Convierte una {@link Decoracion} en su DTO de disponibilidad.
     *
     * @param decoracion     decoración a convertir
     * @param links          relaciones decoración-zona precargadas
     * @param idsZonasLibres IDs de zonas con capacidad disponible
     * @return {@link DecoracionDisponibleResponse} con datos de presentación y compatibilidad
     */
    public DecoracionDisponibleResponse toDecoracionDto(Decoracion decoracion, List<DecoracionZona> links, Set<Long> idsZonasLibres) {
        // Si solo hay un link, no se muestra la opción de seleccionar zona
        boolean puedeSeleccionar = links.size() != 1;

        // Se consideran compatibles las zonas libres que están vinculadas a la decoración
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
     * Convierte una {@link Zona} en su DTO de disponibilidad.
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
     * Convierte una {@link Reserva} en el DTO resumido para el listado del dashboard.
     *
     * @param reserva entidad de reserva a convertir
     * @return {@link ReservaDetalleResponse} con los campos básicos
     */
    public ReservaDetalleResponse toResumen(Reserva reserva) {
        return ReservaDetalleResponse.builder()
                .reservaId(reserva.getReservaId())
                .fechaHoraLlegada(reserva.getReservaFechaHoraLlegada().format(FORMATTER))
                .numeroPersonas(reserva.getReservaNumeroPersonas())
                .estado(reserva.getReservaEstado().name())
                .tipo(reserva.getReservaTipo().name())
                .zonaId(reserva.getZona() != null ? reserva.getZona().getZonaId() : null)
                .decoracionId(reserva.getDecoracion() != null ? reserva.getDecoracion().getDecoracionId() : null)
                .zonaNombre(reserva.getZona() != null ? reserva.getZona().getZonaNombre() : null)
                .decoracionNombre(reserva.getDecoracion() != null ? reserva.getDecoracion().getDecoracionNombre() : null)
                .modificable(esModificable(reserva))
                .build();
    }

    /**
     * Construye una respuesta de disponibilidad negativa con listas vacías.
     *
     * @return {@link DisponibilidadResponse} con {@code disponible = false}
     */
    public DisponibilidadResponse sinDisponibilidad() {
        return DisponibilidadResponse.builder()
                .disponible(false)
                .decoraciones(List.of())
                .zonas(List.of())
                .build();
    }

    /**
     * Convierte una {@link Reserva} con su pre-orden y abonos en el DTO de detalle completo.
     *
     * @param reserva  entidad de reserva
     * @param preOrden ítems de comanda PRE_RESERVA (puede ser vacía)
     * @param abonos   abonos asociados (puede ser vacía)
     * @return {@link ReservaDetalleResponse} con todos los campos del detalle
     */
    public ReservaDetalleResponse toDetalleResponse(Reserva reserva,
                                                    List<ComandaItem> preOrden,
                                                    List<Abono> abonos) {
        // Si la reserva no tiene pre-orden, se dejan los campos relacionados como null para omitirlos en la respuesta.
        List<PreOrdenItemResponse> preOrdenItems = preOrden.isEmpty() ? null
                : construirItemsPreOrden(preOrden);

        BigDecimal preOrdenTotal = preOrden.isEmpty() ? null :
                preOrden.stream()
                        .map(d -> d.getComandaItemPrecio()
                                .multiply(BigDecimal.valueOf(d.getComandaItemCantidad())))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Si no hay abonos, se dejan los campos relacionados como null para omitirlos en la respuesta.
        List<AbonoItemResponse> abonosDto = abonos.isEmpty() ? null
                : construirAbonosDto(abonos);

        BigDecimal totalAbonado = abonos.isEmpty() ? null :
                abonos.stream()
                        .map(Abono::getAbonoMonto)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        return ReservaDetalleResponse.builder()
                .reservaId(reserva.getReservaId())
                .fechaHoraLlegada(reserva.getReservaFechaHoraLlegada().format(FORMATTER))
                .numeroPersonas(reserva.getReservaNumeroPersonas())
                .estado(reserva.getReservaEstado().name())
                .tipo(reserva.getReservaTipo().name())
                .zonaId(reserva.getZona() != null ? reserva.getZona().getZonaId() : null)
                .decoracionId(reserva.getDecoracion() != null ? reserva.getDecoracion().getDecoracionId() : null)
                .zonaNombre(reserva.getZona() != null ? reserva.getZona().getZonaNombre() : null)
                .decoracionNombre(reserva.getDecoracion() != null
                        ? reserva.getDecoracion().getDecoracionNombre() : null)
                .notas(reserva.getReservaNotas())
                .clienteTelefono(reserva.getCliente() != null ? reserva.getCliente().getClienteTelefono() : null)
                .preOrdenItems(preOrdenItems)
                .preOrdenTotal(preOrdenTotal)
                .abonos(abonosDto)
                .totalAbonado(totalAbonado)
                .modificable(esModificable(reserva))
                .build();
    }

    /**
     * Construye el DTO de respuesta para una modificación de reserva.
     *
     * @param reserva          entidad resultante (puede ser nueva en transición ESPECIAL→BASICA)
     * @param requiereWhatsApp {@code true} si la transición de tipo requiere contacto vía WhatsApp
     * @param mensajeWhatsApp  mensaje precompuesto; {@code null} cuando no se requiere WhatsApp
     * @return {@link ModificarReservaResponse} con los datos de la reserva resultante
     */
    public ModificarReservaResponse toModificarResponse(Reserva reserva,
                                                         boolean requiereWhatsApp,
                                                         String mensajeWhatsApp) {
        return ModificarReservaResponse.builder()
                .reservaId(reserva.getReservaId())
                .estado(reserva.getReservaEstado().name())
                .tipo(reserva.getReservaTipo().name())
                .fechaHoraLlegada(reserva.getReservaFechaHoraLlegada().format(FORMATTER))
                .numeroPersonas(reserva.getReservaNumeroPersonas())
                .zonaNombre(reserva.getZona() != null ? reserva.getZona().getZonaNombre() : null)
                .decoracionNombre(reserva.getDecoracion() != null
                        ? reserva.getDecoracion().getDecoracionNombre() : null)
                .notas(reserva.getReservaNotas())
                .requiereWhatsApp(requiereWhatsApp)
                .mensajeWhatsApp(mensajeWhatsApp)
                .build();
    }

    private List<PreOrdenItemResponse> construirItemsPreOrden(List<ComandaItem> items) {
        return items.stream()
                .map(d -> preOrdenMapper.toDetalleResponse(d, List.of()))
                .collect(Collectors.toList());
    }

    private List<AbonoItemResponse> construirAbonosDto(List<Abono> abonos) {
        return abonos.stream()
                .map(a -> AbonoItemResponse.builder()
                        .abonoId(a.getAbonoId())
                        .monto(a.getAbonoMonto())
                        .fechaHora(a.getAbonoFechaHora().format(FORMATTER))
                        .metodo(a.getAbonoMetodo().name())
                        .tipo(a.getAbonoTipo().name())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Convierte una {@link Reserva} cancelada en su DTO de respuesta para la operación de
     * cancelación.
     *
     * @param reserva           entidad actualizada con estado {@code CANCELADA}
     * @param requiereWhatsApp  {@code true} si el cliente debe gestionar el reembolso por WhatsApp
     * @param mensajeWhatsApp   mensaje precompuesto para el chat; {@code null} si no aplica
     * @return {@link CancelarReservaResponse} con los datos de la reserva cancelada
     */
    public CancelarReservaResponse toCancelarResponse(Reserva reserva,
                                                       boolean requiereWhatsApp,
                                                       String mensajeWhatsApp) {
        return CancelarReservaResponse.builder()
                .reservaId(reserva.getReservaId())
                .estado(reserva.getReservaEstado().name())
                .tipo(reserva.getReservaTipo().name())
                .fechaHoraLlegada(reserva.getReservaFechaHoraLlegada().format(FORMATTER))
                .numeroPersonas(reserva.getReservaNumeroPersonas())
                .requiereWhatsApp(requiereWhatsApp)
                .mensajeWhatsApp(mensajeWhatsApp)
                .build();
    }

    /**
     * Calcula si una reserva puede ser modificada por el cliente.
     *
     * <p>Una reserva es modificable si su estado es {@code PENDIENTE} o {@code CONFIRMADA}
     * y el momento actual es anterior a las 16:00 del día de llegada.
     */
    private boolean esModificable(Reserva reserva) {
        // Estado terminal — no puede modificarse
        if (reserva.getReservaEstado() != EstadoReserva.PENDIENTE
                && reserva.getReservaEstado() != EstadoReserva.CONFIRMADA) {
            return false;
        }
        // Calcular límite: 16:00 del día de la reserva
        LocalDateTime limiteModificacion = reserva.getReservaFechaHoraLlegada()
                .toLocalDate().atTime(LocalTime.of(16, 0));
        // Comparar con el instante actual
        return LocalDateTime.now().isBefore(limiteModificacion);
    }
}
