package co.edu.unicauca.backend.modules.mesas_comandas.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

/**
 * Vista completa del formulario de modificar comanda. Agrupa los ítems por
 * estación, expone los identificadores de las dos comandas BORRADOR posibles
 * y los flags que habilitan los botones de envío a producción.
 */
@Getter @Builder
public class BorradorComandaResponse {
    /** Identificador de la visita dueña del borrador. */
    private final Long visitaId;

    /** Identificador legible de la mesa. */
    private final String mesaIdentificador;

    /** Identificador de la comanda BORRADOR de cocina; {@code null} si no existe. */
    private final Long comandaCocinaId;

    /** Identificador de la comanda BORRADOR de barra; {@code null} si no existe. */
    private final Long comandaBarraId;

    /** Ítems con categoría {@code PLATO}, ordenados alfabéticamente. */
    private final List<ItemBorradorResponse> platos;

    /** Ítems con categoría {@code BEBIDA}, ordenados alfabéticamente. */
    private final List<ItemBorradorResponse> bebidas;

    /** SubTotal acumulado de todas las comandas de la visita en BORRADOR. */
    private final BigDecimal subTotal;

    /** Total acumulado de todas las comandas de la visita. */
    private final BigDecimal total;
    
    /** Notas para la cocina; {@code null} si la comanda BORRADOR de cocina no existe o no tiene notas. */
    private final String notasCocina;

    /** Notas para la barra; {@code null} si la comanda BORRADOR de barra no existe o no tiene notas. */
    private final String notasBarra;

    /** {@code true} cuando {@code platos} no está vacío; habilita el envío a cocina. */
    private final Boolean puedeEnviarCocina;
    
    /** {@code true} cuando {@code bebidas} no está vacío; habilita el envío a barra. */
    private final Boolean puedeEnviarBarra;
}
