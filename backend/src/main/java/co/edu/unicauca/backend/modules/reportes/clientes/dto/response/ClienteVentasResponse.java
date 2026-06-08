package co.edu.unicauca.backend.modules.reportes.clientes.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ClienteVentasResponse {
    private final ClienteResumenResponse cliente;
    private final ClienteVentasResumenResponse resumen;
    private final List<VentaDetalleResponse> ventas;
    private final String mensajeCumpleanos;
    private final String mensajeInactivo;
    private final boolean mostrarRecordatorio;
}
