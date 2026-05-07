package co.edu.unicauca.backend.modules.reportes_clientes.dto.response;

import co.edu.unicauca.backend.shared.enums.MetodoPago;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class VentaDetalleResponse {
    private final Long visitaId;
    private final BigDecimal subtotal;
    private final BigDecimal descuento;
    private final BigDecimal total;
    private final MetodoPago metodo;
    private final LocalDateTime fechaHora;
    private final LocalDateTime createdAt;
    private final String cajeroNombre;
}
