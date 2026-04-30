package co.edu.unicauca.backend.modules.mesas_comandas.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import co.edu.unicauca.backend.modules.reservas.dto.response.AbonoItemResponse;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO de respuesta con el detalle completo de una visita,
 * 
 * <p>Reúne en un único objeto toda la información relevante para el cliente:
 * datos de la reserva, productos consumidos, historial de abonos y total de cuenta.
 *
 * <p>Campos opcionales (pueden ser {@code null} si no aplican):
 * <ul>
 *   <li>{@code zonaNombre} — si el cliente no seleccionó zona.</li>
 *   <li>{@code decoracionNombre} — si la reserva no incluyó decoración.</li>
 *   <li>{@code notas} — si no se registraron observaciones especiales.</li>
 *   <li>{@code itemsComanda} — si la visita no generó comanda.</li>
 *   <li>{@code abonos} — si no se registraron anticipos.</li>
 *   <li>{@code totalCuenta} — si la cuenta aún no fue cerrada por caja.</li>
 * </ul>
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VisitaDetalleResponse {

    /** Identificador de la visita registrada. */
    private final Long visitaId;

    /** Identificador único de la reserva asociada; {@code null} para walk-ins. */
    private final Long reservaId;

    /** Identificador del cliente; {@code null} para visitas anónimas. */
    private final Long clienteId;

    /** Nombre del cliente; {@code null} para visitas anónimas. */
    private final String clienteNombre;

    /** Fecha y hora de llegada programada en formato {@code yyyy-MM-dd'T'HH:mm:ss}. */
    private final String fechaHoraLlegada;

    /** Fecha y hora de salida en formato {@code yyyy-MM-dd'T'HH:mm:ss}; {@code null} si la visita está activa. */
    private final String fechaHoraSalida;

    /** Número de comensales de la reserva. */
    private final Integer numeroPersonas;

    /** Nombre de la zona seleccionada; {@code null} si el cliente no eligió zona. */
    private final String zonaNombre;

    /** Nombre de la decoración seleccionada; {@code null} si no aplica. */
    private final String decoracionNombre;

    /** Etiqueta física de la mesa asignada; {@code null} si no hubo mesa. */
    private final String mesaIdentificador;

    /** Nombre del mesero que atendió la mesa; {@code null} si no hubo asignación. */
    private final String meseroNombre;

    /**
     * Productos de la comanda final (suma de todas las comandas de la visita);
     * {@code null} si la visita no generó comandas.
     */
    private final List<ItemComandaResponse> itemsComanda;

    /**
     * Historial de abonos y devoluciones registrados para esta reserva;
     * {@code null} si no se registró ningún abono.
     */
    private final List<AbonoItemResponse> abonos;

    /**
     * Suma de ítems antes de descuentos;
     * {@code null} si la cuenta aún no fue cerrada.
     */
    private final BigDecimal subtotalCuenta;

    /**
     * Monto descontado del subtotal;
     * {@code null} si la cuenta aún no fue cerrada.
     */
    private final BigDecimal descuentoCuenta;

    /**
     * Monto total de la cuenta cerrada por caja;
     * {@code null} si la cuenta aún no fue cerrada.
     */
    private final BigDecimal totalCuenta;
}
