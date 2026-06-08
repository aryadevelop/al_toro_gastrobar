package co.edu.unicauca.backend.modules.reportes.ventas_detalle.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VentaListadoResponse {

    private List<VentaListadoItemResponse> ventas;
    private BigDecimal totalPeriodo;
}
