package co.edu.unicauca.backend.modules.reportes.clientes.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class ClienteResumenResponse {
    private final Long clienteId;
    private final String nombre;
    private final String email;
    private final String telefono;
    private final LocalDate fechaNacimiento;
    private final LocalDateTime clienteDesde;
}
