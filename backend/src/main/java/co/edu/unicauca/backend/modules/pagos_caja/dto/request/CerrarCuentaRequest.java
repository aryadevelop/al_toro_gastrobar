package co.edu.unicauca.backend.modules.pagos_caja.dto.request;

import co.edu.unicauca.backend.shared.enums.MetodoPago;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO de entrada para cerrar la cuenta de una visita y registrar la venta.
 *
 * <p>El subtotal y el total NO se reciben del cliente: se recalculan en el servidor
 * a partir de las comandas persistidas para evitar manipulación. El cajero solo
 * aporta el descuento aplicado y el método de pago.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CerrarCuentaRequest {

    /** Correo del cajero que realiza el cierre de cuenta; obligatorio. */
    @NotNull(message = "El email del cajero es obligatorio")
    @Email(message = "El email del cajero no tiene un formato válido")
    private String emailCajero;

    /** Identificador de la visita cuya cuenta se va a cerrar; obligatorio. */
    @NotNull(message = "El identificador de la visita es obligatorio")
    private Long visitaId;

    /**
     * Descuento aplicado a la cuenta; no puede ser negativo.
     * Si no se envía, el sistema asume {@code 0.00}.
     */
    @DecimalMin(value = "0.00", message = "El descuento no puede ser negativo")
    @Digits(integer = 10, fraction = 2,
            message = "El descuento debe tener máximo 10 dígitos enteros y 2 decimales")
    private BigDecimal descuento;

    /** Método de pago utilizado para cerrar la cuenta; obligatorio. */
    @NotNull(message = "Se debe seleccionar el método de pago")
    private MetodoPago metodo;
}
