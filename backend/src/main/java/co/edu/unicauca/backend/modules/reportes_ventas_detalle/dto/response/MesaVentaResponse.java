package co.edu.unicauca.backend.modules.reportes_ventas_detalle.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MesaVentaResponse {
    private final String identificador;
    private final String zona;
}
