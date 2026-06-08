package co.edu.unicauca.backend.modules.reservas.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO de respuesta con el detalle completo de una reserva.
 *
 * <p>Incluye los datos de la reserva, la pre-orden solicitada al momento de reservar
 * y el historial de abonos registrados por caja.
 *
 * <p>Campos opcionales (pueden ser {@code null} si no aplican):
 * <ul>
 *   <li>{@code zonaId} y {@code zonaNombre} — si el cliente no seleccionó zona.</li>
 *   <li>{@code decoracionId} y {@code decoracionNombre} — si la reserva no incluyó decoración.</li>
 *   <li>{@code notas} — si no se registraron observaciones especiales.</li>
 *   <li>{@code preOrdenItems} — si la reserva no tiene pre-orden.</li>
 *   <li>{@code valorDecoracion} — si la reserva no tiene decoración o esta no tiene costo.</li>
 *   <li>{@code abonos} — si no se registraron anticipos.</li>
 * </ul>
 *
 * <p>Importes siempre presentes: {@code totalPreorden}, {@code totalAPagar} y
 * {@code montoAbonado} (neto). El saldo se expone de forma direccional según el estado:
 * {@code saldoPendiente} (lo que falta por pagar) solo en reservas activas
 * ({@code PENDIENTE}/{@code CONFIRMADA}); {@code pendientePorDevolver} (neto a reembolsar)
 * solo en {@code CANCELADA}/{@code DEVUELTA}. Ambos se omiten en {@code ATENDIDA}/{@code INASISTENCIA}.
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReservaDetalleResponse {

    /** Identificador único de la reserva. */
    private final Long reservaId;
    
    /** Identificador del cliente que realizó la reserva; {@code null} si no aplica. */
    private final Long clienteId;

    /** Nombre del cliente; {@code null} en consultas de cliente autenticado. */
    private final String clienteNombre;

    /** Teléfono del cliente; {@code null} en consultas de cliente. */
    private final String clienteTelefono;

    /** Fecha y hora de llegada programada en formato {@code yyyy-MM-dd'T'HH:mm:ss}. */
    private final String fechaHoraLlegada;

    /** Número de comensales de la reserva. */
    private final Integer numeroPersonas;

    /** Estado actual de la reserva. */
    private final String estado;

    /** Tipo de reserva: {@code BASICA} o {@code ESPECIAL}. */
    private final String tipo;

    /** Identificador de la zona seleccionada; {@code null} si el cliente no eligió zona. */
    private final Long zonaId;

    /** Identificador de la decoración seleccionada; {@code null} si no aplica. */
    private final Long decoracionId;

    /** Nombre de la zona seleccionada; {@code null} si el cliente no eligió zona. */
    private final String zonaNombre;

    /** Nombre de la decoración seleccionada; {@code null} si no aplica. */
    private final String decoracionNombre;

    /**
     * Productos solicitados en la pre-orden al crear la reserva;
     * {@code null} si la reserva no tiene pre-orden.
     */
    private final List<PreOrdenItemResponse> preOrdenItems;

    /**
     * Total de la pre-orden (suma de precio × cantidad de todos los ítems);
     * {@code 0} si no hay pre-orden.
     */
    private final BigDecimal totalPreorden;

    /**
     * Costo adicional cobrado por la decoración seleccionada;
     * {@code null} si la reserva no incluye decoración o si la decoración no tiene costo.
     */
    private final BigDecimal valorDecoracion;

    /**
     * Total a pagar de la reserva ({@code totalPreorden + valorDecoracion}).
     */
    private final BigDecimal totalAPagar;

    /**
     * Historial de abonos y devoluciones registrados para esta reserva;
     * {@code null} si no se registró ningún abono.
     */
    private final List<AbonoItemResponse> abonos;

    /**
     * Monto neto abonado ({@code anticipos − devoluciones}); {@code 0} si no hubo abonos.
     */
    private final BigDecimal montoAbonado;

    /**
     * Saldo que el cliente aún debe pagar ({@code max(totalAPagar − montoAbonado, 0)}).
     * Presente solo en reservas activas ({@code PENDIENTE}/{@code CONFIRMADA});
     * {@code null} (omitido) en estados terminales.
     *
     * <p>Nombre y semántica alineados con {@link ResumenPagoResponse}.
     */
    private final BigDecimal saldoPendiente;

    /**
     * Monto que el restaurante debe devolver al cliente (igual al neto {@code montoAbonado}).
     * Presente solo cuando la reserva está {@code CANCELADA} o {@code DEVUELTA}
     * (en esta última será {@code 0} tras un reembolso total); {@code null} en otros estados.
     *
     * <p>Nombre y semántica alineados con {@link ResumenPagoResponse}.
     */
    private final BigDecimal pendientePorDevolver;

    /** Observaciones o peticiones especiales del cliente; {@code null} si no hay notas. */
    private final String notas;

    /**
     * {@code true} si la reserva puede modificarse (estado activo y antes de las 16:00
     * del día de llegada); {@code false} en caso contrario.
     */
    private final boolean modificable;

}
