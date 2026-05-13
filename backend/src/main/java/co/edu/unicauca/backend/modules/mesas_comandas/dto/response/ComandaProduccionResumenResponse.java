package co.edu.unicauca.backend.modules.mesas_comandas.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Representación reducida de una comanda destinada a las pantallas de
 * producción. Contiene los campos mínimos para identificar la comanda, su
 * estado y su antigüedad sin necesidad de cargar el detalle completo de ítems.
 *
 * <p>Los tres timestamps que expone permiten al cliente mostrar la antigüedad
 * relativa de la comanda según la columna en la que se renderice.
 */
@Getter
@Builder
public class ComandaProduccionResumenResponse {

    /** Identificador de la comanda. */
    private final Long comandaId;

    /** Estación de producción de la comanda: {@code "COCINA"} o {@code "BARRA"}. */
    private final String estacion;

    /** Estado actual: {@code "PENDIENTE"}, {@code "EN_PREPARACION"} o {@code "LISTO"}. */
    private final String comandaEstado;

    /** Etiqueta legible de la mesa asociada. */
    private final String mesaIdentificador;

    /** Nombre del mesero responsable de la mesa. */
    private final String meseroNombre;

    /** Suma de las cantidades de todos los ítems de la comanda. */
    private final Integer totalItems;

    /** Fecha y hora en que se creó el registro de la comanda. */
    private final LocalDateTime createdAt;

    /**
     * Fecha y hora en que la comanda fue enviada a producción. Vale
     * {@code null} mientras la comanda no haya transicionado a {@code PENDIENTE}.
     */
    private final LocalDateTime fechaHoraInicio;

    /**
     * Fecha y hora en que la comanda fue marcada como {@code LISTO}. Vale
     * {@code null} mientras la comanda no haya alcanzado ese estado.
     */
    private final LocalDateTime fechaHoraListo;

    /** Notas para la estación de producción; {@code null} si no aplica. */
    private final String notas;
}
