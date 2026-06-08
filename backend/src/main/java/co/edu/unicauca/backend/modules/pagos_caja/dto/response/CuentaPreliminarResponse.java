package co.edu.unicauca.backend.modules.pagos_caja.dto.response;

import co.edu.unicauca.backend.modules.reservas.dto.response.AbonoItemResponse;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

/**
 * Cuenta preliminar de una mesa para el cierre de venta.
 *
 * <p>Consolida los datos que el cajero necesita antes de registrar el pago:
 * identificación del cliente y sus puntos, ítems consumidos, decoración, totales
 * y el estado de abonos (anticipos) con su saldo pendiente. Los campos nulos se
 * omiten de la respuesta.
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CuentaPreliminarResponse {

    /** Identificador de la visita. */
    private final Long visitaId;

    /** Identificador del cliente; {@code null} si la mesa es de un invitado. */
    private final Long clienteId;

    /** Nombre del cliente; {@code null} si es invitado. */
    private final String clienteNombre;

    /** Correo del cliente; {@code null} si es invitado. */
    private final String clienteEmail;

    /** Saldo de puntos canjeables del cliente; {@code null} si es invitado. */
    private final Integer puntosCanjeables;

    /** Puntos acumulados de por vida; {@code null} si es invitado. */
    private final Integer puntosAcumulados;

    /** Fecha/hora de llegada (formato yyyy-MM-dd'T'HH:mm:ss). */
    private final String fechaHoraLlegada;

    /** Nombre del mesero que atendió; {@code null} si no hay mesa asignada. */
    private final String meseroNombre;

    /** Identificador visible de la mesa; {@code null} si no hay mesa asignada. */
    private final String mesaIdentificador;

    /** Ítems consumidos, ordenados por categoría (PLATO→BEBIDA→OTRO). */
    private final List<CuentaItemResponse> items;

    /** Nombre de la decoración con costo; {@code null} si no aplica. */
    private final String decoracionNombre;

    /** Costo adicional de la decoración; {@code null} si no aplica. */
    private final BigDecimal valorDecoracion;

    /** Total de la pre-orden (suma de subtotales de ítems). */
    private final BigDecimal totalPreorden;

    /** Total a pagar ({@code totalPreorden + valorDecoracion}). */
    private final BigDecimal totalAPagar;

    /** Lista de anticipos/devoluciones; {@code null} si la visita no tiene reserva con abonos. */
    private final List<AbonoItemResponse> anticipos;

    /** Monto neto abonado (anticipos − devoluciones); {@code 0} si no hubo abonos. */
    private final BigDecimal montoAbonado;

    /** Saldo pendiente ({@code totalAPagar − montoAbonado}). */
    private final BigDecimal saldoPendiente;
}
