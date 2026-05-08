package co.edu.unicauca.backend.modules.reportes.clientes.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class ClienteVentasResumenResponse {
    private final long totalVisitas;
    private final BigDecimal totalGastado;
    private final BigDecimal promedioPorVisita;
    private final LocalDateTime ultimaVisita;
    private final LocalDateTime clienteDesde;
}
