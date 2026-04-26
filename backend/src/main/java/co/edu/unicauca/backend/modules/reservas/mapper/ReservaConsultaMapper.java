package co.edu.unicauca.backend.modules.reservas.mapper;

import co.edu.unicauca.backend.modules.reservas.dto.response.*;
import co.edu.unicauca.backend.modules.reservas.entity.Reserva;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Mapper para convertir entidades de Reserva en DTOs de consulta para meseros.
 *
 * <p>Transforma {@link Reserva} en los formatos necesarios para el listado y
 * reutiliza {@link ReservaDetalleResponse} para el detalle completo.
 *
 * @see ReservaConsultaService
 */
@Component
public class ReservaConsultaMapper {

    private static final DateTimeFormatter FORMATTER_TIME = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * Convierte una {@link Reserva} en el DTO de ítem del listado.
     *
     * <p>Campos opcionales (zona, decoración) se omiten ({@code null}) si no fueron asignados.
     *
     * @param reserva entidad de reserva a convertir
     * @return {@link ReservaConsultaResponse} con los campos del listado
     */
    public ReservaConsultaResponse toConsultaResponse(Reserva reserva) {
        return ReservaConsultaResponse.builder()
                .reservaId(reserva.getReservaId())
                .clienteNombre(reserva.getCliente().getClienteNombre())
                .zonaId(reserva.getZona() != null ? reserva.getZona().getZonaId() : null)
                .zonaNombre(reserva.getZona() != null ? reserva.getZona().getZonaNombre() : null)
                .decoracionNombre(reserva.getDecoracion() != null
                        ? reserva.getDecoracion().getDecoracionNombre()
                        : null)
                .horaLlegada(reserva.getReservaFechaHoraLlegada().format(FORMATTER_TIME))
                .numeroPersonas(reserva.getReservaNumeroPersonas())
                .estado(reserva.getReservaEstado().name())
                .build();
    }

    /**
     * Construye el DTO de resumen por zona a partir del conteo de reservas.
     *
     * @param zonaId           identificador de la zona
     * @param zonaNombre       nombre de la zona
     * @param cantidadReservas cantidad de reservas en esa zona
     * @return {@link ResumenZonaResponse} con los datos del resumen
     */
    public ResumenZonaResponse toResumenZona(Long zonaId, String zonaNombre, Integer cantidadReservas) {
        return ResumenZonaResponse.builder()
                .zonaId(zonaId)
                .zonaNombre(zonaNombre)
                .cantidadReservas(cantidadReservas)
                .build();
    }
    
}
