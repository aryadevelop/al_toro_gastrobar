package co.edu.unicauca.backend.modules.reservas.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

/**
 * Detalle completo de un ítem de pre-orden, incluyendo las modificaciones de menú especial seleccionadas.
 * Usado por el módulo de mesas/comandas para pre-cargar la comanda desde una reserva.
 */
@Getter
@Builder
public class PreOrdenDetalleResponse {
    private Long preordenDetalleId;
    private Long productoId;
    private String productoNombre;
    private Integer cantidad;
    private BigDecimal precioUnitario;
    /** Null para ítems normales; texto libre para modificaciones cuyo precio define el cajero. */
    private String descripcion;
    /** Opciones de modificación seleccionadas (solo para ítems de menú especial). */
    private List<OpcionModificacionSeleccionada> modificaciones;

    @Getter
    @Builder
    public static class OpcionModificacionSeleccionada {
        private Long opcionId;
        private String opcionNombre;
        private String tipoComponente;
    }
}
