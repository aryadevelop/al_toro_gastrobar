package co.edu.unicauca.backend.modules.reportes.ventas_detalle.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class MenuEspecialVentaResponse {
    private final String nombreMenu;
    private final BigDecimal valorPorPersona;
    private final Integer numeroPersonas;
    private final BigDecimal totalCalculado;
}
