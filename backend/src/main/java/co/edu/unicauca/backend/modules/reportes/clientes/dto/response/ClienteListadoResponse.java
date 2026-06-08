package co.edu.unicauca.backend.modules.reportes.clientes.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class ClienteListadoResponse {
    private final Long clienteId;
    private final String nombre;
    private final String correoElectronico;
    private final String telefono;
    private final Long totalVisitas;
    private final BigDecimal totalGastado;
    private final Integer puntosAcumulados;
    private final String estado;
    private final Boolean clienteFrecuente;
}
