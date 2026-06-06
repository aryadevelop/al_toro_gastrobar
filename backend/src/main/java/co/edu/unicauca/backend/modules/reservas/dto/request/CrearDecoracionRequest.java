package co.edu.unicauca.backend.modules.reservas.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrearDecoracionRequest {

    @NotBlank(message = "El nombre de la decoración es obligatorio")
    @Size(max = 100, message = "El nombre no debe exceder 100 caracteres")
    private String decoracionNombre;

    @DecimalMin(value = "1.00", message = "El costo adicional debe ser mayor o igual a 1.00")
    @Digits(integer = 10, fraction = 2, message = "El costo debe tener máximo 10 dígitos enteros y 2 decimales")
    private BigDecimal decoracionCostoAdicional;

    private List<Long> zonaIds;
}
