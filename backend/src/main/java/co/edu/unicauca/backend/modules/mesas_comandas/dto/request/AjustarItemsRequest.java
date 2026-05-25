package co.edu.unicauca.backend.modules.mesas_comandas.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Payload del ajuste batch de ítems de la cuenta por parte del cajero.
 *
 * <p>Contiene los ítems a modificar (cantidad y/o precio) y los ítems a eliminar,
 * aplicados de forma transaccional. Los menús especiales mantienen su par COCINA+BARRA
 * en sincronía (la lógica del servicio resuelve el acoplamiento).
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AjustarItemsRequest {

    /** Ítems a modificar; puede ser {@code null} o vacía si solo se eliminan ítems. */
    @Valid
    private List<ItemAjuste> items;

    /** Identificadores de ítems a eliminar; puede ser {@code null} o vacía. */
    private List<Long> eliminados;

    /**
     * Cambio individual sobre un ítem de comanda.
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemAjuste {

        /** Identificador del ítem de comanda a ajustar; obligatorio. */
        @NotNull(message = "El identificador del ítem es obligatorio")
        private Long comandaItemId;

        /** Nueva cantidad; rango [1, 250] (alineado con el borrador del mesero). */
        @NotNull(message = "La cantidad es obligatoria")
        @Min(value = 1, message = "La cantidad mínima es 1")
        @Max(value = 250, message = "La cantidad máxima es 250")
        private Integer cantidad;

        /**
         * Nuevo precio unitario; opcional. Solo se admite en ítems modificados
         * (con descripción). {@code null} = no cambiar el precio.
         */
        @DecimalMin(value = "0.00", message = "El precio no puede ser negativo")
        @Digits(integer = 10, fraction = 2,
                message = "El precio debe tener máximo 10 dígitos enteros y 2 decimales")
        private BigDecimal precio;
    }
}
