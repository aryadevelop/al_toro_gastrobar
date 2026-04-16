package co.edu.unicauca.backend.modules.reservas.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * Resumen de un ítem de pre-orden incluido en la respuesta de reserva.
 */
@Getter
@Builder
public class PreOrdenItemResumen {
    private Long productoId;
    private String productoNombre;
    private Integer cantidad;
    private BigDecimal precioUnitario;
    /** Null para ítems normales; texto libre para modificaciones cuyo precio define el cajero. */
    private String descripcion;
}
