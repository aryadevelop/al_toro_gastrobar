package co.edu.unicauca.backend.modules.produccion.dto.response;

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
public class MenuEspecialResponse {
    private Long productoId;
    private String productoNombre;
    private String productoDescripcion;
    private BigDecimal productoPrecio;
    private List<GrupoModificacionResponse> modificacionesPorComponente;
}
