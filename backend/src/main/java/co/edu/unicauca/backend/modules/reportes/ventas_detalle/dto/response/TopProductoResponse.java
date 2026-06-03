package co.edu.unicauca.backend.modules.reportes.ventas_detalle.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopProductoResponse {

    private String nombre;
    private Long cantidadVendida;
}
