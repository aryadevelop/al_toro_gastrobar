package co.edu.unicauca.backend.modules.reservas.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class DecoracionAdminResponse {

    private Long decoracionId;
    private String decoracionNombre;
    private String decoracionEstado;
    private BigDecimal decoracionCostoAdicional;
    private String decoracionImagenUrl;
    private List<Long> zonaIds;
}
