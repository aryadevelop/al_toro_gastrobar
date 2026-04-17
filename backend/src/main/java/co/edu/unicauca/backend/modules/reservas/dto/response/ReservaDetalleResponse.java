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
 *   <li>{@code zonaNombre} — si el cliente no seleccionó zona.</li>
 *   <li>{@code decoracionNombre} — si la reserva no incluyó decoración.</li>
 *   <li>{@code notas} — si no se registraron observaciones especiales.</li>
 *   <li>{@code preOrdenItems} y {@code preOrdenTotal} — si la reserva no tiene pre-orden.</li>
 *   <li>{@code abonos} y {@code totalAbonado} — si no se registraron anticipos.</li>
 * </ul>
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReservaDetalleResponse {

    /** Identificador único de la reserva. */
    private final Long reservaId;

    /** Fecha y hora de llegada programada en formato {@code yyyy-MM-dd'T'HH:mm:ss}. */
    private final String fechaHoraLlegada;

    /** Número de comensales de la reserva. */
    private final Integer numeroPersonas;

    /** Estado actual de la reserva. */
    private final String estado;

    /** Tipo de reserva: {@code BASICA} o {@code ESPECIAL}. */
    private final String tipo;

    /** Nombre de la zona seleccionada; {@code null} si el cliente no eligió zona. */
    private final String zonaNombre;

    /** Nombre de la decoración seleccionada; {@code null} si no aplica. */
    private final String decoracionNombre;

    /** Observaciones o peticiones especiales del cliente; {@code null} si no hay notas. */
    private final String notas;

    /**
     * Productos solicitados en la pre-orden al crear la reserva;
     * {@code null} si la reserva no tiene pre-orden.
     */
    private final List<PreOrdenItemResponse> preOrdenItems;

    /**
     * Suma de precio × cantidad de todos los ítems de la pre-orden;
     * {@code null} si no hay pre-orden.
     */
    private final BigDecimal preOrdenTotal;

    /**
     * Historial de abonos y devoluciones registrados para esta reserva;
     * {@code null} si no se registró ningún abono.
     */
    private final List<AbonoItemResponse> abonos;

    /**
     * Suma de todos los abonos registrados (anticipos y devoluciones);
     * {@code null} si no se registró ningún abono.
     */
    private final BigDecimal totalAbonado;
}
