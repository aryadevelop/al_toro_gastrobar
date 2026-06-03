package co.edu.unicauca.backend.modules.reportes.ventas_detalle.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VentaListadoItemResponse {

    private Long ventaId;
    private LocalDateTime fechaHora;
    private BigDecimal subtotal;
    private BigDecimal descuento;
    private BigDecimal total;
    private String metodoPago;
    private String clienteNombre;
}
