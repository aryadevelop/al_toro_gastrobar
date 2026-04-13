package co.edu.unicauca.backend.modules.reservas.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * Representación de una reserva retornada al cliente.
 */
@Getter
@Builder
public class ReservaResponse {
    private Long reservaId;
    private String fechaHoraLlegada;
    private Integer numeroPersonas;
    private String estado;
    private String tipo;
    private Long decoracionId;
    private String decoracionNombre;
    private Long zonaId;
    private String zonaNombre;
    private String notas;
    private String fechaCreacion;
    private Long clienteId;
    private String clienteNombre;
}
