package co.edu.unicauca.backend.modules.usuarios.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class EmpleadoResponse {

    private final Long empleadoId;
    private final String nombre;
    private final String correoElectronico;
    private final String telefono;
    private final String direccion;
    private final LocalDate fechaIngreso;
    private final List<String> roles;
    private final String warning;
}
