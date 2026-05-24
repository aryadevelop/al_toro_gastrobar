package co.edu.unicauca.backend.modules.reservas.dto.request;

import co.edu.unicauca.backend.shared.enums.MetodoPago;
import co.edu.unicauca.backend.shared.enums.TipoAbono;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Payload que envía el frontend al registrar un anticipo o devolución sobre una reserva.
 *
 * <p>Todos los campos son obligatorios; el monto debe ser mayor a cero.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistrarAbonoRequest {

    /**
     * Tipo de movimiento: {@code ANTICIPO} o {@code DEVOLUCION}.
     * Obligatorio.
     */
    @NotNull(message = "Debe seleccionar el tipo de abono")
    private TipoAbono tipo;

    /**
     * Importe del abono o devolución en pesos colombianos.
     * Debe ser mayor a cero; obligatorio.
     */
    @NotNull(message = "El monto es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto debe ser mayor a cero")
    private BigDecimal monto;

    /**
     * Medio de pago utilizado para el abono: {@code EFECTIVO}, {@code TARJETA}, etc.
     * Obligatorio.
     */
    @NotNull(message = "Debe seleccionar un método de pago")
    private MetodoPago metodo;

    /**
     * Fecha y hora en que se realiza el movimiento.
     * Obligatorio.
     */
    @NotNull(message = "Debe seleccionar una fecha")
    private LocalDateTime fechaHora;
}
