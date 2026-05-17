package co.edu.unicauca.backend.modules.mesas_comandas.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Representación del tablero de comandas en producción, organizado en tres
 * columnas según el estado actual de cada comanda.
 *
 * <p>Cuando el usuario tiene activas las estaciones {@code COCINA} y
 * {@code BARRA} simultáneamente, las listas combinan comandas de ambas
 * estaciones; el campo {@code estaciones} expone qué estaciones cubre la
 * respuesta y cada comanda lleva la suya en
 * {@link ComandaProduccionResumenResponse#getEstacion()}.
 */
@Getter
@Builder
public class TableroProduccionResponse {

    /**
     * Estaciones cubiertas por el tablero. Posibles valores:
     * {@code ["COCINA"]}, {@code ["BARRA"]} o {@code ["BARRA","COCINA"]}.
     */
    private final List<String> estaciones;

    /** Comandas en estado {@code PENDIENTE} ordenadas de más antigua a más reciente. */
    private final List<ComandaProduccionResumenResponse> pendientes;

    /** Comandas en estado {@code EN_PREPARACION} ordenadas de más antigua a más reciente. */
    private final List<ComandaProduccionResumenResponse> enPreparacion;

    /** Comandas en estado {@code LISTO} ordenadas de más antigua a más reciente. */
    private final List<ComandaProduccionResumenResponse> listos;
}
