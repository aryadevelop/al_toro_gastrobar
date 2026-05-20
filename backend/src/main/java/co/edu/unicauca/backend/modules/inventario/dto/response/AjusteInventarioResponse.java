package co.edu.unicauca.backend.modules.inventario.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * Resultado de registrar un ajuste manual de inventario.
 */
@Getter
@Builder
public class AjusteInventarioResponse {

    /** Identificador del movimiento de inventario creado. */
    private final Long movimientoId;

    /** Stock resultante luego del ajuste. */
    private final BigDecimal stockActualizado;

    /** Número de comandas {@code PENDIENTE} notificadas por el cambio; cero en ingresos. */
    private final int comandasNotificadas;
}
