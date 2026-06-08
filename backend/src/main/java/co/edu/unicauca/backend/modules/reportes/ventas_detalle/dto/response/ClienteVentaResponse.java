package co.edu.unicauca.backend.modules.reportes.ventas_detalle.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ClienteVentaResponse {
    private final String nombre;
    private final String telefono;
}
