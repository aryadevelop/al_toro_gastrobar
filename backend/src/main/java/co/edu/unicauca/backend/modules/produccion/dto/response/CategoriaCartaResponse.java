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
public class CategoriaCartaResponse {
    private Integer categoriaId;
    private String categoriaNombre;
    private Integer orden;
    private List<ProductoCartaResponse> productos;
}
