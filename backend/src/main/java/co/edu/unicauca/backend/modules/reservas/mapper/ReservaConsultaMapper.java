package co.edu.unicauca.backend.modules.reservas.mapper;

import co.edu.unicauca.backend.modules.reservas.dto.response.*;
import co.edu.unicauca.backend.modules.reservas.entity.Reserva;
import co.edu.unicauca.backend.shared.enums.EstadoReserva;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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
     * <p>El campo {@code mostrarBotonInasistencia} se calcula dinámicamente:
     * {@code true} solo si la reserva está {@code CONFIRMADA} y han transcurrido
     * 30+ minutos desde {@code reservaFechaHoraLlegada}.
     *
     * @param reserva entidad de reserva a convertir
     * @return {@link ReservaConsultaResponse} con los campos del listado
     */
    public ReservaConsultaResponse toConsultaResponse(Reserva reserva) {
        // Calcular si debe mostrar el botón "Marcar inasistencia"
        boolean mostrarBotonInasistencia = false;
        if (reserva.getReservaEstado() == EstadoReserva.CONFIRMADA) {
            LocalDateTime horaLimite = reserva.getReservaFechaHoraLlegada().plusMinutes(30);
            mostrarBotonInasistencia = LocalDateTime.now().isAfter(horaLimite);
        }

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
                .clienteTelefono(reserva.getCliente().getClienteTelefono())
                .estado(reserva.getReservaEstado().name())
                .mostrarBotonInasistencia(mostrarBotonInasistencia)
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
