package co.edu.unicauca.backend.modules.mesas_comandas.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Representación detallada de una comanda visible en el tablero de producción.
 *
 * <p>Incluye los timestamps de las transiciones relevantes, los datos de mesa
 * y mesero y la totalidad de los ítems agrupados por categoría de producto.
 * Solo se entrega cuando el estado de la comanda corresponde a alguno de los
 * estados visibles en producción ({@code PENDIENTE}, {@code EN_PREPARACION} o
 * {@code LISTO}) y la estación de la comanda coincide con la del solicitante.
 */
@Getter
@Builder
public class ComandaProduccionDetalleResponse {

    /** Identificador de la comanda. */
    private final Long comandaId;

    /** Estación productora: {@code "COCINA"} o {@code "BARRA"}. */
    private final String estacion;

    /** Estado actual de la comanda. */
    private final String comandaEstado;

    /** Etiqueta legible de la mesa asociada. */
    private final String mesaIdentificador;

    /** Nombre del mesero responsable. */
    private final String meseroNombre;

    /** Fecha y hora en que se creó el registro de la comanda. */
    private final LocalDateTime createdAt;

    /**
     * Fecha y hora en que la comanda fue enviada a producción. Vale
     * {@code null} cuando la comanda nunca alcanzó ese estado.
     */
    private final LocalDateTime fechaHoraInicio;

    /**
     * Fecha y hora en que la comanda fue marcada como {@code LISTO}. Vale
     * {@code null} mientras la comanda no haya alcanzado ese estado.
     */
    private final LocalDateTime fechaHoraListo;

    /** Notas para la estación de producción; {@code null} si no aplica. */
    private final String notas;

    /** Ítems de categoría {@code PLATO} ordenados alfabéticamente por nombre. */
    private final List<ItemDetalleResponse> platos;

    /** Ítems de categoría {@code BEBIDA} ordenados alfabéticamente por nombre. */
    private final List<ItemDetalleResponse> bebidas;

    /** Ítems de categoría {@code OTRO} ordenados alfabéticamente por nombre. */
    private final List<ItemDetalleResponse> otros;
}
