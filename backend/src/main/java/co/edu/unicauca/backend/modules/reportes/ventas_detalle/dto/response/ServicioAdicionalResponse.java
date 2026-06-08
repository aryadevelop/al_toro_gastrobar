package co.edu.unicauca.backend.modules.reportes.ventas_detalle.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class ServicioAdicionalResponse {
    private final String nombre;
    private final BigDecimal costo;
}
