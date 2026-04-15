package co.edu.unicauca.backend.modules.reservas.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * Representación de un bloqueo de disponibilidad retornado al administrador.
 */
@Getter
@Builder
public class BloqueResponse {
    private Long bloqueId;
    private String fechaInicio;
    private String fechaFin;
    private String horaInicio;
    private String horaFin;
    private String motivo;
    private String creadoPor;
    private String fechaCreacion;
}
