package co.edu.unicauca.backend.modules.reportes_clientes.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class VentaAgrupadaMesResponse {
    private final int anio;
    private final int mes;
    private final BigDecimal total;
    private final long cantidad;
}
