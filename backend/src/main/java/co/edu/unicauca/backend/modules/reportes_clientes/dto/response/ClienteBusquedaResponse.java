package co.edu.unicauca.backend.modules.reportes_clientes.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ClienteBusquedaResponse {
    private final Long clienteId;
    private final String nombre;
    private final String email;
    private final String telefono;
}
