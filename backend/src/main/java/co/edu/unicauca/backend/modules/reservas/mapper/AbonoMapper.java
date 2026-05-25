package co.edu.unicauca.backend.modules.reservas.mapper;

import co.edu.unicauca.backend.modules.pagos_caja.entity.Abono;
import co.edu.unicauca.backend.modules.reservas.dto.request.RegistrarAbonoRequest;
import co.edu.unicauca.backend.modules.reservas.dto.response.RegistrarAbonoResponse;
import co.edu.unicauca.backend.modules.reservas.dto.response.ResumenPagoResponse;
import co.edu.unicauca.backend.modules.reservas.entity.Reserva;
import co.edu.unicauca.backend.modules.usuarios.entity.Empleado;
import co.edu.unicauca.backend.shared.enums.EstadoReserva;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

/**
 * Mapper para convertir entre la entidad {@link Abono} y los DTOs de abono del módulo de reservas.
 *
 * <p>Centraliza la lógica de construcción de {@link Abono} a partir de un request entrante,
 * el cálculo del resumen financiero de una reserva y la composición de la respuesta de registro.
 * Ningún método modifica datos; la transformación es puramente de representación.
 *
 * @see co.edu.unicauca.backend.modules.reservas.service.ReservaService
 */
@Component
public class AbonoMapper {

    /** Formato de fecha y hora utilizado en todos los campos de tipo texto de la respuesta. */
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    /**
     * Construye un {@link Abono} a partir del payload del request, la reserva y el cajero.
     *
     * <p>No asigna {@code abonoId}; la base de datos lo genera al persistir la entidad.
     * La fecha y hora del abono se toma directamente del request.
     *
     * @param request request con monto, método, tipo y fecha del movimiento
     * @param reserva reserva sobre la que se registra el abono
     * @param cajero  empleado de caja que registra el movimiento
     * @return {@link Abono} listo para persistir; sin ID asignado
     */
    public Abono toEntity(RegistrarAbonoRequest request, Reserva reserva, Empleado cajero) {
        return Abono.builder()
                .reserva(reserva)
                .cajero(cajero)
                .abonoMonto(request.getMonto())
                .abonoMetodo(request.getMetodo())
                .abonoTipo(request.getTipo())
                .abonoFechaHora(request.getFechaHora())
                .build();
    }

    /**
     * Construye el resumen financiero de una reserva a partir de los totales calculados.
     *
     * <p>Calcula {@code montoAbonado = totalAnticipado - totalDevuelto}.
     * Los campos de saldo pendiente se incluyen condicionalmente según el estado de la reserva:
     * <ul>
     *   <li>{@code CONFIRMADA} — {@code saldoPendiente = max(totalAPagar − neto, 0)};
     *       {@code pendientePorDevolver} es {@code null} (se omite en el JSON).</li>
     *   <li>{@code CANCELADA} — {@code pendientePorDevolver = neto};
     *       {@code saldoPendiente} es {@code null}.</li>
     *   <li>{@code DEVUELTA} — {@code pendientePorDevolver = neto} (será 0 tras reembolso total);
     *       {@code saldoPendiente} es {@code null}.</li>
     *   <li>Cualquier otro estado — ambos campos son {@code null}.</li>
     * </ul>
     *
     * @param reserva          entidad de la reserva cuyo resumen se construye
     * @param totalReserva     valor total de la reserva (pre-orden más decoración)
     * @param totalAnticipado  suma de todos los anticipos registrados para la reserva
     * @param totalDevuelto    suma de todas las devoluciones registradas para la reserva
     * @return {@link ResumenPagoResponse} con los campos financieros calculados
     */
    public ResumenPagoResponse toResumenPago(Reserva reserva, BigDecimal totalReserva,
                                             BigDecimal totalAnticipado, BigDecimal totalDevuelto) {
        // Neto = anticipos acumulados menos devoluciones ya registradas
        BigDecimal neto = totalAnticipado.subtract(totalDevuelto);

        // Campos pendientes condicionales según el estado de la reserva
        EstadoReserva estado = reserva.getReservaEstado();
        BigDecimal pendienteAbonar = (estado == EstadoReserva.CONFIRMADA)
                ? totalReserva.subtract(neto).max(BigDecimal.ZERO)
                : null;
        BigDecimal pendienteDevolver = (estado == EstadoReserva.CANCELADA || estado == EstadoReserva.DEVUELTA)
                ? neto
                : null;

        return ResumenPagoResponse.builder()
                .reservaId(reserva.getReservaId())
                .clienteNombre(reserva.getCliente().getClienteNombre())
                .fechaHoraLlegada(reserva.getReservaFechaHoraLlegada().format(FORMATTER))
                .numeroPersonas(reserva.getReservaNumeroPersonas())
                .estado(reserva.getReservaEstado().name())
                .tipo(reserva.getReservaTipo().name())
                .totalAPagar(totalReserva)
                .totalAnticipado(totalAnticipado)
                .totalDevuelto(totalDevuelto)
                .montoAbonado(neto)
                .saldoPendiente(pendienteAbonar)
                .pendientePorDevolver(pendienteDevolver)
                .build();
    }

    /**
     * Construye la respuesta devuelta al registrar un anticipo o devolución.
     *
     * <p>Combina el identificador y tipo del abono recién persistido con el resumen
     * financiero actualizado de la reserva afectada.
     *
     * @param abono   entidad persistida con su {@code abonoId} asignado
     * @param resumen resumen financiero calculado tras aplicar el movimiento
     * @return {@link RegistrarAbonoResponse} con abonoId, tipo, estado y resumen
     */
    public RegistrarAbonoResponse toRegistrarResponse(Abono abono, ResumenPagoResponse resumen) {
        return RegistrarAbonoResponse.builder()
                .abonoId(abono.getAbonoId())
                .tipo(abono.getAbonoTipo().name())
                .estado(resumen.getEstado())
                .resumen(resumen)
                .build();
    }
}
