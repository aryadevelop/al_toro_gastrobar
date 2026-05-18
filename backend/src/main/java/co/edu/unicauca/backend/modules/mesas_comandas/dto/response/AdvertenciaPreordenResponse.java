package co.edu.unicauca.backend.modules.mesas_comandas.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * Advertencia de stock insuficiente para un ítem del borrador de comanda.
 *
 * <p>Se incluye en {@link BorradorComandaResponse#getAdvertenciasPreorden()} cuando
 * la cantidad solicitada en el borrador excede las unidades realmente disponibles.
 * La advertencia es informativa; no bloquea ninguna operación.
 */
@Getter
@Builder
public class AdvertenciaPreordenResponse {

    /** Identificador del producto con stock insuficiente. */
    private final Long productoId;

    /** Nombre del producto. */
    private final String productoNombre;

    /** Unidades actualmente en el borrador. */
    private final Integer cantidadEnComanda;

    /**
     * Unidades realmente disponibles considerando el stock comprometido
     * en otros borradores y comandas activas ({@code max(disponible, 0)}).
     */
    private final Integer cantidadDisponible;
}
