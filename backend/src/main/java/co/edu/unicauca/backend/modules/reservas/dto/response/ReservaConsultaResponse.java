package co.edu.unicauca.backend.modules.reservas.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

/**
 * DTO de ítem en el listado de reservas para meseros.
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReservaConsultaResponse {

    /** Identificador único de la reserva. */
    private final Long reservaId;

    /** Nombre completo del cliente. */
    private final String clienteNombre;

    /** Identificador de la zona; {@code null} si no fue asignada. */
    private final Long zonaId;

    /** Nombre de la zona; {@code null} si no fue asignada. */
    private final String zonaNombre;

    /** Nombre de la decoración; {@code null} si no fue asignada. */
    private final String decoracionNombre;

    /** Hora de llegada en formato {@code HH:mm} (ej: {@code "19:30"}). */
    private final String horaLlegada;

    /** Número de comensales de la reserva. */
    private final Integer numeroPersonas;

    /** Teléfono del cliente que realizó la reserva. */
    private final String clienteTelefono;

    /** Estado actual de la reserva ({@code CONFIRMADA} o {@code PENDIENTE}). */
    private final String estado;

    /**
     * Indica si debe mostrarse el botón "Marcar inasistencia".
     *
     * <p>Solo se calcula en la vista de mesero; queda {@code null} en la vista de cajero.
     */
    private final Boolean mostrarBotonInasistencia;

    /**
     * Tipo de reserva: {@code BASICA} o {@code ESPECIAL}.
     *
     * <p>Solo se incluye en la vista de cajero; queda {@code null} en la vista de mesero.
     */
    private final String tipo;

    /**
     * Indica si debe mostrarse el botón "Confirmar" (vista de cajero).
     *
     * <p>{@code true} cuando la reserva es {@code ESPECIAL} y está {@code PENDIENTE};
     * {@code null} en la vista de mesero.
     */
    private final Boolean mostrarConfirmar;

    /**
     * Indica si debe mostrarse el botón "Agregar anticipo" (vista de cajero).
     *
     * <p>{@code true} cuando la reserva está {@code CONFIRMADA}; {@code null} en la vista de mesero.
     */
    private final Boolean mostrarAgregarAnticipo;

    /**
     * Indica si debe mostrarse el botón "Agregar devolución" (vista de cajero).
     *
     * <p>{@code true} cuando la reserva está {@code CANCELADA} y tiene al menos un abono
     * registrado; {@code null} en la vista de mesero.
     */
    private final Boolean mostrarAgregarDevolucion;

    /**
     * Indica si debe mostrarse el botón "Cancelar" (vista de cajero).
     *
     * <p>{@code true} cuando la reserva está {@code PENDIENTE} o {@code CONFIRMADA};
     * {@code null} en la vista de mesero.
     */
    private final Boolean mostrarCancelar;
}
