package co.edu.unicauca.backend.modules.reportes.ventas_detalle.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PedidoListoResponse {

    private Long comandaId;
    private Long visitaId;
    private String estacion;
    private LocalDateTime fechaHoraListo;
}
