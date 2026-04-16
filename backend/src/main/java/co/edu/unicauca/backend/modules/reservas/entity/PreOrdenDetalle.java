package co.edu.unicauca.backend.modules.reservas.entity;

import co.edu.unicauca.backend.modules.produccion.entity.Producto;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entidad que representa un ítem de la pre-orden asociada a una reserva.
 *
 * <p>Cada fila corresponde a un producto solicitado por el cliente al momento de crear
 * la reserva. La pre-orden completa se compone de uno o más registros de esta entidad
 * vinculados a la misma {@link Reserva}.
 *
 * <p>Reglas de uso:
 * <ul>
 *   <li>{@code preordenDetalleCantidad} debe ser al menos {@code 1}; obligatorio.</li>
 *   <li>{@code preordenDetalleDescripcion} es opcional y sirve para anotaciones del cliente
 *       (máximo 500 caracteres); {@code null} si no hay observaciones.</li>
 *   <li>Las modificaciones de menú especial seleccionadas para este ítem se almacenan
 *       en {@link PreOrdenMenuModificacion}.</li>
 *   <li>{@code createdAt} se asigna automáticamente en {@code @PrePersist}.</li>
 * </ul>
 *
 * @see Reserva
 * @see PreOrdenMenuModificacion
 * @see co.edu.unicauca.backend.modules.produccion.entity.Producto
 */
@Entity
@Table(name = "preorden_detalle", schema = "restaurante",
       indexes = {
           @Index(name = "idx_preorden_reserva_id", columnList = "reserva_id"),
           @Index(name = "idx_preorden_producto_id", columnList = "producto_id")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PreOrdenDetalle {

    /** Identificador único del ítem de pre-orden. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "preorden_detalle_id")
    private Long preordenDetalleId;

    /** Reserva a la que pertenece este ítem de pre-orden; obligatorio. */
    @NotNull(message = "La reserva es obligatoria")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reserva_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_preorden_reserva"))
    private Reserva reserva;

    /** Producto solicitado por el cliente en este ítem; obligatorio. */
    @NotNull(message = "El producto es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_preorden_producto"))
    private Producto producto;

    /**
     * Cantidad de unidades del producto solicitadas.
     * Valor mínimo: {@code 1}; obligatorio.
     */
    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad debe ser al menos 1")
    @Column(name = "preorden_detalle_cantidad", nullable = false)
    private Integer preordenDetalleCantidad;

    /**
     * Observaciones o notas del cliente sobre este ítem (p. ej. alergias, puntos de cocción).
     * Máximo 500 caracteres; {@code null} si no hay observaciones.
     */
    @Size(max = 500, message = "La descripción no debe exceder 500 caracteres")
    @Column(name = "preorden_detalle_descripcion", length = 500)
    private String preordenDetalleDescripcion;

    /** Fecha y hora en que se registró el ítem; asignada automáticamente. */
    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PreOrdenDetalle)) return false;
        PreOrdenDetalle that = (PreOrdenDetalle) o;
        return preordenDetalleId != null && preordenDetalleId.equals(that.getPreordenDetalleId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
