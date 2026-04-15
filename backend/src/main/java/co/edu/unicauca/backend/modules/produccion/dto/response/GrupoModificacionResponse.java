package co.edu.unicauca.backend.modules.produccion.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GrupoModificacionResponse {
    private String tipoComponente;
    private String tipoComponenteDescripcion;
    private List<OpcionModificacionResponse> opciones;
}
