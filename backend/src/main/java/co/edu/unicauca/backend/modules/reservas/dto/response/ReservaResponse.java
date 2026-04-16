package co.edu.unicauca.backend.modules.reservas.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

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
    /** Ítems de pre-orden asociados a la reserva. Null si no hay pre-orden. */
    private List<PreOrdenItemResumen> preOrdenItems;
    /**
     * Total aproximado de la pre-orden (CA-01).
     * Suma precio × cantidad de ítems normales; excluye modificaciones libres (precio TBD por el cajero).
     */
    private BigDecimal preOrdenTotal;
}
