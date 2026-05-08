package co.edu.unicauca.backend.modules.reportes.ventas_detalle.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class ItemVentaResponse {
    private final String nombre;
    private final Integer cantidad;
    private final BigDecimal precioUnitario;
    private final BigDecimal subtotal;
    private final String especificaciones;
}