package co.edu.unicauca.backend.modules.inventario.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request para actualizar los datos de un insumo (nombre, costo, fecha de vencimiento).
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActualizarInsumoRequest {

    @NotBlank(message = "El nombre del insumo es obligatorio")
    @Size(max = 100, message = "El nombre no debe exceder 100 caracteres")
    private String insumoNombre;

    @DecimalMin(value = "0.00", message = "El costo unitario no puede ser negativo")
    @Digits(integer = 10, fraction = 2, message = "El costo unitario debe tener máximo 10 dígitos enteros y 2 decimales")
    private BigDecimal insumoCosoUnitario;

    /** Fecha de vencimiento; puede ser nula si no aplica. */
    private LocalDate insumoFechaVencimiento;
}
