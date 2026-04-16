package co.edu.unicauca.backend.modules.reservas.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

/**
 * Detalle completo de un ítem de pre-orden, incluyendo las modificaciones de menú especial seleccionadas.
 *
 * <p>Reglas de uso:
 * <ul>
 *   <li>{@code descripcion} es {@code null} para ítems normales; contiene texto libre
 *       cuando el cajero define una modificación cuyo precio es variable.</li>
 *   <li>{@code modificaciones} solo está poblado para ítems de menú especial;
 *       {@code null} o lista vacía para ítems normales.</li>
 * </ul>
 *
 * @see PreOrdenItemResumenResponse
 */
@Getter
@Builder
public class PreOrdenDetalleResponse {

    /** Identificador único del detalle de pre-orden en base de datos. */
    private Long preordenDetalleId;

    /** Identificador del producto asociado al ítem. */
    private Long productoId;

    /** Nombre del producto */
    private String productoNombre;

    /** Cantidad de unidades solicitadas del producto. */
    private Integer cantidad;

    /** Precio unitario del producto en el momento de crear la reserva. */
    private BigDecimal precioUnitario;

    /**
     * Texto de modificación libre del ítem.
     * {@code null} para ítems normales; contiene una descripción cuando el cajero
     * define una modificación cuyo precio no está tabulado.
     */
    private String descripcion;

    /**
     * Lista de opciones de modificación seleccionadas para este ítem.
     * Solo aplica para ítems de menú especial; {@code null} o lista vacía para ítems normales.
     */
    private List<OpcionModificacionSeleccionada> modificaciones;

    /**
     * Opción de modificación seleccionada por el cliente para un ítem de menú especial.
     *
     * @see PreOrdenDetalleResponse
     */
    @Getter
    @Builder
    public static class OpcionModificacionSeleccionada {

        /** Identificador único de la opción de modificación. */
        private Long opcionId;

        /** Nombre de la opción, por ejemplo {"Extra picante"}. */
        private String opcionNombre;

        /** Tipo de componente del menú al que pertenece la opción (ver {@code TipoComponenteMenu}). */
        private String tipoComponente;
    }
}
